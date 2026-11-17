package com.edumanage.notifications.entity;

import com.edumanage.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "communication_deliveries", uniqueConstraints = @UniqueConstraint(
        columnNames = {"campaign_id", "recipient_type", "recipient_id", "channel"}))
@Getter
@Setter
public class CommunicationDelivery extends BaseEntity {

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "recipient_type", nullable = false, length = 30)
    private String recipientType;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "channel", nullable = false, length = 10)
    private String channel;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

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

    @Column(name = "recipient_name", length = 255)
    private String recipientName;

    @Column(name = "recipient_phone", length = 50)
    private String recipientPhone;
}
