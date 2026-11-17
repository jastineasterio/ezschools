package com.edumanage.resultsnotifications.entity;

import com.edumanage.common.entity.UuidBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "result_notification_deliveries")
@Getter
@Setter
public class ResultNotificationDelivery extends UuidBaseEntity {

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "sms_message_id")
    private Long smsMessageId;

    @Column(name = "recipient_type", nullable = false, length = 20)
    private String recipientType;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "recipient_name", nullable = false, length = 255)
    private String recipientName;

    @Column(name = "recipient_phone", length = 50)
    private String recipientPhone;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "student_name", length = 255)
    private String studentName;

    @Column(name = "admission_number", length = 100)
    private String admissionNumber;

    @Column(name = "channel", nullable = false, length = 10)
    private String channel = "SMS";

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "batch_id", length = 255)
    private String batchId;

    @Column(name = "failure_code", length = 60)
    private String failureCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "delivery_status", length = 20)
    private String deliveryStatus;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;
}
