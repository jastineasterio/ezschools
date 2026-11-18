package com.edumanage.audit.service;

import com.edumanage.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditRetentionJob {

    private static final int RETENTION_DAYS = 90;
    private static final int BATCH_SIZE = 5000;

    private final AuditLogRepository auditLogRepository;

    /**
     * Runs once daily. Permanently deletes audit log records older than the exact
     * 90-day retention cutoff, in safe database-side batches. Idempotent: records
     * within the retention window are never touched and a second run deletes
     * nothing once the cutoff has passed.
     */
    @Scheduled(cron = "${app.audit.retention.cron:0 2 2 * * ?}")
    @Transactional
    public void purgeExpiredAuditLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        long remainingBefore = auditLogRepository.countOlderThan(cutoff);
        int totalDeleted = 0;
        int deleted;
        do {
            deleted = auditLogRepository.deleteOlderThanBatch(cutoff, BATCH_SIZE);
            totalDeleted += deleted;
        } while (deleted > 0);
        log.info("Audit retention purge completed: cutoff={} (older than {} days), "
                + "records eligible before={} records deleted={}",
                cutoff, RETENTION_DAYS, remainingBefore, totalDeleted);
    }
}
