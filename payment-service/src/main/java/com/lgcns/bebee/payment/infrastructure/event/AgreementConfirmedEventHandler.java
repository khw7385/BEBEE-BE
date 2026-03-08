package com.lgcns.bebee.payment.infrastructure.event;


import com.lgcns.bebee.common.data.event.DomainEventPublisher;
import com.lgcns.bebee.common.data.event.match.AgreementConfirmedEvent;
import com.lgcns.bebee.common.data.event.EventHandler;
import com.lgcns.bebee.common.data.event.payment.PaymentFailedEvent;
import com.lgcns.bebee.common.data.event.payment.PaymentSucceededEvent;
import com.lgcns.bebee.payment.application.usecase.UseHoneyUseCase;
import com.lgcns.bebee.payment.domain.entity.sync.EngagementType;
import com.lgcns.bebee.payment.domain.entity.sync.PaymentAgreementSync;
import com.lgcns.bebee.payment.domain.entity.sync.PaymentMatchSync;
import com.lgcns.bebee.payment.domain.repository.AgreementRepository;
import com.lgcns.bebee.payment.domain.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgreementConfirmedEventHandler implements EventHandler<AgreementConfirmedEvent> {

    private final UseHoneyUseCase useHoneyUseCase;
    private final AgreementRepository agreementRepository;
    private final MatchRepository matchRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Class<AgreementConfirmedEvent> getEventClass() {
        return AgreementConfirmedEvent.class;
    }


    @Override
    @Transactional
    public void handle(AgreementConfirmedEvent event) {
        log.info("AgreementConfirmed 이벤트 처리 시작 - matchId: {}, agreementId: {}", event.getMatchId(), event.getAgreementId());

        // 1. PaymentAgreementSync 동기화 (없으면 생성)
        PaymentAgreementSync agreement = agreementRepository.findById(event.getAgreementId())
                .orElseGet(() -> {
                    log.info("PaymentAgreementSync 생성 - agreementId: {}", event.getAgreementId());
                    PaymentAgreementSync newAgreement = PaymentAgreementSync.create(
                            event.getAgreementId(),
                            event.getUnitHoney(),
                            event.getTotalHoney(),
                            EngagementType.valueOf(event.getType())
                    );
                    return agreementRepository.save(newAgreement);
                });

        // 2. PaymentMatchSync 동기화 (없으면 생성)
        matchRepository.findByMatchId(event.getMatchId())
                .orElseGet(() -> {
                    log.info("PaymentMatchSync 생성 - matchId: {}", event.getMatchId());
                    PaymentMatchSync newMatch = PaymentMatchSync.create(
                            event.getMatchId(),
                            event.getHelperId(),
                            event.getDisabledId(),
                            agreement
                    );
                    return matchRepository.save(newMatch);
                });

        // 3. UseHoneyUseCase 실행
        log.info("UseHoneyUseCase 실행 - disabledId: {}, matchId: {}, unitHoney: {}",
                event.getDisabledId(), event.getMatchId(), event.getUnitHoney());

        UseHoneyUseCase.Param param = new UseHoneyUseCase.Param(
                event.getDisabledId(),
                event.getMatchId(),
                event.getUnitHoney()
        );

        try {
            useHoneyUseCase.execute(param);

            // 4. 결제 성공 이벤트 발행
            eventPublisher.publish(new PaymentSucceededEvent(
                    event.getMatchId(),
                    event.getAgreementId(),
                    event.getDisabledId()
            ));

            log.info("AgreementConfirmed 이벤트 처리 완료 - matchId: {}", event.getMatchId());
        } catch (Exception e) {
            log.error("결제 실패 - matchId: {}, reason: {}", event.getMatchId(), e.getMessage());
            eventPublisher.publish(new PaymentFailedEvent(
                    event.getMatchId(),
                    event.getAgreementId(),
                    event.getDisabledId()
            ));
            throw e;
        }
    }
}
