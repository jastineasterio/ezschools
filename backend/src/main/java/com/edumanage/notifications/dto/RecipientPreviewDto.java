package com.edumanage.notifications.dto;

public record RecipientPreviewDto(
        String recipientGroup,
        String channel,
        int eligibleCount,
        int excludedCount,
        boolean channelEnabled,
        String disabledReason
) {
}
