package com.edumanage.notifications.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationSettingsRequest {
    private Map<String, Object> globalSettings;
    private Map<String, Map<String, Boolean>> eventChannels;
}
