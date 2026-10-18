package com.edumanage.notifications.repository.global;

import com.edumanage.notifications.entity.global.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {
    Page<NotificationHistory> findAll(Pageable pageable);
    Page<NotificationHistory> findByChannel(String channel, Pageable pageable);
    Page<NotificationHistory> findByStatus(String status, Pageable pageable);
}
