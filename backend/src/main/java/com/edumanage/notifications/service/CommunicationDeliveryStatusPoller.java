package com.edumanage.notifications.service;

import com.edumanage.notifications.repository.CommunicationDeliveryRepository;
import com.edumanage.sms.service.SmsConfigurationService;
import com.edumanage.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduled poller that confirms DARSMS delivery status for non-terminal
 * {@code SENT} CommunicationDelivery records.
 *
 * <p>Only statuses explicitly confirmed by DARSMS are promoted to terminal:
 * {@code DELIVERED}. {@code QUEUED}, {@code SENT} and unknown provider statuses
 * remain non-terminal ({@code SENT}) and are never labelled as failed. A delivery
 * that the provider already accepted is never flipped to FAILED by this poller,
 * because a successful submission must not be reported as failed merely because a
 * delivery receipt has not arrived (or reports a non-delivered state).
 *
 * <p>Each delivery is polled per-school using that school's own DARSMS credentials;
 * global credentials are never used as a substitute.
 *
 * <p>Polling is idempotent (a delivery is only updated while still SENT) and
 * concurrent runs are guarded by a simple lock because the default Spring scheduler
 * is single-threaded.
 */
@Component
public class CommunicationDeliveryStatusPoller {

    private static final Logger log = LoggerFactory.getLogger(CommunicationDeliveryStatusPoller.class);

    private final CommunicationDeliveryRepository deliveryRepository;
    private final SmsConfigurationService smsConfigurationService;
    private final DarsmsProviderService darsmsProviderService;

    private final boolean enabled;
    private final int maxAttempts;
    private final long minIntervalMs;

    private final AtomicBoolean polling = new AtomicBoolean(false);
    private final Map<Long, Instant> lastPolledAt = new ConcurrentHashMap<>();
    private final Map<Long, Integer> pollCount = new ConcurrentHashMap<>();

    public CommunicationDeliveryStatusPoller(
            CommunicationDeliveryRepository deliveryRepository,
            SmsConfigurationService smsConfigurationService,
            DarsmsProviderService darsmsProviderService,
            @Value("${app.notifications.delivery-poll.enabled:true}") boolean enabled,
            @Value("${app.notifications.delivery-poll.max-attempts:24}") int maxAttempts,
            @Value("${app.notifications.delivery-poll.min-interval-ms:240000}") long minIntervalMs) {
        this.deliveryRepository = deliveryRepository;
        this.smsConfigurationService = smsConfigurationService;
        this.darsmsProviderService = darsmsProviderService;
        this.enabled = enabled;
        this.maxAttempts = maxAttempts;
        this.minIntervalMs = minIntervalMs;
    }

    @Scheduled(fixedDelayString = "${app.notifications.delivery-poll.interval-ms:300000}")
    public void pollAcceptedDeliveries() {
        if (!enabled || !polling.compareAndSet(false, true)) {
            return;
        }
        try {
            pollOnce();
        } finally {
            polling.set(false);
        }
    }

    private void pollOnce() {
        List<Object[]> rows = deliveryRepository.findAcceptedDeliveriesWithProviderMessageId();
        if (rows.isEmpty()) {
            return;
        }

        int polled = 0;
        for (Object[] row : rows) {
            Long deliveryId = ((Number) row[0]).longValue();
            Long schoolId = ((Number) row[1]).longValue();
            String messageId = (String) row[2];

            Instant last = lastPolledAt.get(deliveryId);
            if (last != null
                    && Duration.between(last, Instant.now()).toMillis() < minIntervalMs) {
                continue;
            }

            Integer count = pollCount.getOrDefault(deliveryId, 0);
            if (count >= maxAttempts) {
                continue;
            }

            pollCount.put(deliveryId, count + 1);
            lastPolledAt.put(deliveryId, Instant.now());

            TenantContext.setTenantId(schoolId);
            try {
                SmsConfigurationService.SmsCredentials creds =
                        smsConfigurationService.getCredentials(schoolId);
                DarsmsProviderService.MessageStatus providerStatus =
                        darsmsProviderService.getMessageStatus(creds.apiKey(), messageId);

                String mapped = mapProviderStatus(providerStatus.status());
                // Only a confirmed DELIVERY receipt promotes a submission to DELIVERED.
                // A provider-reported non-delivery is not promoted to FAILED here, because
                // the SMS was successfully accepted by the provider and must not be
                // reported as failed.
                if ("DELIVERED".equals(mapped)) {
                    updateDeliveryStatus(deliveryId, mapped, null);
                    polled++;
                }
            } catch (NotificationProviderException e) {
                // Provider errors (auth, rate limit, unavailable) are transient or
                // permanent provider-side issues; do not flip delivery status.
                log.warn("Delivery poll failed for deliveryId={} schoolId={} code={}",
                        deliveryId, schoolId, e.getFailureCode());
            } finally {
                TenantContext.clear();
            }
        }

        if (polled > 0 && log.isInfoEnabled()) {
            log.info("Delivery poll confirmed {} delivery status(es)", polled);
        }
    }

    private void updateDeliveryStatus(Long deliveryId, String status, String errorMessage) {
        var delivery = deliveryRepository.findById(deliveryId).orElse(null);
        if (delivery == null || !"SENT".equals(delivery.getStatus())) {
            // Already updated by another run or no longer exists: idempotent no-op.
            return;
        }
        delivery.setStatus(status);
        delivery.setDeliveryStatus(status);
        if ("DELIVERED".equals(status)) {
            delivery.setDeliveredAt(LocalDateTime.now());
        }
        deliveryRepository.save(delivery);
    }

    private String mapProviderStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status.toUpperCase(java.util.Locale.ROOT)) {
            case "DELIVERED" -> "DELIVERED";
            // QUEUED, SENT, FAILED and any unknown provider status remain non-terminal.
            // A delivery already accepted by the provider is never flipped to FAILED here.
            default -> null;
        };
    }
}
