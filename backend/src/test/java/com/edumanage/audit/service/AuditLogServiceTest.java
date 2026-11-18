package com.edumanage.audit.service;

import com.edumanage.audit.dto.AuditChartPoint;
import com.edumanage.audit.dto.AuditDistributionItem;
import com.edumanage.audit.dto.AuditLogDto;
import com.edumanage.audit.entity.AuditLog;
import com.edumanage.audit.repository.AuditLogRepository;
import com.edumanage.audit.util.AuditRedactor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogServiceTest {

    private AuditLogRepository repository;
    private ActorResolver actorResolver;
    private ObjectMapper objectMapper;
    private AuditLogService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuditLogRepository.class);
        actorResolver = mock(ActorResolver.class);
        objectMapper = new ObjectMapper();
        service = new AuditLogService(repository, actorResolver, objectMapper);
    }

    @Test
    void logWithActorStoresActorAndResult() {
        when(actorResolver.currentActor())
                .thenReturn(new ActorResolver.Actor(42L, "Jane Admin", "ROLE_SUPER_ADMIN", null));

        service.logWithActor("SCHOOL_UPDATE", "School", 7L, "Renamed school",
                "127.0.0.1", "Mozilla/5.0", Map.of("key", "value"),
                "SUCCESS", 42L, "Jane Admin", "ROLE_SUPER_ADMIN", 7L);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals(42L, log.getActorUserId());
        assertEquals("Jane Admin", log.getActorName());
        assertEquals("ROLE_SUPER_ADMIN", log.getActorRole());
        assertEquals("SCHOOL_UPDATE", log.getAction());
        assertEquals("SUCCESS", log.getResult());
        assertEquals(7L, log.getSchoolId());
        assertNotNull(log.getCreatedAt());
    }

    @Test
    void logWithActorDefaultsResultToSuccessWhenNull() {
        when(actorResolver.currentActor())
                .thenReturn(new ActorResolver.Actor(null, "System", null, null));

        service.logWithActor("LOGIN", "Auth", null, "User logged in",
                null, null, null, null, null, null, null, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertEquals("SUCCESS", captor.getValue().getResult());
    }

    @Test
    void overviewAggregatesRepositoryCounts() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 31, 23, 59);

        when(repository.countFiltered(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    String action = inv.getArgument(4, String.class);
                    return action == null ? 100L : 1L;
                });
        when(repository.countSuccessesFiltered(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(80L);
        when(repository.countFailuresFiltered(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(20L);
        when(repository.countDistinctActorsFiltered(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(5L);

        AuditLogService.AuditOverview overview = service.getOverview(start, end, null, null, null, null, null, null);

        assertEquals(100L, overview.totalEvents());
        assertEquals(80L, overview.successful());
        assertEquals(20L, overview.failed());
        assertEquals(5L, overview.uniqueActors());
        assertEquals(10L, overview.securityEvents());
    }

    @Test
    void sensitiveMetadataKeysAreRedacted() {
        when(actorResolver.currentActor())
                .thenReturn(new ActorResolver.Actor(1L, "Actor", "ROLE_SUPER_ADMIN", null));

        service.logWithActor("USER_UPDATE", "User", 9L, "Updated user",
                null, null, Map.of("password", "s3cret", "token", "abc", "displayName", "John"),
                "SUCCESS", null, null, null, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        String metadata = captor.getValue().getMetadata();
        assertNotNull(metadata);
        assertFalse(metadata.contains("s3cret"));
        assertFalse(metadata.contains("abc"));
        assertTrue(metadata.contains("John"));
        assertTrue(metadata.contains("[REDACTED]"));
    }

    @Test
    void getAuditLogMapsAllFields() {
        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setActorUserId(2L);
        log.setActorName("Bob");
        log.setActorRole("ROLE_TEACHER");
        log.setSchoolId(3L);
        log.setAction("LOGIN");
        log.setEntityType("Auth");
        log.setEntityId(2L);
        log.setDescription("User logged in");
        log.setResult("SUCCESS");
        log.setIpAddress("10.0.0.1");
        log.setUserAgent("ua");
        log.setCreatedAt(LocalDateTime.now());
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(log));

        AuditLogDto dto = service.getAuditLog(1L);

        assertEquals(1L, dto.getId());
        assertEquals(2L, dto.getActorUserId());
        assertEquals("Bob", dto.getActorName());
        assertEquals("ROLE_TEACHER", dto.getActorRole());
        assertEquals(3L, dto.getSchoolId());
        assertEquals("LOGIN", dto.getAction());
        assertEquals("SUCCESS", dto.getResult());
        assertEquals("10.0.0.1", dto.getIpAddress());
        assertEquals("ua", dto.getUserAgent());
    }

    @Test
    void auditRedactorMasksSensitiveKeys() {
        assertEquals("[REDACTED]", AuditRedactor.redact("password"));
        assertEquals("[REDACTED]", AuditRedactor.redact("api_key"));
        assertEquals("hello", AuditRedactor.redact("hello"));
        assertNull(AuditRedactor.redact(null));
    }

    @Test
    void activityTrendMapsRepositoryRows() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 31, 23, 59);

        when(repository.dailyTrend(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(new Object[] { "2026-01-05", 3L }, new Object[] { "2026-01-06", 0L }));

        List<AuditChartPoint> trend = service.getActivityTrend(start, end, null, null, null, null, null, null);

        assertEquals(2, trend.size());
        assertEquals("2026-01-05", trend.get(0).period());
        assertEquals(3L, trend.get(0).count());
        assertEquals("2026-01-06", trend.get(1).period());
        assertEquals(0L, trend.get(1).count());
    }

    @Test
    void actionDistributionMapsRepositoryRows() {
        when(repository.actionDistribution(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(new Object[] { "LOGIN", 10L }, new Object[] { "LOGOUT", 4L }));

        List<AuditDistributionItem> distribution = service.getActionDistribution(
                null, null, null, null, null, null, null, null);

        assertEquals(2, distribution.size());
        assertEquals("LOGIN", distribution.get(0).name());
        assertEquals(10L, distribution.get(0).count());
        assertEquals("LOGOUT", distribution.get(1).name());
        assertEquals(4L, distribution.get(1).count());
    }

    @Test
    void resultDistributionMapsRepositoryRows() {
        when(repository.resultDistribution(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(new Object[] { "SUCCESS", 40L }, new Object[] { "FAILURE", 2L }));

        List<AuditDistributionItem> distribution = service.getResultDistribution(
                null, null, null, null, null, null, null, null);

        assertEquals(2, distribution.size());
        assertEquals("SUCCESS", distribution.get(0).name());
        assertEquals(40L, distribution.get(0).count());
        assertEquals("FAILURE", distribution.get(1).name());
        assertEquals(2L, distribution.get(1).count());
    }
}
