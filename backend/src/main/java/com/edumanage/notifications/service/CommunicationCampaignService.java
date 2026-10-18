package com.edumanage.notifications.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.notifications.dto.CommunicationCampaignDto;
import com.edumanage.notifications.dto.CommunicationDeliveryDto;
import com.edumanage.notifications.dto.CreateCampaignRequest;
import com.edumanage.notifications.dto.RecipientPreviewDto;
import com.edumanage.notifications.entity.CommunicationCampaign;
import com.edumanage.notifications.entity.CommunicationDelivery;
import com.edumanage.notifications.repository.CommunicationCampaignRepository;
import com.edumanage.notifications.repository.CommunicationDeliveryRepository;
import com.edumanage.tenant.TenantContext;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommunicationCampaignService {

    private static final Logger log = LoggerFactory.getLogger(CommunicationCampaignService.class);
    private static final int MAX_RECIPIENTS_PER_CAMPAIGN = 500;
    private static final List<String> CATEGORIES = List.of(
            "GENERAL", "ACADEMIC", "ADMISSIONS", "REGISTRATION", "PARENT_ANNOUNCEMENT",
            "FEES", "SCHEDULE", "EVENT", "EMERGENCY");
    private static final Map<String, String> CHANNELS = Map.of(
            "SMS", "SMS", "EMAIL", "EMAIL", "BOTH", "BOTH");
    private static final List<String> GROUPS = List.of(
            "STUDENTS", "PARENTS", "TEACHERS", "STAFF", "ALL_USERS", "SELECTED");

    private final CommunicationCampaignRepository campaignRepository;
    private final CommunicationDeliveryRepository deliveryRepository;
    private final CommunicationRecipientService recipientService;
    private final CommunicationSettingsService settingsService;
    private final DarsmsProviderService smsProvider;
    private final EmailNotificationProviderService emailProvider;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public CommunicationCampaignService(CommunicationCampaignRepository campaignRepository,
                                        CommunicationDeliveryRepository deliveryRepository,
                                        CommunicationRecipientService recipientService,
                                        CommunicationSettingsService settingsService,
                                        DarsmsProviderService smsProvider,
                                        EmailNotificationProviderService emailProvider,
                                        UserRepository userRepository,
                                        AuditLogService auditLogService) {
        this.campaignRepository = campaignRepository;
        this.deliveryRepository = deliveryRepository;
        this.recipientService = recipientService;
        this.settingsService = settingsService;
        this.smsProvider = smsProvider;
        this.emailProvider = emailProvider;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public RecipientPreviewDto preview(String group, String channel) {
        Scope scope = currentScope();
        String normalizedGroup = normalizeGroup(group);
        String normalizedChannel = normalizeChannel(channel);
        List<CommunicationRecipient> recipients = recipientService.resolve(scope.schoolId(), normalizedGroup, null);
        int eligible = (int) recipients.stream().filter(recipient ->
                eligibleForAnyChannel(recipient, normalizedChannel)).count();
        boolean enabled = channelsFor(normalizedChannel).stream()
                .allMatch(item -> settingsService.isChannelAllowed(scope.userId(), scope.schoolId(), item));
        String reason = enabled ? null : channelsFor(normalizedChannel).stream()
                .map(item -> settingsService.disabledReason(scope.userId(), scope.schoolId(), item))
                .filter(item -> item != null).findFirst().orElse("Selected channel is unavailable");
        return new RecipientPreviewDto(normalizedGroup, normalizedChannel, eligible,
                recipients.size() - eligible, enabled, reason);
    }

    public CommunicationCampaignDto send(CreateCampaignRequest request) {
        Scope scope = currentScope();
        String channel = normalizeChannel(request.getChannel());
        String group = normalizeGroup(request.getRecipientGroup());
        String category = request.getCategory().trim().toUpperCase(Locale.ROOT);
        if (!CATEGORIES.contains(category)) {
            throw new BadRequestException("Unsupported notification category");
        }
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new BadRequestException("Idempotency key is required");
        }
        requireChannelsAllowed(scope, channel);

        var existing = campaignRepository.findBySchoolIdAndSenderUserIdAndIdempotencyKey(
                scope.schoolId(), scope.userId(), request.getIdempotencyKey());
        if (existing.isPresent()) {
            return map(existing.get());
        }

        List<CommunicationRecipient> recipients = recipientService.resolve(
                scope.schoolId(), group, request.getRecipientUserIds());
        if (recipients.isEmpty()) {
            throw new BadRequestException("No eligible recipients were found");
        }

        List<String> channels = channelsFor(channel);
        int excluded = (int) recipients.stream().filter(recipient ->
                channels.stream().noneMatch(item -> recipientService.hasEligibleContact(recipient, item))).count();
        List<CommunicationRecipient> eligibleRecipients = recipients.stream()
                .filter(recipient -> channels.stream()
                        .anyMatch(item -> recipientService.hasEligibleContact(recipient, item)))
                .toList();
        if (eligibleRecipients.isEmpty()) {
            throw new BadRequestException("No recipients have valid contact details for the selected channel");
        }
        if (eligibleRecipients.size() > MAX_RECIPIENTS_PER_CAMPAIGN) {
            throw new BadRequestException("Campaign exceeds the maximum of "
                    + MAX_RECIPIENTS_PER_CAMPAIGN + " recipients");
        }

        CommunicationCampaign campaign = new CommunicationCampaign();
        campaign.setSchoolId(scope.schoolId());
        campaign.setSenderUserId(scope.userId());
        campaign.setIdempotencyKey(request.getIdempotencyKey());
        campaign.setTitle(request.getTitle().trim());
        campaign.setMessage(request.getMessage().trim());
        campaign.setCategory(category);
        campaign.setChannel(channel);
        campaign.setRecipientGroup(group);
        campaign.setIntendedCount(recipients.size());
        campaign.setExcludedCount(excluded);
        campaign.setStatus("PENDING");
        try {
            campaign = campaignRepository.saveAndFlush(campaign);
        } catch (DataIntegrityViolationException e) {
            return campaignRepository.findBySchoolIdAndSenderUserIdAndIdempotencyKey(
                            scope.schoolId(), scope.userId(), request.getIdempotencyKey())
                    .map(this::map)
                    .orElseThrow(() -> e);
        }

        List<CommunicationDelivery> deliveries = new ArrayList<>();
        for (CommunicationRecipient recipient : eligibleRecipients) {
            for (String deliveryChannel : channels) {
                if (!recipientService.hasEligibleContact(recipient, deliveryChannel)) {
                    continue;
                }
                CommunicationDelivery delivery = new CommunicationDelivery();
                delivery.setCampaignId(campaign.getId());
                delivery.setRecipientType(recipient.type());
                delivery.setRecipientId(recipient.id());
                delivery.setChannel(deliveryChannel);
                delivery.setStatus("PENDING");
                deliveries.add(delivery);
            }
        }
        deliveryRepository.saveAllAndFlush(deliveries);

        Map<String, CommunicationRecipient> recipientLookup = eligibleRecipients.stream()
                .collect(Collectors.toMap(targetKey(), Function.identity()));
        CommunicationCampaign dispatchCampaign = campaign;
        deliveries.forEach(delivery -> dispatch(dispatchCampaign, delivery, recipientLookup));
        refreshCampaignStatus(campaign);
        auditLogService.log("COMMUNICATION_CAMPAIGN_SEND", "CommunicationCampaign", campaign.getId(),
                "Created school notification campaign; intended recipients=" + campaign.getIntendedCount()
                        + ", excluded=" + excluded, null, null, null);
        return map(campaignRepository.findById(campaign.getId()).orElseThrow());
    }

    public PageResponse<CommunicationCampaignDto> list(Pageable pageable) {
        Scope scope = currentScope();
        Page<CommunicationCampaign> page = campaignRepository.findBySchoolIdOrderByCreatedAtDesc(
                scope.schoolId(), pageable);
        return PageResponse.from(page.map(this::map));
    }

    public PageResponse<CommunicationCampaignDto> listAllForPlatform(Pageable pageable) {
        return PageResponse.from(campaignRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::map));
    }

    public List<CommunicationDeliveryDto> getDeliveries(Long campaignId) {
        Scope scope = currentScope();
        CommunicationCampaign campaign = campaignRepository.findBySchoolIdAndId(scope.schoolId(), campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
        return deliveryRepository.findByCampaignId(campaign.getId()).stream().map(this::mapDelivery).toList();
    }

    public List<CommunicationDeliveryDto> getDeliveriesForPlatform(Long campaignId) {
        campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
        return deliveryRepository.findByCampaignId(campaignId).stream().map(this::mapDelivery).toList();
    }

    public CommunicationCampaignDto retryFailed(Long campaignId) {
        Scope scope = currentScope();
        CommunicationCampaign campaign = campaignRepository.findBySchoolIdAndId(scope.schoolId(), campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
        requireChannelsAllowed(scope, campaign.getChannel());

        Map<String, CommunicationRecipient> recipients = recipientService.resolve(
                        campaign.getSchoolId(), campaign.getRecipientGroup(), null).stream()
                .collect(Collectors.toMap(targetKey(), Function.identity()));
        List<CommunicationDelivery> failed = deliveryRepository.findByCampaignIdAndStatus(campaignId, "FAILED");
        for (CommunicationDelivery delivery : failed) {
            if (deliveryRepository.markFailedDeliveryForRetry(delivery.getId()) != 1) {
                continue;
            }
            delivery.setStatus("RETRYING");
            CommunicationRecipient recipient = recipients.get(targetKey(delivery.getRecipientType(), delivery.getRecipientId()));
            if (recipient == null || !recipientService.hasEligibleContact(recipient, delivery.getChannel())) {
                delivery.setStatus("FAILED");
                delivery.setFailureCode("RECIPIENT_NO_LONGER_AVAILABLE");
                delivery.setAttemptCount(delivery.getAttemptCount() + 1);
                delivery.setLastAttemptAt(LocalDateTime.now());
                deliveryRepository.save(delivery);
                continue;
            }
            dispatch(campaign, delivery, recipients);
        }
        refreshCampaignStatus(campaign);
        auditLogService.log("COMMUNICATION_CAMPAIGN_RETRY", "CommunicationCampaign", campaignId,
                "Retried eligible failed notification deliveries", null, null, null);
        return map(campaignRepository.findById(campaignId).orElseThrow());
    }

    private void dispatch(CommunicationCampaign campaign, CommunicationDelivery delivery,
                          Map<String, CommunicationRecipient> recipients) {
        CommunicationRecipient recipient = recipients.get(targetKey(
                delivery.getRecipientType(), delivery.getRecipientId()));
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setLastAttemptAt(LocalDateTime.now());
        try {
            if (recipient == null) {
                throw new NotificationProviderException("RECIPIENT_NO_LONGER_AVAILABLE");
            }
            String providerMessageId = null;
            if ("SMS".equals(delivery.getChannel())) {
                providerMessageId = smsProvider.send(recipient.phone(), campaign.getMessage());
            } else {
                emailProvider.send(recipient.email(), campaign.getTitle(), campaign.getMessage());
            }
            delivery.setProviderMessageId(providerMessageId);
            delivery.setStatus("ACCEPTED");
            delivery.setFailureCode(null);
        } catch (NotificationProviderException e) {
            delivery.setStatus("FAILED");
            delivery.setFailureCode(e.getFailureCode());
            log.warn("Notification delivery failed; campaignId={}, deliveryId={}, code={}",
                    campaign.getId(), delivery.getId(), e.getFailureCode());
        }
        deliveryRepository.save(delivery);
    }

    private void refreshCampaignStatus(CommunicationCampaign campaign) {
        List<CommunicationDelivery> deliveries = deliveryRepository.findByCampaignId(campaign.getId());
        long accepted = deliveries.stream().filter(item -> "ACCEPTED".equals(item.getStatus())).count();
        long failed = deliveries.stream().filter(item -> "FAILED".equals(item.getStatus())).count();
        campaign.setAcceptedCount((int) accepted);
        campaign.setFailedCount((int) failed);
        if (accepted == deliveries.size()) {
            campaign.setStatus("ACCEPTED");
        } else if (accepted > 0 || deliveries.stream().anyMatch(item -> "PENDING".equals(item.getStatus())
                || "RETRYING".equals(item.getStatus()))) {
            campaign.setStatus("PARTIAL");
        } else {
            campaign.setStatus("FAILED");
        }
        campaignRepository.save(campaign);
    }

    private boolean eligibleForAnyChannel(CommunicationRecipient recipient, String channel) {
        return channelsFor(channel).stream().anyMatch(item -> recipientService.hasEligibleContact(recipient, item));
    }

    private void requireChannelsAllowed(Scope scope, String channel) {
        for (String item : channelsFor(channel)) {
            if (!settingsService.isChannelAllowed(scope.userId(), scope.schoolId(), item)) {
                String reason = settingsService.disabledReason(scope.userId(), scope.schoolId(), item);
                throw new ForbiddenException(reason == null ? "Selected channel is unavailable" : reason);
            }
        }
    }

    private List<String> channelsFor(String channel) {
        return switch (channel) {
            case "SMS", "EMAIL" -> List.of(channel);
            case "BOTH" -> List.of("SMS", "EMAIL");
            default -> throw new BadRequestException("Unsupported notification channel");
        };
    }

    private String normalizeChannel(String channel) {
        if (channel == null || !CHANNELS.containsKey(channel.trim().toUpperCase(Locale.ROOT))) {
            throw new BadRequestException("Unsupported notification channel");
        }
        return channel.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeGroup(String group) {
        if (group == null || !GROUPS.contains(group.trim().toUpperCase(Locale.ROOT))) {
            throw new BadRequestException("Unsupported recipient group");
        }
        return group.trim().toUpperCase(Locale.ROOT);
    }

    private Scope currentScope() {
        Long schoolId = TenantContext.getTenantId();
        if (schoolId == null) {
            throw new ForbiddenException("School context required");
        }
        User user = userRepository.findByEmail(CommunicationRecipientService.currentUserEmail())
                .filter(current -> current.isActive() && !current.isDeleted())
                .orElseThrow(() -> new ForbiddenException("User not authenticated"));
        if (!schoolId.equals(user.getSchoolId())) {
            throw new ForbiddenException("Access denied for this school");
        }
        return new Scope(user.getId(), schoolId);
    }

    private CommunicationCampaignDto map(CommunicationCampaign campaign) {
        return new CommunicationCampaignDto(campaign.getId(), campaign.getSchoolId(), campaign.getSenderUserId(),
                campaign.getTitle(), campaign.getCategory(), campaign.getChannel(), campaign.getRecipientGroup(),
                campaign.getIntendedCount(), campaign.getExcludedCount(), campaign.getAcceptedCount(),
                campaign.getFailedCount(), campaign.getStatus(), campaign.getCreatedAt());
    }

    private CommunicationDeliveryDto mapDelivery(CommunicationDelivery delivery) {
        return new CommunicationDeliveryDto(delivery.getId(), delivery.getRecipientType(), delivery.getRecipientId(),
                delivery.getChannel(), delivery.getStatus(), delivery.getProviderMessageId(),
                delivery.getFailureCode(), delivery.getAttemptCount(), delivery.getLastAttemptAt());
    }

    private Function<CommunicationRecipient, String> targetKey() {
        return recipient -> targetKey(recipient.type(), recipient.id());
    }

    private String targetKey(String type, Long id) {
        return type + ":" + id;
    }

    private record Scope(Long userId, Long schoolId) {
    }
}
