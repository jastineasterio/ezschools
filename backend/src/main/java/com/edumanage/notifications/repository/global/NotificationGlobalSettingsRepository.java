package com.edumanage.notifications.repository.global;

import com.edumanage.notifications.entity.global.NotificationGlobalSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationGlobalSettingsRepository extends JpaRepository<NotificationGlobalSettings, Long> {
    Optional<NotificationGlobalSettings> findBySettingKey(String settingKey);
}
