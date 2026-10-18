package com.edumanage.notifications.repository.global;

import com.edumanage.notifications.entity.global.NotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {
    Optional<NotificationEvent> findByEventCode(String eventCode);
}
