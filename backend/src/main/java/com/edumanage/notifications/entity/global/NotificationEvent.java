package com.edumanage.notifications.entity.global;

import com.edumanage.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_events")
@Getter
@Setter
public class NotificationEvent extends BaseEntity {
    @Column(name = "event_code", nullable = false, unique = true, length = 100)
    private String eventCode;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "description")
    private String description;

    @Column(name = "default_enabled", nullable = false)
    private boolean defaultEnabled = true;

    @Column(name = "target_role", length = 50)
    private String targetRole;
}
