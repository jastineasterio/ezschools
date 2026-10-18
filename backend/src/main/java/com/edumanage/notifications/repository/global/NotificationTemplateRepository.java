package com.edumanage.notifications.repository.global;

import com.edumanage.notifications.entity.global.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Page<NotificationTemplate> findByIsActive(boolean active, Pageable pageable);
    Page<NotificationTemplate> findByChannel(String channel, Pageable pageable);
    Optional<NotificationTemplate> findByNameAndChannel(String name, String channel);
}
