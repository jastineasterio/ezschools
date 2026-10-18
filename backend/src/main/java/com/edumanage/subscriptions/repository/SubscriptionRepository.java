package com.edumanage.subscriptions.repository;

import com.edumanage.subscriptions.entity.Subscription;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUuid(UUID uuid);
    Optional<Subscription> findBySchoolIdAndStatus(Long schoolId, String status);
    List<Subscription> findBySchoolId(Long schoolId);
    Page<Subscription> findBySchoolId(Long schoolId, Pageable pageable);
    Page<Subscription> findAll(Pageable pageable);
    long countByStatus(String status);
}
