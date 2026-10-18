package com.edumanage.notifications.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.notifications.dto.ManagerChannelPermissionDto;
import com.edumanage.notifications.dto.NotificationChannelsDto;
import com.edumanage.notifications.entity.ManagerChannelPermission;
import com.edumanage.notifications.repository.ManagerChannelPermissionRepository;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommunicationSettingsService {

    private final JdbcTemplate jdbcTemplate;
    private final ManagerChannelPermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final DarsmsProviderService darsmsProvider;
    private final EmailNotificationProviderService emailProvider;
    private final AuditLogService auditLogService;

    public CommunicationSettingsService(JdbcTemplate jdbcTemplate,
                                        ManagerChannelPermissionRepository permissionRepository,
                                        UserRepository userRepository,
                                        DarsmsProviderService darsmsProvider,
                                        EmailNotificationProviderService emailProvider,
                                        AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.darsmsProvider = darsmsProvider;
        this.emailProvider = emailProvider;
        this.auditLogService = auditLogService;
    }

    public NotificationChannelsDto getGlobalSettings() {
        boolean global = settingEnabled("notifications.global.enabled", true);
        boolean sms = settingEnabled("sms.enabled", true);
        boolean email = settingEnabled("email.enabled", true);
        return new NotificationChannelsDto(global, sms, darsmsProvider.isConfigured(),
                email, emailProvider.isConfigured());
    }

    public boolean isChannelAllowed(Long managerUserId, Long schoolId, String channel) {
        NotificationChannelsDto settings = getGlobalSettings();
        if (!settings.notificationsEnabled()) {
            return false;
        }
        boolean globallyEnabled = switch (channel) {
            case "SMS" -> settings.smsEnabled() && settings.smsConfigured();
            case "EMAIL" -> settings.emailEnabled() && settings.emailConfigured();
            default -> false;
        };
        if (!globallyEnabled) {
            return false;
        }
        ManagerChannelPermission permission = permissionRepository.findByManagerUserId(managerUserId).orElse(null);
        if (permission == null) {
            return true;
        }
        if (!schoolId.equals(permission.getSchoolId())) {
            return false;
        }
        return "SMS".equals(channel) ? permission.isSmsEnabled() : permission.isEmailEnabled();
    }

    public String disabledReason(Long managerUserId, Long schoolId, String channel) {
        NotificationChannelsDto settings = getGlobalSettings();
        if (!settings.notificationsEnabled()) {
            return "Notifications are disabled globally";
        }
        boolean enabled = "SMS".equals(channel) ? settings.smsEnabled() : settings.emailEnabled();
        boolean configured = "SMS".equals(channel) ? settings.smsConfigured() : settings.emailConfigured();
        if (!enabled) {
            return channel + " is disabled globally";
        }
        if (!configured) {
            return channel + " provider is not configured";
        }
        ManagerChannelPermission permission = permissionRepository.findByManagerUserId(managerUserId).orElse(null);
        if (permission != null && (!schoolId.equals(permission.getSchoolId())
                || ("SMS".equals(channel) && !permission.isSmsEnabled())
                || ("EMAIL".equals(channel) && !permission.isEmailEnabled()))) {
            return "This channel is disabled for your manager account";
        }
        return null;
    }

    @Transactional
    public NotificationChannelsDto updateGlobalSettings(boolean notifications, boolean sms, boolean email) {
        setSetting("notifications.global.enabled", notifications);
        setSetting("sms.enabled", sms);
        setSetting("email.enabled", email);
        auditLogService.log("NOTIFICATION_SETTINGS_UPDATE", "NotificationSettings", null,
                "Updated global notification channel settings", null, null, null);
        return getGlobalSettings();
    }

    @Transactional(readOnly = true)
    public List<ManagerChannelPermissionDto> listManagers() {
        return userRepository.findManagers().stream().map(this::mapManager).toList();
    }

    @Transactional
    public ManagerChannelPermissionDto updateManager(Long managerId, boolean smsEnabled, boolean emailEnabled) {
        User manager = userRepository.findById(managerId)
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> "MANAGER".equalsIgnoreCase(role.getName())))
                .orElseThrow(() -> new com.edumanage.exception.ResourceNotFoundException("Manager not found"));
        if (manager.getSchoolId() == null || manager.isDeleted()) {
            throw new ForbiddenException("Manager is not assigned to a school");
        }

        ManagerChannelPermission permission = permissionRepository.findByManagerUserId(managerId)
                .orElseGet(() -> {
                    ManagerChannelPermission created = new ManagerChannelPermission();
                    created.setManagerUserId(managerId);
                    created.setSchoolId(manager.getSchoolId());
                    return created;
                });
        permission.setSmsEnabled(smsEnabled);
        permission.setEmailEnabled(emailEnabled);
        permissionRepository.save(permission);
        auditLogService.log("NOTIFICATION_MANAGER_PERMISSION_UPDATE", "User", managerId,
                "Updated notification channel access for manager", null, null, null);
        return mapManager(manager);
    }

    private ManagerChannelPermissionDto mapManager(User manager) {
        ManagerChannelPermission permission = permissionRepository.findByManagerUserId(manager.getId()).orElse(null);
        return new ManagerChannelPermissionDto(manager.getId(), manager.getEmail(), manager.getSchoolId(),
                permission == null || permission.isSmsEnabled(),
                permission == null || permission.isEmailEnabled());
    }

    private boolean settingEnabled(String key, boolean defaultValue) {
        List<String> values = jdbcTemplate.query(
                "SELECT setting_value FROM notification_global_settings WHERE setting_key = ?",
                (resultSet, row) -> resultSet.getString(1), key);
        return values.isEmpty() ? defaultValue : Boolean.parseBoolean(values.get(0));
    }

    private void setSetting(String key, boolean value) {
        jdbcTemplate.update("""
                INSERT INTO notification_global_settings (setting_key, setting_value, description)
                VALUES (?, ?, 'Managed by Super Admin')
                ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value,
                    updated_at = CURRENT_TIMESTAMP
                """, key, Boolean.toString(value));
    }
}
