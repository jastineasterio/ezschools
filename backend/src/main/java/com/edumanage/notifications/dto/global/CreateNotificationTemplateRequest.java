package com.edumanage.notifications.dto.global;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationTemplateRequest {
    private String name;
    private String channel;
    private String subject;
    private String content;
    private String variables;
    private boolean active;
}
