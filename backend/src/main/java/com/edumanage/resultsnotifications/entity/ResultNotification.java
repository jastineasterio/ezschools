package com.edumanage.resultsnotifications.entity;

import com.edumanage.common.entity.UuidBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "result_notifications")
@Getter
@Setter
public class ResultNotification extends UuidBaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "examination_id", nullable = false)
    private Long examinationId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "stream_id")
    private Long streamId;

    @Column(name = "recipient_type", nullable = false, length = 20)
    private String recipientType;

    @Column(name = "idempotency_key", nullable = false, length = 100, unique = true)
    private String idempotencyKey;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "total_recipients", nullable = false)
    private int totalRecipients;

    @Column(name = "excluded_count", nullable = false)
    private int excludedCount;

    @Column(name = "successful_count", nullable = false)
    private int successfulCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "pending_count", nullable = false)
    private int pendingCount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "initiated_by")
    private Long initiatedBy;
}
