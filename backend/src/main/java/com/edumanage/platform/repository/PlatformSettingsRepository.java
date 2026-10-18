package com.edumanage.platform.repository;

import com.edumanage.platform.entity.PlatformSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformSettingsRepository extends JpaRepository<PlatformSettings, Long> {
    Optional<PlatformSettings> findBySettingKey(String settingKey);
}
