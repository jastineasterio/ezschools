package com.edumanage.notifications.dto;

import java.time.LocalDateTime;

public record CommunicationCampaignDto(
        Long id,
        Long schoolId,
        Long senderUserId,
        String title,
        String category,
        String channel,
        String recipientGroup,
        int intendedCount,
        int excludedCount,
        int sentCount,
        int failedCount,
        int deliveredCount,
        String status,
        LocalDateTime createdAt
) {
}
