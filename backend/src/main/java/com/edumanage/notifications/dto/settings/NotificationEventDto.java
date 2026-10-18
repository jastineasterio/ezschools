package com.edumanage.notifications.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEventDto {
    private Long id;
    private String eventCode;
    private String displayName;
    private String description;
    private boolean enabled;
    private Map<String, Boolean> channels;
}
