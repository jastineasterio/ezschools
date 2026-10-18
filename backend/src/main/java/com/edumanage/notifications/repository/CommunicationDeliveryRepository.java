package com.edumanage.notifications.repository;

import com.edumanage.notifications.entity.CommunicationDelivery;
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

    @Modifying
    @Transactional
    @Query(value = "UPDATE communication_deliveries SET status = 'RETRYING' WHERE id = :id AND status = 'FAILED'",
            nativeQuery = true)
    int markFailedDeliveryForRetry(Long id);
}
