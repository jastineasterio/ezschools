package com.edumanage.notifications.repository;

import com.edumanage.notifications.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdAndSchoolIdOrderByCreatedAtDesc(Long userId, Long schoolId, Pageable pageable);
    List<Notification> findByUserIdAndSchoolIdAndIsReadFalseOrderByCreatedAtDesc(Long userId, Long schoolId);
    long countByUserIdAndSchoolIdAndIsReadFalse(Long userId, Long schoolId);
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
