package com.edumanage.notifications.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_manager_channel_permissions")
@Getter
@Setter
public class ManagerChannelPermission {

    @Id
    @Column(name = "manager_user_id")
    private Long managerUserId;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "sms_enabled", nullable = false)
    private boolean smsEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;
}
