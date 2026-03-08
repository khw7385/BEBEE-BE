package com.lgcns.bebee.match.infrastructure.event;

import com.lgcns.bebee.common.data.event.EventHandler;
import com.lgcns.bebee.common.data.event.payment.PaymentFailedEvent;
import com.lgcns.bebee.match.application.usecase.CancelAgreementUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFailedEventHandler implements EventHandler<PaymentFailedEvent> {
    private final CancelAgreementUseCase cancelAgreementUseCase;

    @Override
    public Class<PaymentFailedEvent> getEventClass() {
        return PaymentFailedEvent.class;
    }

    @Override
    public void handle(PaymentFailedEvent event) {
        log.info("PaymentFailed 이벤트 처리 시작 - matchId: {}, agreementId: {}",
                event.getMatchId(), event.getAgreementId());

        CancelAgreementUseCase.Param param = new CancelAgreementUseCase.Param(
                event.getMatchId(),
                event.getAgreementId()
        );

        cancelAgreementUseCase.execute(param);

        log.info("PaymentFailed 이벤트 처리 완료 - matchId: {}, agreementId: {}",
                event.getMatchId(), event.getAgreementId());
    }
}