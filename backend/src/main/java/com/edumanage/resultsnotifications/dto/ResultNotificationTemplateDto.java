package com.edumanage.resultsnotifications.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ResultNotificationTemplateDto(
        Long schoolId,
        String smsTemplate,
        boolean isDefault,
        LocalDateTime updatedAt,
        List<PlaceholderInfo> supportedPlaceholders
) {
}
