package com.edumanage.sms.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSmsConfigurationRequest {
    @Size(max = 50)
    private String provider;

    @Size(max = 200)
    private String apiKey;

    @Size(max = 200)
    private String apiSecret;

    @Size(max = 30)
    private String senderId;

    private Integer monthlyLimit;

    private Integer dailyLimit;

    private String remarks;

    private Boolean isActive;
}
