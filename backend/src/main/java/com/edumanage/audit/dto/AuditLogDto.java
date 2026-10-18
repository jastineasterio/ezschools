package com.edumanage.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    private Long id;
    private Long actorUserId;
    private Long schoolId;
    private String action;
    private String entityType;
    private Long entityId;
    private String description;
    private String ipAddress;
    private String userAgent;
    private String metadata;
    private LocalDateTime createdAt;
}
