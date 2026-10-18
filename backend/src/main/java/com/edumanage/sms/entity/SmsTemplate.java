package com.edumanage.sms.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sms_templates")
@Getter
@Setter
public class SmsTemplate extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "variables", columnDefinition = "JSONB")
    private String variables;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
