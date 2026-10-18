package com.edumanage.notifications.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationChannelsRequest(
        @NotNull Boolean notificationsEnabled,
        @NotNull Boolean smsEnabled,
        @NotNull Boolean emailEnabled
) {
}
