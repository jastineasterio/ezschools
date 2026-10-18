package com.edumanage.notifications.dto;

public record NotificationChannelsDto(
        boolean notificationsEnabled,
        boolean smsEnabled,
        boolean smsConfigured,
        boolean emailEnabled,
        boolean emailConfigured
) {
}
