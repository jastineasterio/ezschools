package com.edumanage.resultsnotifications.repository;

import com.edumanage.resultsnotifications.entity.ResultNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResultNotificationRepository extends JpaRepository<ResultNotification, Long> {
    Page<ResultNotification> findBySchoolIdOrderByCreatedAtDesc(Long schoolId, Pageable pageable);

    Optional<ResultNotification> findBySchoolIdAndId(Long schoolId, Long id);

    Optional<ResultNotification> findBySchoolIdAndIdempotencyKey(Long schoolId, String idempotencyKey);

    boolean existsBySchoolIdAndIdempotencyKey(Long schoolId, String idempotencyKey);
}
