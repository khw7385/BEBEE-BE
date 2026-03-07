package com.lgcns.bebee.notification.presentation;

import com.lgcns.bebee.common.annotation.CurrentMember;
import com.lgcns.bebee.notification.application.BatchPushMessageProcessor;
import com.lgcns.bebee.notification.application.usecase.RegisterFcmTokenUseCase;
import com.lgcns.bebee.notification.application.client.PushNotificationClient;
import com.lgcns.bebee.notification.presentation.dto.req.FcmTokenRegisterReqDTO;
import com.lgcns.bebee.notification.presentation.dto.req.PushNotificationTestReqDTO;
import com.lgcns.bebee.notification.presentation.swagger.NotificationSwagger;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationSwagger {
    private final RegisterFcmTokenUseCase registerFcmTokenUseCase;
    private final PushNotificationClient pushNotificationClient;
    private final BatchPushMessageProcessor batchPushMessageProcessor;

    @PostMapping("/fcm/tokens")
    public ResponseEntity<Void> registerToken(
            @CurrentMember Long memberId,
            @RequestBody FcmTokenRegisterReqDTO reqDTO
            ){
        RegisterFcmTokenUseCase.Param param = new RegisterFcmTokenUseCase.Param(memberId, reqDTO.token(), reqDTO.deviceType());

        registerFcmTokenUseCase.execute(param);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/fcm/test")
    public void sendTestPushNotification(
            @RequestBody PushNotificationTestReqDTO reqDTO
    ) {
        String token = reqDTO.token();
        String title = reqDTO.title() != null ? reqDTO.title() : "테스트 알림";
        String body = reqDTO.body() != null ? reqDTO.body() : "푸시 알림 테스트 메시지입니다.";
        Map<String, String> data = reqDTO.data() != null ? reqDTO.data() : new HashMap<>();

        pushNotificationClient.sendMessage(token, title, body, data);
    }

    /**
     * FCM 푸시 알림 비동기 전송 테스트 API.
     */
    @PostMapping("/fcm/test/async")
    public void sendTestPushNotificationAsync(
            @RequestBody PushNotificationTestReqDTO reqDTO
    ) {
        String token = reqDTO.token();
        String title = reqDTO.title() != null ? reqDTO.title() : "테스트 알림";
        String body = reqDTO.body() != null ? reqDTO.body() : "푸시 알림 테스트 메시지입니다.";
        Map<String, String> data = reqDTO.data() != null ? reqDTO.data() : new HashMap<>();

        pushNotificationClient.sendMessageAsync(token, title, body, data)
                .thenApply(messageId -> Map.of(
                        "status", "success",
                        "messageId", messageId
                ))
                .exceptionally(e -> Map.of(
                        "status", "error",
                        "message", e.getMessage()
                ));
    }

    @PostMapping("/fcm/test/batch")
    public void sendTestPushNotificationBatch(
            @RequestBody PushNotificationTestReqDTO reqDTO
    ) {
        String token = reqDTO.token();
        String title = reqDTO.title() != null ? reqDTO.title() : "테스트 알림";
        String body = reqDTO.body() != null ? reqDTO.body() : "푸시 알림 테스트 메시지입니다.";
        Map<String, String> data = reqDTO.data() != null ? reqDTO.data() : new HashMap<>();

        batchPushMessageProcessor.add(token, title, body, data);
    }

    @PostMapping("/fcm/test/batch/lock-free")
    public void sendTestPushNotificationBatchLockFree(
            @RequestBody PushNotificationTestReqDTO reqDTO
    ) {
        String token = reqDTO.token();
        String title = reqDTO.title() != null ? reqDTO.title() : "테스트 알림";
        String body = reqDTO.body() != null ? reqDTO.body() : "푸시 알림 테스트 메시지입니다.";
        Map<String, String> data = reqDTO.data() != null ? reqDTO.data() : new HashMap<>();

        batchPushMessageProcessor.addLockFree(token, title, body, data);
    }
}
