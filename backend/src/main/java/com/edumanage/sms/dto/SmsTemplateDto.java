package com.edumanage.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsTemplateDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private String name;
    private String content;
    private String variables;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
