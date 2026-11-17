package com.edumanage.resultsnotifications.entity;

import com.edumanage.common.entity.UuidBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "result_notification_settings",
        uniqueConstraints = @UniqueConstraint(columnNames = "school_id"))
@Getter
@Setter
public class ResultNotificationSetting extends UuidBaseEntity {

    @Column(name = "school_id", nullable = false, unique = true)
    private Long schoolId;

    @Column(name = "sms_template", columnDefinition = "TEXT")
    private String smsTemplate;

    @Column(name = "updated_by")
    private Long updatedBy;
}
