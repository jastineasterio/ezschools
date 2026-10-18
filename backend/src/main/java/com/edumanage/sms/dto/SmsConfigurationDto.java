package com.edumanage.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsConfigurationDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private String provider;
    private String senderId;
    private Integer monthlyLimit;
    private Integer dailyLimit;
    private String remarks;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
