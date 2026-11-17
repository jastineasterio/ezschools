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
import com.edumanage.sms.service.SmsConfigurationService;
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
    private static final int MAX_RETRY_ATTEMPTS = 3;

    /**
     * Provider failures where the outcome of the submission is unknown (for example a
     * network timeout). These must not be recorded as FAILED because the provider may
     * already have accepted the message. They are recorded as PENDING and are never
     * retried automatically, to avoid duplicate SMS.
     */
    private static final List<String> UNCERTAIN_FAILURE_CODES = List.of(
            "SMS_PROVIDER_UNAVAILABLE",
            "SMS_PROVIDER_CONNECTION_FAILED",
            "SMS_PROVIDER_CONNECT_TIMEOUT",
            "SMS_PROVIDER_READ_TIMEOUT",
            "SMS_PROVIDER_TLS_FAILURE",
            "SMS_PROVIDER_SERVER_ERROR",
            "SMS_PROVIDER_RATE_LIMITED"
    );

    /**
     * Provider failures that are permanent and must not be retried indefinitely.
     */
    private static final List<String> PERMANENT_FAILURE_CODES = List.of(
            "SMS_NOT_CONFIGURED",
            "SMS_PROVIDER_AUTH_FAILED",
            "SMS_PROVIDER_ACCOUNT_DISABLED",
            "SMS_PROVIDER_REJECTED_RECIPIENT",
            "SMS_PROVIDER_REJECTED",
            "SMS_PROVIDER_INVALID_RESPONSE",
            "INVALID_PHONE",
            "RECIPIENT_NO_LONGER_AVAILABLE"
    );

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
    private final SmsConfigurationService smsConfigurationService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public CommunicationCampaignService(CommunicationCampaignRepository campaignRepository,
                                        CommunicationDeliveryRepository deliveryRepository,
                                        CommunicationRecipientService recipientService,
                                        CommunicationSettingsService settingsService,
                                        DarsmsProviderService smsProvider,
                                        EmailNotificationProviderService emailProvider,
                                        SmsConfigurationService smsConfigurationService,
                                        UserRepository userRepository,
                                        AuditLogService auditLogService) {
        this.campaignRepository = campaignRepository;
        this.deliveryRepository = deliveryRepository;
        this.recipientService = recipientService;
        this.settingsService = settingsService;
        this.smsProvider = smsProvider;
        this.emailProvider = emailProvider;
        this.smsConfigurationService = smsConfigurationService;
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
        if (!isValidCategory(category)) {
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
        for (CommunicationRecipient recipient : recipients) {
            boolean eligible = channels.stream()
                    .anyMatch(item -> recipientService.hasEligibleContact(recipient, item));
            if (!eligible) {
                CommunicationDelivery excludedDelivery = new CommunicationDelivery();
                excludedDelivery.setCampaignId(campaign.getId());
                excludedDelivery.setRecipientType(recipient.type());
                excludedDelivery.setRecipientId(recipient.id());
                excludedDelivery.setChannel(channels.get(0));
                excludedDelivery.setStatus("EXCLUDED");
                excludedDelivery.setFailureCode("INVALID_PHONE");
                excludedDelivery.setAttemptCount(0);
                excludedDelivery.setLastAttemptAt(LocalDateTime.now());
                excludedDelivery.setRecipientName(recipient.displayName());
                excludedDelivery.setRecipientPhone(recipient.phone());
                deliveries.add(excludedDelivery);
                continue;
            }
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
                delivery.setAttemptCount(0);
                delivery.setLastAttemptAt(LocalDateTime.now());
                delivery.setRecipientName(recipient.displayName());
                delivery.setRecipientPhone(recipient.phone());
                deliveries.add(delivery);
            }
        }
        deliveryRepository.saveAllAndFlush(deliveries);

        final CommunicationCampaign savedCampaign = campaign;
        Map<String, CommunicationRecipient> recipientLookup = eligibleRecipients.stream()
                .collect(Collectors.toMap(targetKey(), Function.identity()));
        SmsConfigurationService.SmsCredentials smsCreds = List.of("SMS", "BOTH").contains(channel)
                ? smsConfigurationService.getCredentials(scope.schoolId())
                : null;
        deliveries.stream()
                .filter(delivery -> "PENDING".equals(delivery.getStatus()))
                .forEach(delivery -> dispatch(savedCampaign, delivery, recipientLookup, smsCreds));
        refreshCampaignStatus(savedCampaign);
        auditLogService.log("COMMUNICATION_CAMPAIGN_SEND", "CommunicationCampaign", savedCampaign.getId(),
                "Created school notification campaign; intended recipients=" + savedCampaign.getIntendedCount()
                        + ", excluded=" + excluded, null, null, null);
        return map(campaignRepository.findById(savedCampaign.getId()).orElseThrow());
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
        // Only genuinely FAILED deliveries are retried. SENT, DELIVERED, EXCLUDED and
        // PENDING (uncertain outcome) deliveries are never retried, avoiding duplicate SMS.
        List<CommunicationDelivery> failed = deliveryRepository.findByCampaignIdAndStatus(campaignId, "FAILED");
        SmsConfigurationService.SmsCredentials smsCreds = List.of("SMS", "BOTH").contains(campaign.getChannel())
                ? smsConfigurationService.getCredentials(scope.schoolId())
                : null;
        for (CommunicationDelivery delivery : failed) {
            if (delivery.getAttemptCount() >= MAX_RETRY_ATTEMPTS) {
                continue;
            }
            if (PERMANENT_FAILURE_CODES.contains(delivery.getFailureCode())) {
                continue;
            }
            if (deliveryRepository.markFailedDeliveryForRetry(delivery.getId()) != 1) {
                continue;
            }
            CommunicationRecipient recipient = recipients.get(targetKey(delivery.getRecipientType(), delivery.getRecipientId()));
            if (recipient == null || !recipientService.hasEligibleContact(recipient, delivery.getChannel())) {
                delivery.setStatus("FAILED");
                delivery.setFailureCode("RECIPIENT_NO_LONGER_AVAILABLE");
                delivery.setErrorMessage("Recipient is no longer available");
                delivery.setAttemptCount(delivery.getAttemptCount() + 1);
                delivery.setLastAttemptAt(LocalDateTime.now());
                deliveryRepository.save(delivery);
                continue;
            }
            dispatch(campaign, delivery, recipients, smsCreds);
        }
        refreshCampaignStatus(campaign);
        auditLogService.log("COMMUNICATION_CAMPAIGN_RETRY", "CommunicationCampaign", campaignId,
                "Retried eligible failed notification deliveries", null, null, null);
        return map(campaignRepository.findById(campaignId).orElseThrow());
    }

    private void dispatch(CommunicationCampaign campaign, CommunicationDelivery delivery,
                          Map<String, CommunicationRecipient> recipients,
                          SmsConfigurationService.SmsCredentials smsCreds) {
        CommunicationRecipient recipient = recipients.get(targetKey(
                delivery.getRecipientType(), delivery.getRecipientId()));
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setLastAttemptAt(LocalDateTime.now());
        try {
            if (recipient == null) {
                throw new NotificationProviderException("RECIPIENT_NO_LONGER_AVAILABLE");
            }
            String providerMessageId = null;
            String batchId = null;
            if ("SMS".equals(delivery.getChannel())) {
                DarsmsProviderService.SendResult result =
                        smsProvider.send(recipient.phone(), campaign.getMessage(),
                                smsCreds.apiKey(), smsCreds.senderId());
                providerMessageId = result.messageId();
                batchId = result.batchId();
            } else {
                emailProvider.send(recipient.email(), campaign.getTitle(), campaign.getMessage());
            }
            delivery.setProviderMessageId(providerMessageId);
            delivery.setBatchId(batchId);
            delivery.setStatus("SENT");
            delivery.setFailureCode(null);
            delivery.setErrorMessage(null);
            delivery.setSentAt(LocalDateTime.now());
            log.info("SMS accepted by provider; campaignId={}, deliveryId={}, messageId={}",
                    campaign.getId(), delivery.getId(), providerMessageId);
        } catch (NotificationProviderException e) {
            String code = e.getFailureCode();
            if (isUncertainFailure(code)) {
                // Outcome unknown: the provider may already have accepted the message.
                // Record PENDING, never FAILED, to avoid duplicate SMS on retry.
                delivery.setStatus("PENDING");
                delivery.setFailureCode(code);
                delivery.setErrorMessage(e.getMessage());
                log.warn("Notification delivery outcome uncertain; campaignId={}, deliveryId={}, code={}",
                        campaign.getId(), delivery.getId(), code);
            } else {
                delivery.setStatus("FAILED");
                delivery.setFailureCode(code);
                delivery.setErrorMessage(e.getMessage());
                log.warn("Notification delivery failed; campaignId={}, deliveryId={}, code={}",
                        campaign.getId(), delivery.getId(), code);
            }
        }
        deliveryRepository.save(delivery);
    }

    private void refreshCampaignStatus(CommunicationCampaign campaign) {
        List<CommunicationDelivery> deliveries = deliveryRepository.findByCampaignId(campaign.getId());
        long sent = deliveries.stream()
                .filter(item -> "SENT".equals(item.getStatus()) || "DELIVERED".equals(item.getStatus())).count();
        long delivered = deliveries.stream()
                .filter(item -> "DELIVERED".equals(item.getStatus())).count();
        long failed = deliveries.stream()
                .filter(item -> "FAILED".equals(item.getStatus())).count();
        long pending = deliveries.stream()
                .filter(item -> "PENDING".equals(item.getStatus()) || "RETRYING".equals(item.getStatus())).count();
        long eligible = deliveries.stream()
                .filter(item -> !"EXCLUDED".equals(item.getStatus())).count();

        campaign.setAcceptedCount((int) sent);
        campaign.setFailedCount((int) failed);
        campaign.setDeliveredCount((int) delivered);

        String status;
        if (eligible == 0) {
            status = "NO RECIPIENTS";
        } else if (pending > 0) {
            status = "PENDING";
        } else if (failed == eligible) {
            status = "FAILED";
        } else if (sent == eligible) {
            status = "SENT";
        } else {
            status = "PARTIALLY SENT";
        }
        campaign.setStatus(status);
        campaignRepository.save(campaign);
    }

    private boolean isValidCategory(String category) {
        return switch (category) {
            case "GENERAL", "ACADEMIC", "ADMISSIONS", "REGISTRATION", "PARENT_ANNOUNCEMENT",
                 "FEES", "SCHEDULE", "EVENT", "EMERGENCY" -> true;
            default -> false;
        };
    }

    private boolean eligibleForAnyChannel(CommunicationRecipient recipient, String channel) {
        return channelsFor(channel).stream().anyMatch(item -> recipientService.hasEligibleContact(recipient, item));
    }

    private boolean isUncertainFailure(String code) {
        return code != null && UNCERTAIN_FAILURE_CODES.contains(code);
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
                campaign.getFailedCount(), campaign.getDeliveredCount(), campaign.getStatus(), campaign.getCreatedAt());
    }

    private CommunicationDeliveryDto mapDelivery(CommunicationDelivery delivery) {
        return new CommunicationDeliveryDto(delivery.getId(), delivery.getRecipientType(), delivery.getRecipientId(),
                delivery.getChannel(), delivery.getStatus(), delivery.getProviderMessageId(),
                delivery.getFailureCode(), delivery.getErrorMessage(), delivery.getDeliveryStatus(),
                delivery.getSentAt(), delivery.getDeliveredAt(), delivery.getBatchId(),
                delivery.getAttemptCount(), delivery.getLastAttemptAt(),
                delivery.getRecipientName(), delivery.getRecipientPhone());
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
