package com.edumanage.notifications.entity.global;

import com.edumanage.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_global_settings")
@Getter
@Setter
public class NotificationGlobalSettings extends BaseEntity {
    @Column(name = "setting_key", nullable = false, unique = true, length = 150)
    private String settingKey;

    @Column(name = "setting_value")
    private String settingValue;

    @Column(name = "description")
    private String description;
}
