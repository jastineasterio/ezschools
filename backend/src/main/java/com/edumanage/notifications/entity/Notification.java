package com.edumanage.notifications.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification extends SoftDeletableEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType = "SYSTEM";

    @Column(name = "priority", nullable = false, length = 20)
    private String priority = "NORMAL";

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;
}
