package com.lgcns.bebee.notification.infrastructure.fcm;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.messaging.*;
import com.lgcns.bebee.notification.application.client.PushNotificationClient;
import com.lgcns.bebee.notification.core.dto.BatchResponseDTO;
import com.lgcns.bebee.notification.core.dto.PushMessageDTO;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Firebase Cloud Messaging을 사용한 푸시 알림 클라이언트 구현체.
 * 동기/비동기, 단일/배치 전송을 지원합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FcmPushNotificationPublisher implements PushNotificationClient {

    private final FirebaseMessaging firebaseMessaging;
    private final MeterRegistry meterRegistry;

    @Override
    public void sendMessage(String token, String title, String body, Map<String, String> data) {
        Timer.Sample sample = Timer.start(meterRegistry);

        Map<String, String> messageData = new HashMap<>(data);
        messageData.put("title", title);
        messageData.put("body", body);

        try {
            Message message = buildMessage(token, title, body, messageData);
            String messageId = firebaseMessaging.send(message);
            log.info("FCM 메시지 전송 성공: messageId={}, token={}", messageId, maskToken(token));


        } catch (FirebaseMessagingException e) {
//            log.error("FCM 전송 실패: error={}", e.getMessage(), e);
            handleFcmException(token, e);
        }finally{
            sample.stop(meterRegistry.timer("fcm.send.latency.per.message", "type", "sync"));
            meterRegistry.counter("fcm.messages.processed", "type", "sync").increment();
        }
    }

    @Override
    public CompletableFuture<String> sendMessageAsync(String token, String title, String body, Map<String, String> data) {
        Timer.Sample sample = Timer.start(meterRegistry);

        Map<String, String> messageData = new HashMap<>(data);
        messageData.put("title", title);
        messageData.put("body", body);

        Message message = buildMessage(token, title, body, messageData);

        // sendAsync()는 논블로킹 - 스레드 점유 없이 비동기 처리
        ApiFuture<String> apiFuture = firebaseMessaging.sendAsync(message);

        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        ApiFutures.addCallback(apiFuture, new ApiFutureCallback<>() {
            @Override
            public void onSuccess(String messageId) {
                sample.stop(meterRegistry.timer("fcm.send.latency.per.message", "type", "async"));
                meterRegistry.counter("fcm.messages.processed", "type", "async").increment();
                // log.info("FCM 비동기 메시지 전송 성공: messageId={}, token={}", messageId, maskToken(token));
                completableFuture.complete(messageId);
            }

            @Override
            public void onFailure(Throwable t) {
                sample.stop(meterRegistry.timer("fcm.send.latency.per.message", "type", "async"));
//                log.error("FCM 비동기 메시지 전송 실패: token={}, error={}", maskToken(token), t.getMessage());
                meterRegistry.counter("fcm.messages.processed", "type", "async").increment();
                if (t instanceof FirebaseMessagingException e) {
                    handleFcmException(token, e);
                }
                completableFuture.completeExceptionally(t);
            }
        }, MoreExecutors.directExecutor());

        return completableFuture;
    }

    // ========================================
    // 3. 다중 메시지 배치 전송 (sendAll)
    // ========================================
    @Override
    public BatchResponseDTO sendAllMessages(List<PushMessageDTO> messages) {
        if (messages == null || messages.isEmpty()) {
            return new BatchResponseDTO(0, 0, List.of());
        }

        List<Message> fcmMessages = messages.stream()
                .map(this::convertToFcmMessage)
                .toList();

        try {
            BatchResponse response = firebaseMessaging.sendEach(fcmMessages);

            List<String> failedTokens = extractFailedTokens(messages, response);

            log.info("FCM 배치 전송 완료: 성공={}, 실패={}",
                    response.getSuccessCount(), response.getFailureCount());

            return new BatchResponseDTO(
                    response.getSuccessCount(),
                    response.getFailureCount(),
                    failedTokens
            );

        } catch (FirebaseMessagingException e) {
            log.error("FCM 배치 전송 실패: error={}", e.getMessage(), e);
            List<String> allTokens = messages.stream().map(PushMessageDTO::token).toList();
            return new BatchResponseDTO(0, messages.size(), allTokens);
        }
    }

    // ========================================
    // 4. 다중 메시지 비동기 배치 전송 (sendEachAsync 사용 - 논블로킹)
    // ========================================
    @Override
    public CompletableFuture<BatchResponseDTO> sendAllMessagesAsync(List<PushMessageDTO> messages) {
        if (messages == null || messages.isEmpty()) {
            return CompletableFuture.completedFuture(new BatchResponseDTO(0, 0, List.of()));
        }

        Timer.Sample sample = Timer.start(meterRegistry);

        List<Message> fcmMessages = messages.stream()
                .map(this::convertToFcmMessage)
                .toList();

        // sendEachAsync()는 논블로킹 - 스레드 점유 없이 비동기 처리
        ApiFuture<BatchResponse> apiFuture =
                firebaseMessaging.sendEachAsync(fcmMessages);

        CompletableFuture<BatchResponseDTO> completableFuture = new CompletableFuture<>();

        ApiFutures.addCallback(apiFuture, new ApiFutureCallback<>() {
            @Override
            public void onSuccess(com.google.firebase.messaging.BatchResponse response) {
                long elapsedNanos = sample.stop(meterRegistry.timer("fcm.send.latency", "type", "batch-async"));
                long perMessageNanos = elapsedNanos / messages.size();
                meterRegistry.timer("fcm.send.latency.per.message", "type", "batch-async").record(perMessageNanos, TimeUnit.NANOSECONDS);
                meterRegistry.counter("fcm.messages.processed", "type", "batch-async").increment(messages.size());

                List<String> failedTokens = extractFailedTokens(messages, response);

//                log.info("FCM 비동기 배치 전송 완료: 성공={}, 실패={}",
//                        response.getSuccessCount(), response.getFailureCount());

                completableFuture.complete(new BatchResponseDTO(
                        response.getSuccessCount(),
                        response.getFailureCount(),
                        failedTokens
                ));
            }

            @Override
            public void onFailure(Throwable t) {
                long elapsedNanos = sample.stop(meterRegistry.timer("fcm.send.latency", "type", "batch-async"));
                long perMessageNanos = elapsedNanos / messages.size();
                meterRegistry.timer("fcm.send.latency.per.message", "type", "batch-async").record(perMessageNanos, TimeUnit.NANOSECONDS);
                meterRegistry.counter("fcm.messages.processed", "type", "async").increment();
//                log.error("FCM 비동기 배치 전송 실패: error={}", t.getMessage(), t);
                List<String> allTokens = messages.stream().map(PushMessageDTO::token).toList();
                completableFuture.complete(new BatchResponseDTO(0, messages.size(), allTokens));
            }
        }, MoreExecutors.directExecutor());

        return completableFuture;
    }

    private CompletableFuture<String> toCompletableFuture(ApiFuture<String> apiFuture, String token) {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        ApiFutures.addCallback(apiFuture, new ApiFutureCallback<>() {
            @Override
            public void onSuccess(String messageId) {


                log.info("FCM 비동기 메시지 전송 성공: messageId={}, token={}", messageId, maskToken(token));
                completableFuture.complete(messageId);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("FCM 비동기 메시지 전송 실패: token={}, error={}", maskToken(token), t.getMessage());

                if (t instanceof FirebaseMessagingException e) {
                    handleFcmException(token, e);
                }
                completableFuture.completeExceptionally(t);
            }
        }, MoreExecutors.directExecutor());

        return completableFuture;
    }

    private CompletableFuture<BatchResponseDTO> toBatchCompletableFuture(
            ApiFuture<BatchResponse> apiFuture,
            List<PushMessageDTO> messages) {

        CompletableFuture<BatchResponseDTO> completableFuture = new CompletableFuture<>();

        ApiFutures.addCallback(apiFuture, new ApiFutureCallback<>() {
            @Override
            public void onSuccess(com.google.firebase.messaging.BatchResponse response) {
                List<String> failedTokens = extractFailedTokens(messages, response);

                log.info("FCM 비동기 배치 전송 완료: 성공={}, 실패={}",
                        response.getSuccessCount(), response.getFailureCount());

                completableFuture.complete(new BatchResponseDTO(
                        response.getSuccessCount(),
                        response.getFailureCount(),
                        failedTokens
                ));
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("FCM 비동기 배치 전송 실패: error={}", t.getMessage(), t);
                List<String> allTokens = messages.stream().map(PushMessageDTO::token).toList();
                completableFuture.complete(new BatchResponseDTO(0, messages.size(), allTokens));
            }
        }, MoreExecutors.directExecutor());

        return completableFuture;
    }

    private Message buildMessage(String token, String title, String body, Map<String, String> data) {
        return Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .build();
    }

    private Message convertToFcmMessage(PushMessageDTO pushMessage) {
        Map<String, String> messageData = new HashMap<>(pushMessage.data() != null ? pushMessage.data() : Map.of());
        messageData.put("title", pushMessage.title());
        messageData.put("body", pushMessage.body());

        return buildMessage(
                pushMessage.token(),
                pushMessage.title(),
                pushMessage.body(),
                messageData
        );
    }

    private List<String> extractFailedTokens(List<PushMessageDTO> messages,
                                              BatchResponse response) {
        List<String> failedTokens = new ArrayList<>();
        List<SendResponse> responses = response.getResponses();

        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (!sendResponse.isSuccessful()) {
                String failedToken = messages.get(i).token();
                failedTokens.add(failedToken);

                FirebaseMessagingException exception = sendResponse.getException();
                if (exception != null) {
                    handleFcmException(failedToken, exception);
                }
            }
        }

        return failedTokens;
    }

    private void handleFcmException(String token, FirebaseMessagingException e) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();

        if (errorCode == MessagingErrorCode.UNREGISTERED ||
            errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
            // 무효한 토큰 - 삭제 필요
//            log.warn("무효 토큰 감지 - 삭제 처리 필요: token={}, errorCode={}", maskToken(token), errorCode);
            // TODO: 토큰 삭제 이벤트 발행 또는 직접 삭제
        }
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 20) {
            return "***";
        }
        return token.substring(0, 10) + "..." + token.substring(token.length() - 5);
    }
}