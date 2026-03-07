package com.lgcns.bebee.notification.core.dto;

import java.util.List;

public record BatchResponseDTO(
        int successCount,
        int failureCount,
        List<String> failedTokens
) {}
