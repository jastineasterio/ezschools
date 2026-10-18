package com.edumanage.subscriptions.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.packages.entity.Package;
import com.edumanage.packages.repository.PackageRepository;
import com.edumanage.schools.entity.School;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.subscriptions.dto.AssignPackageRequest;
import com.edumanage.subscriptions.dto.SubscriptionDto;
import com.edumanage.subscriptions.entity.Subscription;
import com.edumanage.subscriptions.repository.SubscriptionRepository;
import com.edumanage.audit.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PackageRepository packageRepository;
    private final SchoolRepository schoolRepository;
    private final AuditLogService auditLogService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, PackageRepository packageRepository,
                               SchoolRepository schoolRepository, AuditLogService auditLogService) {
        this.subscriptionRepository = subscriptionRepository;
        this.packageRepository = packageRepository;
        this.schoolRepository = schoolRepository;
        this.auditLogService = auditLogService;
    }

    public SubscriptionDto getActiveSubscription(Long schoolId) {
        var subscription = subscriptionRepository.findBySchoolId(schoolId).stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription found"));
        return mapToDto(subscription);
    }

    public List<SubscriptionDto> listAllSubscriptions() {
        return subscriptionRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public SubscriptionDto getSubscription(Long subscriptionId) {
        var subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        return mapToDto(subscription);
    }

    @Transactional
    public SubscriptionDto assignPackage(Long schoolId, AssignPackageRequest request) {
        var pkg = packageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        var school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        var existingActive = subscriptionRepository.findBySchoolIdAndStatus(schoolId, "ACTIVE");
        existingActive.ifPresent(sub -> {
            sub.setStatus("CANCELLED");
            sub.setCancelledAt(LocalDateTime.now());
            subscriptionRepository.save(sub);
        });

        Subscription subscription = new Subscription();
        subscription.setSchoolId(schoolId);
        subscription.setPackageId(request.getPackageId());
        subscription.setStatus("TRIAL");
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(LocalDateTime.now().plusMonths(1));
        subscription.setBillingPeriod(request.getBillingPeriod());
        subscription.setPrice(pkg.getPrice());
        subscription.setPaymentStatus("PENDING");
        subscription.setTrialEndsAt(LocalDateTime.now().plusDays(14));

        subscriptionRepository.save(subscription);

        auditLogService.log("SUBSCRIPTION_ASSIGN", "Subscription", subscription.getId(),
                "Assigned package " + pkg.getName() + " to school " + school.getName(), null, null, null);

        return mapToDto(subscription);
    }

    @Transactional
    public SubscriptionDto changePackage(Long subscriptionId, Long newPackageId) {
        var subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        var newPkg = packageRepository.findById(newPackageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        subscription.setPackageId(newPackageId);
        subscription.setPrice(newPkg.getPrice());
        subscription.setEndDate(LocalDateTime.now().plusMonths(1));
        subscriptionRepository.save(subscription);

        auditLogService.log("SUBSCRIPTION_CHANGE", "Subscription", subscription.getId(),
                "Changed subscription package to " + newPkg.getName(), null, null, null);

        return mapToDto(subscription);
    }

    @Transactional
    public SubscriptionDto activateSubscription(Long subscriptionId) {
        var subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        subscription.setStatus("ACTIVE");
        subscription.setPaymentStatus("PAID");
        subscriptionRepository.save(subscription);

        auditLogService.log("SUBSCRIPTION_ACTIVATE", "Subscription", subscription.getId(),
                "Activated subscription", null, null, null);

        return mapToDto(subscription);
    }

    @Transactional
    public SubscriptionDto suspendSubscription(Long subscriptionId) {
        var subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        subscription.setStatus("SUSPENDED");
        subscriptionRepository.save(subscription);

        auditLogService.log("SUBSCRIPTION_SUSPEND", "Subscription", subscription.getId(),
                "Suspended subscription", null, null, null);

        return mapToDto(subscription);
    }

    @Transactional
    public SubscriptionDto cancelSubscription(Long subscriptionId, String reason) {
        var subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        subscription.setStatus("CANCELLED");
        subscription.setCancelledAt(LocalDateTime.now());
        subscription.setCancellationReason(reason);
        subscriptionRepository.save(subscription);

        auditLogService.log("SUBSCRIPTION_CANCEL", "Subscription", subscription.getId(),
                "Cancelled subscription: " + reason, null, null, null);

        return mapToDto(subscription);
    }

    private SubscriptionDto mapToDto(Subscription subscription) {
        SubscriptionDto dto = new SubscriptionDto();
        dto.setId(subscription.getId());
        dto.setUuid(subscription.getUuid() != null ? subscription.getUuid().toString() : null);
        dto.setSchoolId(subscription.getSchoolId());
        dto.setPackageId(subscription.getPackageId());
        dto.setStatus(subscription.getStatus());
        dto.setStartDate(subscription.getStartDate());
        dto.setEndDate(subscription.getEndDate());
        dto.setBillingPeriod(subscription.getBillingPeriod());
        dto.setPrice(subscription.getPrice());
        dto.setPaymentStatus(subscription.getPaymentStatus());
        dto.setTrialEndsAt(subscription.getTrialEndsAt());
        dto.setCancelledAt(subscription.getCancelledAt());
        dto.setCancellationReason(subscription.getCancellationReason());
        dto.setCreatedAt(subscription.getCreatedAt());
        return dto;
    }
}
