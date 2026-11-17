package com.edumanage.resultsnotifications.dto;

import java.time.LocalDateTime;

public record ResultNotificationDto(
        Long id,
        String uuid,
        Long schoolId,
        Long examinationId,
        Long classId,
        Long streamId,
        String recipientType,
        String idempotencyKey,
        String title,
        int totalRecipients,
        int excludedCount,
        int successfulCount,
        int failedCount,
        int pendingCount,
        String status,
        Long initiatedBy,
        LocalDateTime createdAt
) {
}
