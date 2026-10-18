package com.edumanage.notifications.service.management;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.notifications.dto.global.*;
import com.edumanage.notifications.dto.settings.NotificationEventDto;
import com.edumanage.notifications.dto.settings.NotificationSettingsDto;
import com.edumanage.notifications.dto.settings.TestNotificationRequest;
import com.edumanage.notifications.dto.settings.UpdateNotificationSettingsRequest;
import com.edumanage.notifications.entity.global.*;
import com.edumanage.notifications.repository.global.*;
import com.edumanage.notifications.service.providers.EmailProviderService;
import com.edumanage.notifications.service.providers.SmsProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationManagementService {
    private final NotificationGlobalSettingsRepository globalSettingsRepository;
    private final NotificationEventRepository eventRepository;
    private final NotificationEventChannelRepository eventChannelRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationHistoryRepository historyRepository;
    private final SmsProviderService smsProviderService;
    private final EmailProviderService emailProviderService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public NotificationSettingsDto getSettings() {
        Map<String, Object> globalSettings = new HashMap<>();
        globalSettingsRepository.findAll().forEach(s -> {
            globalSettings.put(s.getSettingKey(), parseValue(s.getSettingValue()));
        });

        List<NotificationEvent> events = eventRepository.findAll();
        List<NotificationEventDto> eventDtos = new ArrayList<>();
        for (NotificationEvent event : events) {
            List<NotificationEventChannel> channels = eventChannelRepository.findByEventId(event.getId());
            Map<String, Boolean> channelMap = new HashMap<>();
            channelMap.put("SMS", channels.stream().anyMatch(c -> "SMS".equals(c.getChannel()) && c.isEnabled()));
            channelMap.put("EMAIL", channels.stream().anyMatch(c -> "EMAIL".equals(c.getChannel()) && c.isEnabled()));
            channelMap.put("IN_APP", channels.stream().anyMatch(c -> "IN_APP".equals(c.getChannel()) && c.isEnabled()));
            boolean enabled = channels.stream().anyMatch(NotificationEventChannel::isEnabled);
            eventDtos.add(new NotificationEventDto(event.getId(), event.getEventCode(), event.getDisplayName(),
                    event.getDescription(), enabled, channelMap));
        }

        return new NotificationSettingsDto(globalSettings, eventDtos);
    }

    @Transactional
    public NotificationSettingsDto updateSettings(UpdateNotificationSettingsRequest request) {
        if (request.getGlobalSettings() != null) {
            request.getGlobalSettings().forEach((key, value) -> {
                NotificationGlobalSettings setting = globalSettingsRepository.findBySettingKey(key)
                        .orElseGet(() -> {
                            NotificationGlobalSettings s = new NotificationGlobalSettings();
                            s.setSettingKey(key);
                            return s;
                        });
                setting.setSettingValue(value == null ? "false" : value.toString());
                globalSettingsRepository.save(setting);
            });
        }

        if (request.getEventChannels() != null) {
            for (Map.Entry<String, Map<String, Boolean>> entry : request.getEventChannels().entrySet()) {
                String eventCode = entry.getKey();
                Map<String, Boolean> channels = entry.getValue();
                eventRepository.findByEventCode(eventCode).ifPresent(event -> {
                    for (Map.Entry<String, Boolean> ch : channels.entrySet()) {
                        String channel = ch.getKey();
                        boolean enabled = Boolean.TRUE.equals(ch.getValue());
                        List<NotificationEventChannel> existing = eventChannelRepository.findByEventId(event.getId());
                        Optional<NotificationEventChannel> ex = existing.stream()
                                .filter(c -> channel.equals(c.getChannel()))
                                .findFirst();
                        if (ex.isPresent()) {
                            ex.get().setEnabled(enabled);
                            eventChannelRepository.save(ex.get());
                        } else {
                            NotificationEventChannel nec = new NotificationEventChannel();
                            nec.setEventId(event.getId());
                            nec.setChannel(channel);
                            nec.setEnabled(enabled);
                            eventChannelRepository.save(nec);
                        }
                    }
                });
            }
        }

        auditLogService.log("NOTIFICATION_SETTINGS_UPDATE", "NotificationManagement", null,
                "Updated notification settings", null, null, null);
        return getSettings();
    }

    @Transactional
    public void sendTestNotification(TestNotificationRequest request) {
        String channel = request.getChannel() != null ? request.getChannel().toUpperCase() : "EMAIL";
        String recipient = request.getRecipient();
        String message = request.getMessage() != null ? request.getMessage() : "Test notification from EduManage";

        NotificationHistory history = new NotificationHistory();
        history.setEventCode("TEST_NOTIFICATION");
        history.setChannel(channel);
        history.setRecipientType(channel.equals("SMS") ? "PHONE" : "EMAIL");
        history.setRecipientIdentifier(recipient);
        history.setSubject(channel.equals("SMS") ? null : "Test Notification");
        history.setMessage(message);
        history.setStatus("SENDING");

        if ("SMS".equals(channel)) {
            var res = smsProviderService.sendSms(recipient, message);
            if (Boolean.TRUE.equals(res.get("success"))) {
                history.setStatus("SENT");
                history.setProvider((String) res.get("provider"));
                history.setProviderMessageId((String) res.get("providerMessageId"));
                history.setSentAt(LocalDateTime.now());
            } else {
                history.setStatus("FAILED");
                history.setErrorMessage((String) res.get("error"));
            }
        } else if ("EMAIL".equals(channel)) {
            var res = emailProviderService.sendEmail(recipient, request.getSubject() != null ? request.getSubject() : "Test Notification", message);
            if (Boolean.TRUE.equals(res.get("success"))) {
                history.setStatus("SENT");
                history.setProvider("SMTP");
                history.setProviderMessageId((String) res.get("providerMessageId"));
                history.setSentAt(LocalDateTime.now());
            } else {
                history.setStatus("FAILED");
                history.setErrorMessage((String) res.get("error"));
            }
        } else {
            history.setStatus("SENT");
            history.setSentAt(LocalDateTime.now());
        }

        historyRepository.save(history);
        auditLogService.log("NOTIFICATION_TEST_SEND", "NotificationManagement", history.getId(),
                "Sent test notification", null, null, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationTemplateDto> listTemplates(Pageable pageable) {
        Page<NotificationTemplate> page = templateRepository.findAll(pageable);
        return PageResponse.from(page.map(this::mapTemplate));
    }

    @Transactional
    public NotificationTemplateDto createTemplate(CreateNotificationTemplateRequest request) {
        NotificationTemplate template = new NotificationTemplate();
        template.setName(request.getName());
        template.setChannel(request.getChannel() != null ? request.getChannel().toUpperCase() : "EMAIL");
        template.setSubject(request.getSubject());
        template.setContent(request.getContent());
        template.setVariables(request.getVariables());
        template.setActive(request.isActive());
        templateRepository.save(template);
        auditLogService.log("NOTIFICATION_TEMPLATE_CREATE", "NotificationTemplate", template.getId(),
                "Created template", null, null, null);
        return mapTemplate(template);
    }

    @Transactional
    public NotificationTemplateDto updateTemplate(Long id, CreateNotificationTemplateRequest request) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        template.setName(request.getName());
        template.setChannel(request.getChannel() != null ? request.getChannel().toUpperCase() : template.getChannel());
        template.setSubject(request.getSubject());
        template.setContent(request.getContent());
        template.setVariables(request.getVariables());
        template.setActive(request.isActive());
        templateRepository.save(template);
        auditLogService.log("NOTIFICATION_TEMPLATE_UPDATE", "NotificationTemplate", template.getId(),
                "Updated template", null, null, null);
        return mapTemplate(template);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        templateRepository.delete(template);
        auditLogService.log("NOTIFICATION_TEMPLATE_DELETE", "NotificationTemplate", template.getId(),
                "Deleted template", null, null, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationHistoryDto> listHistory(Pageable pageable) {
        Page<NotificationHistory> page = historyRepository.findAll(pageable);
        return PageResponse.from(page.map(this::mapHistory));
    }

    private Object parseValue(String value) {
        if (value == null) return null;
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return value;
        }
    }

    private NotificationTemplateDto mapTemplate(NotificationTemplate t) {
        return new NotificationTemplateDto(
                t.getId(),
                t.getUuid() != null ? t.getUuid().toString() : null,
                t.getName(),
                t.getChannel(),
                t.getSubject(),
                t.getContent(),
                t.getVariables(),
                t.isActive(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    private NotificationHistoryDto mapHistory(NotificationHistory h) {
        return new NotificationHistoryDto(
                h.getId(),
                h.getUuid() != null ? h.getUuid().toString() : null,
                h.getEventCode(),
                h.getChannel(),
                h.getRecipientType(),
                h.getRecipientIdentifier(),
                h.getSubject(),
                h.getMessage(),
                h.getStatus(),
                h.getProvider(),
                h.getProviderMessageId(),
                h.getErrorMessage(),
                h.getSentAt(),
                h.getCreatedAt()
        );
    }
}

