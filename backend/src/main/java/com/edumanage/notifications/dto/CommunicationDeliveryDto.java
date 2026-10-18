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
        int attemptCount,
        LocalDateTime lastAttemptAt
) {
}
