package com.edumanage.reports.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_jobs")
@Getter
@Setter
public class ReportJob extends SoftDeletableEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Column(name = "report_type", nullable = false, length = 100)
    private String reportType;

    @Column(name = "report_name", nullable = false, length = 255)
    private String reportName;

    @Column(name = "parameters", columnDefinition = "JSONB")
    private String parameters;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
