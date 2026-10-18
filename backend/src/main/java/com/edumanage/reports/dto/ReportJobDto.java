package com.edumanage.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportJobDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long requestedBy;
    private String reportType;
    private String reportName;
    private String parameters;
    private String status;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
