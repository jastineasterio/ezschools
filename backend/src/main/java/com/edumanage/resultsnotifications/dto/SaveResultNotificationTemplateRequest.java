package com.edumanage.resultsnotifications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveResultNotificationTemplateRequest {

    @NotBlank(message = "SMS template is required")
    @Size(max = 2000, message = "SMS template must not exceed 2000 characters")
    private String smsTemplate;
}
