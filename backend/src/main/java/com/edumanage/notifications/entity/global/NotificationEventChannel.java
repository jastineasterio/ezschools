package com.edumanage.notifications.entity.global;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_event_channels")
@Getter
@Setter
public class NotificationEventChannel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "channel", nullable = false, length = 50)
    private String channel;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
}
