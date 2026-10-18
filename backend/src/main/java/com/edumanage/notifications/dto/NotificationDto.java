package com.edumanage.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long userId;
    private String title;
    private String message;
    private String notificationType;
    private String priority;
    private Boolean isRead;
    private LocalDateTime readAt;
    private String actionUrl;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
