package com.lgcns.bebee.notification.infrastructure.event;

import com.lgcns.bebee.common.data.event.EventHandler;
import com.lgcns.bebee.common.data.event.match.PostAppliedEvent;
import com.lgcns.bebee.notification.application.usecase.SendPushNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostAppliedEventHandler implements EventHandler<PostAppliedEvent> {
    private final SendPushNotificationUseCase sendPushNotificationUseCase;

    @Override
    public Class<PostAppliedEvent> getEventClass() {
        return PostAppliedEvent.class;
    }

    @Override
    public void handle(PostAppliedEvent event) {
        SendPushNotificationUseCase.Param param = new SendPushNotificationUseCase.Param(
                event.getHelperId(),
                null,
                event.getDisabledId(),
                "APPLICATION",
                event.getApplicationId(),
                null,
                null,
                null
        );

        sendPushNotificationUseCase.execute(param);
    }
}
