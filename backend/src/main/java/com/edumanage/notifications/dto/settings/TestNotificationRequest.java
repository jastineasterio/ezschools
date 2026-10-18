package com.edumanage.notifications.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestNotificationRequest {
    private String channel;
    private String recipient;
    private String subject;
    private String message;
}
