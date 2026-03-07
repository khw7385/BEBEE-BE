package com.lgcns.bebee.notification.application.client;

import com.lgcns.bebee.notification.core.dto.BatchResponseDTO;
import com.lgcns.bebee.notification.core.dto.PushMessageDTO;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface PushNotificationClient {

    /**
     * 단일 메시지 동기 전송
     */
    void sendMessage(String token, String title, String body, Map<String, String> data);

    /**
     * 단일 메시지 비동기 전송
     */
    CompletableFuture<String> sendMessageAsync(String token, String title, String body, Map<String, String> data);

    /**
     * 다중 메시지 배치 전송 (sendAll)
     */
    BatchResponseDTO sendAllMessages(List<PushMessageDTO> messages);

    /**
     * 다중 메시지 비동기 배치 전송
     */
    CompletableFuture<BatchResponseDTO> sendAllMessagesAsync(List<PushMessageDTO> messages);

}
