package com.lgcns.bebee.notification.presentation.dto.req;

import java.util.Map;

/**
 * FCM 푸시 알림 직접 전송 테스트용 DTO.
 * 토큰을 직접 입력받아 푸시 알림을 전송합니다.
 */
public record PushNotificationTestReqDTO(
        String token,
        String title,
        String body,
        Map<String, String> data
) {
}
