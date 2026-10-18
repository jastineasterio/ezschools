package com.edumanage.notifications.service;

import com.edumanage.notifications.dto.CreateNotificationRequest;
import com.edumanage.notifications.entity.global.NotificationEvent;
import com.edumanage.notifications.entity.global.NotificationEventChannel;
import com.edumanage.notifications.entity.global.NotificationHistory;
import com.edumanage.notifications.repository.global.NotificationEventChannelRepository;
import com.edumanage.notifications.repository.global.NotificationEventRepository;
import com.edumanage.notifications.repository.global.NotificationHistoryRepository;
import com.edumanage.notifications.service.providers.EmailProviderService;
import com.edumanage.notifications.service.providers.SmsProviderService;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcherService {
    private final NotificationEventRepository eventRepository;
    private final NotificationEventChannelRepository eventChannelRepository;
    private final NotificationHistoryRepository historyRepository;
    private final SmsProviderService smsProviderService;
    private final EmailProviderService emailProviderService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Transactional
    public void dispatchToManagers(String eventCode, String message, String title) {
        eventRepository.findByEventCode(eventCode).ifPresent(event -> {
            List<NotificationEventChannel> channels = eventChannelRepository.findByEventId(event.getId());
            List<User> managers = userRepository.findAll().stream()
                    .filter(u -> u.getRoles() != null && u.getRoles().stream()
                            .anyMatch(r -> "MANAGER".equalsIgnoreCase(r.getName())))
                    .filter(User::isActive)
                    .collect(java.util.stream.Collectors.toList());

            for (NotificationEventChannel channel : channels) {
                if (!channel.isEnabled()) continue;
                if ("SMS".equalsIgnoreCase(channel.getChannel())) {
                    for (User m : managers) {
                        if (m.getPhone() != null && !m.getPhone().isBlank()) {
                            var res = smsProviderService.sendSms(m.getPhone(), message);
                            logHistory(eventCode, "SMS", m, res);
                        }
                    }
                } else if ("EMAIL".equalsIgnoreCase(channel.getChannel())) {
                    for (User m : managers) {
                        if (m.getEmail() != null && !m.getEmail().isBlank()) {
                            var res = emailProviderService.sendEmail(m.getEmail(), title != null ? title : message, message);
                            logHistory(eventCode, "EMAIL", m, res);
                        }
                    }
                } else if ("IN_APP".equalsIgnoreCase(channel.getChannel())) {
                    for (User m : managers) {
                        CreateNotificationRequest req = new CreateNotificationRequest();
                        req.setTitle(title != null ? title : event.getDisplayName());
                        req.setMessage(message);
                        req.setNotificationType("SYSTEM");
                        req.setPriority("NORMAL");
                        notificationService.create(req, m.getId(), m.getSchoolId());
                        logInAppHistory(eventCode, m);
                    }
                }
            }
        });
    }

    private void logHistory(String eventCode, String channel, User m, java.util.Map<String, Object> res) {
        NotificationHistory history = new NotificationHistory();
        history.setEventCode(eventCode);
        history.setChannel(channel);
        history.setRecipientType(channel.equalsIgnoreCase("SMS") ? "PHONE" : "EMAIL");
        history.setRecipientId(m.getId());
        history.setRecipientIdentifier(channel.equalsIgnoreCase("SMS") ? m.getPhone() : m.getEmail());
        history.setMessage("");
        history.setStatus(Boolean.TRUE.equals(res.get("success")) ? "SENT" : "FAILED");
        history.setProvider((String) res.get("provider"));
        history.setProviderMessageId((String) res.get("providerMessageId"));
        history.setErrorMessage((String) res.get("error"));
        if ("SENT".equals(history.getStatus())) history.setSentAt(LocalDateTime.now());
        historyRepository.save(history);
    }

    private void logInAppHistory(String eventCode, User m) {
        NotificationHistory history = new NotificationHistory();
        history.setEventCode(eventCode);
        history.setChannel("IN_APP");
        history.setRecipientType("USER");
        history.setRecipientId(m.getId());
        history.setRecipientIdentifier(m.getEmail());
        history.setStatus("SENT");
        history.setSentAt(LocalDateTime.now());
        historyRepository.save(history);
    }
}
