package com.edumanage.sms.entity;

import com.edumanage.common.entity.UuidBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sms_messages")
@Getter
@Setter
public class SmsMessage extends UuidBaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "recipient", nullable = false, length = 50)
    private String recipient;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "provider_response", columnDefinition = "TEXT")
    private String providerResponse;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "sent_at")
    private java.time.LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private java.time.LocalDateTime deliveredAt;

    @Column(name = "failed_at")
    private java.time.LocalDateTime failedAt;

    @Column(name = "created_by")
    private Long createdBy;
}
