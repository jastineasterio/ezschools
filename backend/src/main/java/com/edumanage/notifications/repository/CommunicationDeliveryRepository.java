package com.edumanage.notifications.repository;

import com.edumanage.notifications.entity.CommunicationDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CommunicationDeliveryRepository extends JpaRepository<CommunicationDelivery, Long> {
    List<CommunicationDelivery> findByCampaignId(Long campaignId);

    List<CommunicationDelivery> findByCampaignIdAndStatus(Long campaignId, String status);

    Optional<CommunicationDelivery> findByCampaignIdAndRecipientTypeAndRecipientIdAndChannel(
            Long campaignId, String recipientType, Long recipientId, String channel);

    /**
     * Returns non-terminal SENT deliveries that carry a provider message id and
     * whose campaign belongs to a school, so they can be polled for delivery
     * confirmation. Excludes terminal (DELIVERED/FAILED) records.
     */
    @Query(value = """
            SELECT d.id, c.school_id, d.provider_message_id
            FROM communication_deliveries d
            JOIN communication_campaigns c ON c.id = d.campaign_id
            WHERE d.status = 'SENT' AND d.provider_message_id IS NOT NULL
            """, nativeQuery = true)
    List<Object[]> findAcceptedDeliveriesWithProviderMessageId();

    @Modifying
    @Transactional
    @Query(value = "UPDATE communication_deliveries SET status = 'RETRYING' WHERE id = :id AND status = 'FAILED'",
            nativeQuery = true)
    int markFailedDeliveryForRetry(Long id);

    @Query(value = """
            SELECT d.*
            FROM communication_deliveries d
            JOIN communication_campaigns c ON c.id = d.campaign_id
            WHERE c.school_id = ?1
            """, nativeQuery = true)
    Page<CommunicationDelivery> findBySchoolId(Long schoolId, Pageable pageable);
}
