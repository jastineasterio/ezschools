package com.edumanage.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsMessageDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long templateId;
    private String recipient;
    private String message;
    private String status;
    private String providerResponse;
    private String providerMessageId;
    private Integer retryCount;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime failedAt;
    private String errorMessage;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
