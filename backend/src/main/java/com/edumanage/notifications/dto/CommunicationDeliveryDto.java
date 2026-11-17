package com.edumanage.notifications.dto;

import java.time.LocalDateTime;

public record CommunicationDeliveryDto(
        Long id,
        String recipientType,
        Long recipientId,
        String channel,
        String status,
        String providerMessageId,
        String failureCode,
        String errorMessage,
        String deliveryStatus,
        LocalDateTime sentAt,
        LocalDateTime deliveredAt,
        String batchId,
        int attemptCount,
        LocalDateTime lastAttemptAt,
        String recipientName,
        String recipientPhone
) {
}
