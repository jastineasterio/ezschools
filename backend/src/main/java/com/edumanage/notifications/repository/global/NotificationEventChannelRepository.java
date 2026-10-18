package com.edumanage.notifications.repository.global;

import com.edumanage.notifications.entity.global.NotificationEventChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationEventChannelRepository extends JpaRepository<NotificationEventChannel, Long> {
    List<NotificationEventChannel> findByEventId(Long eventId);
}
