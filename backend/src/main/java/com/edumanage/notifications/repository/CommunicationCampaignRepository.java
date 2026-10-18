package com.edumanage.notifications.repository;

import com.edumanage.notifications.entity.CommunicationCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunicationCampaignRepository extends JpaRepository<CommunicationCampaign, Long> {
    Optional<CommunicationCampaign> findBySchoolIdAndSenderUserIdAndIdempotencyKey(
            Long schoolId, Long senderUserId, String idempotencyKey);

    Optional<CommunicationCampaign> findBySchoolIdAndId(Long schoolId, Long id);

    Page<CommunicationCampaign> findBySchoolIdOrderByCreatedAtDesc(Long schoolId, Pageable pageable);

    Page<CommunicationCampaign> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
