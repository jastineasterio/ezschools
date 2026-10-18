package com.edumanage.sms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSmsTemplateRequest {
    @NotNull(message = "School ID is required")
    private Long schoolId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Content is required")
    private String content;

    private String variables;

    private boolean isActive = true;
}
