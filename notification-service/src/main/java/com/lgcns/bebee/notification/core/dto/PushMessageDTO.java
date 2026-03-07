package com.lgcns.bebee.notification.core.dto;

import java.util.Map;

public record PushMessageDTO(
    String token,
    String title,
    String body,
    Map<String, String> data
) {}
