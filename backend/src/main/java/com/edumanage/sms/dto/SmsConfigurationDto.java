package com.edumanage.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String schoolName;
    private String provider;
    private String senderId;
    private Integer monthlyLimit;
    private Integer dailyLimit;
    private String remarks;
    @JsonProperty("isActive")
    private boolean isActive;
    private boolean credentialsConfigured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
