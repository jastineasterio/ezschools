package com.edumanage.notifications.entity;

import com.edumanage.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_preferences", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"school_id", "user_id"})
})
@Getter
@Setter
public class NotificationPreference extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "in_app_enabled", nullable = false)
    private Boolean inAppEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = false;

    @Column(name = "sms_enabled", nullable = false)
    private Boolean smsEnabled = false;
}
