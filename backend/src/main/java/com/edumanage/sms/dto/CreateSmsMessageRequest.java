package com.edumanage.sms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSmsMessageRequest {
    @NotNull(message = "School ID is required")
    private Long schoolId;

    @NotBlank(message = "Recipient is required")
    @Size(max = 50)
    private String recipient;

    @NotBlank(message = "Message is required")
    private String message;

    private Long templateId;

    private boolean bulk = false;
}
