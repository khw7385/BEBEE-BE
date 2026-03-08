package com.lgcns.bebee.common.data.event.payment;

import com.lgcns.bebee.common.data.event.DomainEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PaymentFailedEvent implements DomainEvent {
    private final Long matchId;
    private final Long agreementId;
    private final Long disabledId;
}