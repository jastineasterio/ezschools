package com.edumanage.sms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSmsConfigurationRequest {
    @NotNull(message = "School ID is required")
    private Long schoolId;

    @NotBlank(message = "Provider is required")
    @Size(max = 50)
    private String provider;

    @NotBlank(message = "API key is required")
    private String apiKey;

    @Size(max = 200)
    private String apiSecret;

    @Size(max = 30)
    private String senderId;

    private Integer monthlyLimit;

    private Integer dailyLimit;

    private String remarks;

    @JsonProperty("isActive")
    private boolean isActive = true;
}
