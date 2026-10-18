package com.edumanage.notifications.dto;

public record ManagerChannelPermissionDto(
        Long managerUserId,
        String managerEmail,
        Long schoolId,
        boolean smsEnabled,
        boolean emailEnabled
) {
}
