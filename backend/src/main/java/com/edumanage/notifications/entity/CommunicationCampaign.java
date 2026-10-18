package com.edumanage.notifications.entity;

import com.edumanage.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "communication_campaigns")
@Getter
@Setter
public class CommunicationCampaign extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "category", nullable = false, length = 40)
    private String category;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "recipient_group", nullable = false, length = 30)
    private String recipientGroup;

    @Column(name = "intended_count", nullable = false)
    private int intendedCount;

    @Column(name = "excluded_count", nullable = false)
    private int excludedCount;

    @Column(name = "accepted_count", nullable = false)
    private int acceptedCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;
}
