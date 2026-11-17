package com.edumanage.resultsnotifications.repository;

import com.edumanage.resultsnotifications.entity.ResultNotificationDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultNotificationDeliveryRepository extends JpaRepository<ResultNotificationDelivery, Long> {
    List<ResultNotificationDelivery> findByNotificationIdOrderByCreatedAtAsc(Long notificationId);

    List<ResultNotificationDelivery> findByNotificationIdAndStatus(Long notificationId, String status);

    Optional<ResultNotificationDelivery> findBySmsMessageId(Long smsMessageId);

    /**
     * Atomically marks a single delivery as RETRYING so it can be retried.
     * Only FAILED deliveries are eligible, preventing duplicate SMS for
     * SENT/DELIVERED/EXCLUDED/PENDING records. Returns the number of rows updated.
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE result_notification_deliveries SET status = 'RETRYING' WHERE id = :id AND status = 'FAILED'",
            nativeQuery = true)
    int markPendingDeliveryForRetry(Long id);
}
