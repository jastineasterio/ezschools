package com.edumanage.audit.dto;

import java.time.LocalDateTime;

/**
 * Lightweight representation of a recent audit event, safe for platform
 * dashboard display (no before/after changes, metadata or other sensitive
 * payloads).
 */
public record RecentAuditLogDto(
        Long id,
        Long actorUserId,
        String actorName,
        String actorRole,
        Long schoolId,
        String action,
        String entityType,
        Long entityId,
        String result,
        LocalDateTime createdAt) {
}
