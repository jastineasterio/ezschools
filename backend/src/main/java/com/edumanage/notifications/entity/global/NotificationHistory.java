package com.edumanage.notifications.entity.global;

import com.edumanage.common.entity.UuidBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_history")
@Getter
@Setter
public class NotificationHistory extends UuidBaseEntity {
    @Column(name = "event_code", length = 100)
    private String eventCode;

    @Column(name = "channel", length = 50)
    private String channel;

    @Column(name = "recipient_type", length = 50)
    private String recipientType;

    @Column(name = "recipient_id")
    private Long recipientId;

    @Column(name = "recipient_identifier", length = 255)
    private String recipientIdentifier;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "provider", length = 100)
    private String provider;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
