package com.edumanage.notifications.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateManagerChannelsRequest(
        @NotNull Boolean smsEnabled,
        @NotNull Boolean emailEnabled
) {
}
