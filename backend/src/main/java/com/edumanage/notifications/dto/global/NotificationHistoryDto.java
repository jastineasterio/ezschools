package com.edumanage.notifications.dto.global;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationHistoryDto {
    private Long id;
    private String uuid;
    private String eventCode;
    private String channel;
    private String recipientType;
    private String recipientIdentifier;
    private String subject;
    private String message;
    private String status;
    private String provider;
    private String providerMessageId;
    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
