package com.edumanage.notifications.dto.global;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateDto {
    private Long id;
    private String uuid;
    private String name;
    private String channel;
    private String subject;
    private String content;
    private String variables;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
