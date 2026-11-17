package com.edumanage.resultsnotifications.dto;

import java.time.LocalDateTime;

public record ResultNotificationDeliveryDto(
        Long id,
        Long notificationId,
        Long smsMessageId,
        String recipientType,
        Long recipientId,
        String recipientName,
        String recipientPhone,
        Long studentId,
        String studentName,
        String admissionNumber,
        String channel,
        String status,
        String providerMessageId,
        String batchId,
        String failureCode,
        String errorMessage,
        String deliveryStatus,
        int attemptCount,
        LocalDateTime lastAttemptAt,
        LocalDateTime sentAt,
        LocalDateTime deliveredAt,
        LocalDateTime failedAt
) {
}
