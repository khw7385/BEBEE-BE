package com.lgcns.bebee.notification.infrastructure.event;

import com.lgcns.bebee.common.data.event.EventHandler;
import com.lgcns.bebee.common.data.event.chat.ChatroomCreatedEvent;
import com.lgcns.bebee.notification.application.usecase.SendPushNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatroomCreatedEventHandler implements EventHandler<ChatroomCreatedEvent> {
    private final SendPushNotificationUseCase sendPushNotificationUseCase;

    @Override
    public Class<ChatroomCreatedEvent> getEventClass() {
        return ChatroomCreatedEvent.class;
    }

    @Override
    public void handle(ChatroomCreatedEvent event) {
        SendPushNotificationUseCase.Param param = new SendPushNotificationUseCase.Param(
                event.getDisabledId(),
                event.getDisabledNickname(),
                event.getHelperId(),
                "CHAT",
                null,
                null,
                event.getChatroomId(),
                null
        );

        sendPushNotificationUseCase.execute(param);
    }
}
