package com.edumanage.audit.service;

import com.edumanage.audit.dto.AuditChartPoint;
import com.edumanage.audit.dto.AuditDistributionItem;
import com.edumanage.audit.dto.AuditLogDto;
import com.edumanage.audit.dto.RecentAuditLogDto;
import com.edumanage.audit.entity.AuditLog;
import com.edumanage.audit.repository.AuditLogRepository;
import com.edumanage.audit.util.AuditRedactor;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final Set<String> SECURITY_ACTIONS = Set.of(
            "LOGIN", "LOGIN_FAILURE", "LOGOUT", "REFRESH",
            "PASSWORD_CHANGE", "PASSWORD_RESET", "ACCOUNT_ACTIVATE", "ACCOUNT_DEACTIVATE",
            "ACCESS_DENIED", "SESSION_INVALIDATED"
    );

    private final AuditLogRepository auditLogRepository;
    private final ActorResolver actorResolver;
    private final ObjectMapper objectMapper;

    @Transactional
    public void log(String action, String entityType, Long entityId, String description,
                    String ipAddress, String userAgent, Object metadata) {
        ActorResolver.Actor actor = actorResolver.currentActor();
        logInternal(action, entityType, entityId, description, ipAddress, userAgent,
                metadata, actor, null, null, "SUCCESS", TenantContext.getTenantId());
    }

    @Transactional
    public void logForSchool(String action, String entityType, Long entityId, String description,
                             String ipAddress, String userAgent, Object metadata, Long schoolId) {
        ActorResolver.Actor actor = actorResolver.currentActor();
        logInternal(action, entityType, entityId, description, ipAddress, userAgent,
                metadata, actor, null, null, "SUCCESS", schoolId);
    }

    @Transactional
    public void logWithResult(String action, String entityType, Long entityId, String description,
                              String ipAddress, String userAgent, Object metadata,
                              String result, Object beforeChanges, Object afterChanges, Long schoolId) {
        ActorResolver.Actor actor = actorResolver.currentActor();
        logInternal(action, entityType, entityId, description, ipAddress, userAgent,
                metadata, actor, beforeChanges, afterChanges, result, schoolId);
    }

    @Transactional
    public void logFailure(String action, String entityType, Long entityId, String description,
                           String ipAddress, String userAgent, Object metadata, Throwable error) {
        ActorResolver.Actor actor = actorResolver.currentActor();
        String safeDescription = description + " (failed: " + sanitizeMessage(error.getMessage()) + ")";
        logInternal(action, entityType, entityId, safeDescription, ipAddress, userAgent,
                metadata, actor, null, null, "FAILURE", TenantContext.getTenantId());
    }

    @Transactional
    public void logWithActor(String action, String entityType, Long entityId, String description,
                             String ipAddress, String userAgent, Object metadata, String result,
                             Long actorUserId, String actorName, String actorRole, Long schoolId) {
        logInternal(action, entityType, entityId, description, ipAddress, userAgent,
                metadata, new ActorResolver.Actor(actorUserId, actorName, actorRole, schoolId),
                null, null, result, schoolId);
    }

    private void logInternal(String action, String entityType, Long entityId, String description,
                             String ipAddress, String userAgent, Object metadata,
                             ActorResolver.Actor actor, Object beforeChanges, Object afterChanges,
                             String result, Long schoolId) {
        AuditLog log = new AuditLog();
        log.setActorUserId(actor.userId());
        log.setActorName(actor.name());
        log.setActorRole(actor.role());
        log.setSchoolId(schoolId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDescription(redact(description));
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setResult(result == null ? "SUCCESS" : result);
        log.setCreatedAt(LocalDateTime.now());
        log.setMetadata(toJson(redactObject(metadata)));
        log.setBeforeChanges(toJson(redactObject(beforeChanges)));
        log.setAfterChanges(toJson(redactObject(afterChanges)));
        auditLogRepository.save(log);
    }

    public PageResponse<AuditLogDto> getAuditLogs(Pageable pageable) {
        return getAuditLogs(pageable, null, null, null, null, null, null, null, null);
    }

    public PageResponse<AuditLogDto> getAuditLogs(Pageable pageable, String search, String action,
                                                   String entityType, LocalDateTime start, LocalDateTime end,
                                                   String result, Long actorUserId, Long schoolId) {
        Long tenantId = TenantContext.getTenantId();
        LocalDateTime endExclusive = endExclusive(end);
        Page<AuditLog> page;
        if (tenantId != null) {
            page = auditLogRepository.findFilteredBySchoolId(
                    tenantId, start, endExclusive, result, actorUserId, action, entityType, search, pageable);
        } else {
            page = auditLogRepository.findFiltered(
                    start, endExclusive, result, actorUserId, action, entityType, schoolId, search, pageable);
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public AuditLogDto getAuditLog(Long id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Audit log not found"));
        return mapToDto(log);
    }

    public AuditOverview getOverview(LocalDateTime start, LocalDateTime end) {
        return getOverview(start, end, null, null, null, null, null, null);
    }

    public AuditOverview getOverview(LocalDateTime start, LocalDateTime end, String search, String action,
                                      String entityType, String result, Long actorUserId, Long schoolId) {
        LocalDateTime endExclusive = endExclusive(end);
        long total = auditLogRepository.countFiltered(start, endExclusive, result, actorUserId, action,
                entityType, schoolId, search);
        long successes = auditLogRepository.countSuccessesFiltered(start, endExclusive, result, actorUserId, action,
                entityType, schoolId, search);
        long failures = auditLogRepository.countFailuresFiltered(start, endExclusive, result, actorUserId, action,
                entityType, schoolId, search);
        long uniqueActors = auditLogRepository.countDistinctActorsFiltered(start, endExclusive, result, actorUserId,
                action, entityType, schoolId, search);
        long securityEvents = countSecurityEvents(start, endExclusive, result, actorUserId, action, entityType,
                schoolId, search);
        return new AuditOverview(total, successes, failures, uniqueActors, securityEvents);
    }

    private LocalDateTime endExclusive(LocalDateTime end) {
        if (end == null) {
            return null;
        }
        return end.toLocalDate().plusDays(1).atStartOfDay();
    }

    private long countSecurityEvents(LocalDateTime start, LocalDateTime endExclusive, String result,
                                      Long actorUserId, String action, String entityType, Long schoolId,
                                      String search) {
        return SECURITY_ACTIONS.stream()
                .mapToLong(secAction -> auditLogRepository.countFiltered(
                        start, endExclusive, result, actorUserId, secAction, entityType, schoolId, search))
                .sum();
    }

    public List<AuditChartPoint> getActivityTrend(LocalDateTime start, LocalDateTime end, String search,
                                                   String action, String entityType, String result,
                                                   Long actorUserId, Long schoolId) {
        LocalDateTime endExclusive = endExclusive(end);
        return auditLogRepository.dailyTrend(start, endExclusive, result, actorUserId, action, entityType,
                schoolId, search).stream()
                .map(row -> new AuditChartPoint(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    public List<AuditDistributionItem> getActionDistribution(LocalDateTime start, LocalDateTime end, String search,
                                                              String action, String entityType, String result,
                                                              Long actorUserId, Long schoolId) {
        LocalDateTime endExclusive = endExclusive(end);
        return auditLogRepository.actionDistribution(start, endExclusive, result, actorUserId, action, entityType,
                schoolId, search).stream()
                .map(row -> new AuditDistributionItem(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    public List<AuditDistributionItem> getResultDistribution(LocalDateTime start, LocalDateTime end, String search,
                                                              String action, String entityType, String result,
                                                              Long actorUserId, Long schoolId) {
        LocalDateTime endExclusive = endExclusive(end);
        return auditLogRepository.resultDistribution(start, endExclusive, result, actorUserId, action, entityType,
                schoolId, search).stream()
                .map(row -> new AuditDistributionItem(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    private AuditLogDto mapToDto(AuditLog log) {
        AuditLogDto dto = new AuditLogDto();
        dto.setId(log.getId());
        dto.setActorUserId(log.getActorUserId());
        dto.setActorName(log.getActorName());
        dto.setActorRole(log.getActorRole());
        dto.setSchoolId(log.getSchoolId());
        dto.setAction(log.getAction());
        dto.setEntityType(log.getEntityType());
        dto.setEntityId(log.getEntityId());
        dto.setDescription(log.getDescription());
        dto.setResult(log.getResult());
        dto.setIpAddress(log.getIpAddress());
        dto.setUserAgent(log.getUserAgent());
        dto.setMetadata(log.getMetadata());
        dto.setBeforeChanges(log.getBeforeChanges());
        dto.setAfterChanges(log.getAfterChanges());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String redact(String value) {
        return value;
    }

    @SuppressWarnings("unchecked")
    private Object redactObject(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map) {
            return AuditRedactor.redactMap((Map<String, Object>) value);
        }
        return AuditRedactor.redact(value.toString());
    }

    private String sanitizeMessage(String message) {
        if (message == null) {
            return "unknown error";
        }
        return AuditRedactor.redact(message);
    }

    public record AuditOverview(long totalEvents, long successful, long failed,
                                long uniqueActors, long securityEvents) {
    }
}
