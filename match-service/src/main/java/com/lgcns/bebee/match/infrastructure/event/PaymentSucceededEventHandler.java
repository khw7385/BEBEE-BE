package com.lgcns.bebee.match.infrastructure.event;

import com.lgcns.bebee.common.data.event.EventHandler;
import com.lgcns.bebee.common.data.event.payment.PaymentSucceededEvent;
import com.lgcns.bebee.match.application.usecase.CompleteMatchPaymentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSucceededEventHandler implements EventHandler<PaymentSucceededEvent> {
    private final CompleteMatchPaymentUseCase completeMatchPaymentUseCase;

    @Override
    public Class<PaymentSucceededEvent> getEventClass() {
        return PaymentSucceededEvent.class;
    }

    @Override
    public void handle(PaymentSucceededEvent event) {
        log.info("PaymentSucceeded 이벤트 처리 시작 - matchId: {}", event.getMatchId());

        CompleteMatchPaymentUseCase.Param param = new CompleteMatchPaymentUseCase.Param(
                event.getMatchId()
        );

        completeMatchPaymentUseCase.execute(param);

        log.info("PaymentSucceeded 이벤트 처리 완료 - matchId: {}", event.getMatchId());
    }
}