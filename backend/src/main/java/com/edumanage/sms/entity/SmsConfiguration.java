package com.edumanage.sms.entity;

import com.edumanage.common.entity.UuidBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sms_configurations")
@Getter
@Setter
public class SmsConfiguration extends UuidBaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "encrypted_api_key", nullable = false, columnDefinition = "TEXT")
    private String encryptedApiKey;

    @Column(name = "encrypted_api_secret", nullable = false, columnDefinition = "TEXT")
    private String encryptedApiSecret;

    @Column(name = "sender_id", length = 30)
    private String senderId;

    @Column(name = "monthly_limit")
    private Integer monthlyLimit;

    @Column(name = "daily_limit")
    private Integer dailyLimit;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
