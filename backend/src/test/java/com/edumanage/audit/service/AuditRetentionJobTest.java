package com.edumanage.audit.service;

import com.edumanage.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditRetentionJobTest {

    @Test
    void purgeDeletesInBatchesUntilNoneLeft() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        when(repository.countOlderThan(any())).thenReturn(7L);
        when(repository.deleteOlderThanBatch(any(), anyInt())).thenReturn(5000, 2, 0);

        AuditRetentionJob job = new AuditRetentionJob(repository);
        job.purgeExpiredAuditLogs();

        verify(repository).countOlderThan(any());
        verify(repository, times(3)).deleteOlderThanBatch(any(), anyInt());
    }

    @Test
    void purgeIsIdempotentWhenNothingEligible() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        when(repository.countOlderThan(any())).thenReturn(0L);
        when(repository.deleteOlderThanBatch(any(), anyInt())).thenReturn(0);

        AuditRetentionJob job = new AuditRetentionJob(repository);
        job.purgeExpiredAuditLogs();

        verify(repository).countOlderThan(any());
        verify(repository, times(1)).deleteOlderThanBatch(any(), anyInt());
    }

    @Test
    void cutoffIsExactly90Days() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        when(repository.countOlderThan(any())).thenReturn(0L);
        when(repository.deleteOlderThanBatch(any(), anyInt())).thenReturn(0);

        AuditRetentionJob job = new AuditRetentionJob(repository);
        job.purgeExpiredAuditLogs();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).countOlderThan(captor.capture());
        LocalDateTime expected = LocalDateTime.now().minusDays(90);
        assertTrue(captor.getValue().isBefore(expected.plusSeconds(2)),
                "cutoff should be approximately now minus 90 days");
        assertTrue(captor.getValue().isAfter(expected.minusSeconds(2)));
    }
}
