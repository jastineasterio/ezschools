package com.edumanage.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private String notificationType = "SYSTEM";
    private String priority = "NORMAL";
    private String actionUrl;
    private String metadata;
}
