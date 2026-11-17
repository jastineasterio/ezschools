package com.edumanage.resultsnotifications.dto;

public record ResultNotificationSummary(
        int totalRecipients,
        int excludedCount,
        int successfulCount,
        int failedCount,
        int pendingCount
) {
}
