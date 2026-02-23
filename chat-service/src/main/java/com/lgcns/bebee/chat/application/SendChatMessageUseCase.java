package com.lgcns.bebee.chat.application;

import com.lgcns.bebee.chat.application.client.MessagePublisher;
import com.lgcns.bebee.chat.domain.entity.Chat;
import com.lgcns.bebee.chat.domain.entity.Chatroom;
import com.lgcns.bebee.chat.domain.entity.sync.MemberSync;
import com.lgcns.bebee.chat.domain.repository.ChatRepository;
import com.lgcns.bebee.chat.domain.service.ChatroomManagement;
import com.lgcns.bebee.chat.domain.service.MemberManagement;
import com.lgcns.bebee.common.application.Params;
import com.lgcns.bebee.common.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SendChatMessageUseCase implements UseCase<SendChatMessageUseCase.Param, Void> {
    private final MessagePublisher messagePublisher;

    private final ChatroomManagement chatroomManagement;
    private final MemberManagement memberManagement;
    private final ChatRepository chatRepository;

    @Override
    @Transactional
    public Void execute(Param param) {
        MemberSync sender = memberManagement.getExistingMember(param.senderId);
        MemberSync receiver = memberManagement.getExistingMember(param.receiverId);

        Chatroom chatroom = chatroomManagement.getExistingChatroom(param.chatroomId);

        Chat chat = Chat.create(
                chatroom.getId(),
                param.senderId,
                param.receiverId,
                param.textContent,
                param.chatType,
                param.attachments,
                null, null, null, null, null,
                null, null, null, null,null, null,
                param.createdAt
        );

        Chat savedChat = chatRepository.save(chat);

        chatroomManagement.updateLastMessage(chatroom, savedChat);

        // Redis를 통해 발신자와 수신자에게 메시지 발행
        messagePublisher.publishToMember(param.senderId, param.receiverId, chat);

        return null;
    }

    @RequiredArgsConstructor
    public static class Param implements Params {
        private final Long chatroomId;
        private final Long senderId;
        private final Long receiverId;
        private final String textContent;
        private final String chatType;
        private final List<String> attachments;
        private final LocalDateTime createdAt;
    }
}