package com.edumanage.notifications.entity.global;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_templates")
@Getter
@Setter
public class NotificationTemplate extends SoftDeletableEntity {
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "channel", nullable = false, length = 50)
    private String channel;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "variables", columnDefinition = "JSONB")
    private String variables;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
