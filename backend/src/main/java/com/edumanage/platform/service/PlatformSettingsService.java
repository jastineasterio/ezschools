package com.edumanage.platform.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.platform.dto.PlatformSettingsDto;
import com.edumanage.platform.entity.PlatformSettings;
import com.edumanage.platform.repository.PlatformSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlatformSettingsService {

    private final PlatformSettingsRepository platformSettingsRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public PlatformSettingsDto getSettings() {
        Map<String, Object> settings = new HashMap<>();
        platformSettingsRepository.findAll().forEach(s -> {
            settings.put(s.getSettingKey(), parseValue(s.getSettingValue()));
        });

        // Set defaults if missing
        if (!settings.containsKey("notifications.enabled")) {
            settings.put("notifications.enabled", true);
        }
        if (!settings.containsKey("email.enabled")) {
            settings.put("email.enabled", true);
        }
        if (!settings.containsKey("sms.enabled")) {
            settings.put("sms.enabled", true);
        }
        if (!settings.containsKey("manager.account.creation.notifications.enabled")) {
            settings.put("manager.account.creation.notifications.enabled", true);
        }
        if (!settings.containsKey("platform.announcements.enabled")) {
            settings.put("platform.announcements.enabled", true);
        }

        return new PlatformSettingsDto(settings);
    }

    @Transactional
    public PlatformSettingsDto updateSettings(Map<String, Object> updates) {
        Map<String, Object> before = new LinkedHashMap<>();
        platformSettingsRepository.findAll().forEach(s -> before.put(s.getSettingKey(), parseValue(s.getSettingValue())));

        updates.forEach((key, value) -> {
            PlatformSettings setting = platformSettingsRepository.findBySettingKey(key)
                    .orElseGet(() -> {
                        PlatformSettings s = new PlatformSettings();
                        s.setSettingKey(key);
                        return s;
                    });
            setting.setSettingValue(value == null ? "false" : value.toString());
            platformSettingsRepository.save(setting);
        });

        Map<String, Object> after = new LinkedHashMap<>();
        platformSettingsRepository.findAll().forEach(s -> after.put(s.getSettingKey(), parseValue(s.getSettingValue())));

        auditLogService.logWithResult("PLATFORM_SETTINGS_UPDATE", "PlatformSettings", null,
                "Updated platform settings", null, null, null,
                "SUCCESS", before, after, null);

        return getSettings();
    }

    private Object parseValue(String value) {
        if (value == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }
}
