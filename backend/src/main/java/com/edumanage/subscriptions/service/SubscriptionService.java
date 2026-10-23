package com.edumanage.subscriptions.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.packages.repository.PackageRepository;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.subscriptions.dto.AssignPackageRequest;
import com.edumanage.subscriptions.dto.SubscriptionDto;
import com.edumanage.subscriptions.entity.Subscription;
import com.edumanage.subscriptions.repository.SubscriptionRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'SUPER_ADMIN')")
    public SubscriptionDto assignPackage(Long schoolId, AssignPackageRequest request) {
        var school = schoolRepository.findByIdForUpdate(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        var pkg = packageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));
        if (!pkg.isActive()) {
            throw new BadRequestException("Inactive packages cannot be assigned");
        }
        if (request.getBillingPeriod() != null
                && !pkg.getBillingPeriod().equalsIgnoreCase(request.getBillingPeriod().trim())) {
            throw new BadRequestException("Billing period is determined by the selected package");
        }
        if (pkg.getPrice() == null || pkg.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Package price must be a non-negative configured amount");
        }

        LocalDate selectedStartDate = request.getStartDate() == null ? LocalDate.now() : request.getStartDate();
        LocalDate today = LocalDate.now();
        if (!selectedStartDate.equals(today)) {
            throw new BadRequestException("Package assignment must start today; scheduled subscriptions are not supported");
        }
        var startDate = selectedStartDate.atStartOfDay();
        var endDate = calculateEndDate(startDate, pkg.getBillingPeriod());
        var existingSubscriptions = subscriptionRepository.findBySchoolId(schoolId);
        boolean hasUnexpiredPaidSubscription = existingSubscriptions.stream()
                .filter(sub -> "ACTIVE".equals(sub.getStatus()) || "SUSPENDED".equals(sub.getStatus()))
                .anyMatch(sub -> sub.getEndDate() == null || sub.getEndDate().isAfter(startDate));
        if (hasUnexpiredPaidSubscription) {
            throw new ConflictException("An unexpired subscription cannot be replaced before its end date");
        }

        var previousSubscription = existingSubscriptions.stream()
                .max(java.util.Comparator.comparing(Subscription::getStartDate,
                        java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())))
                .orElse(null);
        String previousDetails = previousSubscription == null
                ? "no prior subscription"
                : previousSubscription.getStatus() + " subscription " + previousSubscription.getId()
                        + " for package " + packageName(previousSubscription.getPackageId());
        var subscriptionsToCancel = existingSubscriptions.stream()
                .filter(sub -> "ACTIVE".equals(sub.getStatus())
                        || "TRIAL".equals(sub.getStatus())
                        || "SUSPENDED".equals(sub.getStatus()))
                .toList();
        subscriptionsToCancel.forEach(sub -> {
            sub.setStatus("CANCELLED");
            sub.setCancelledAt(startDate);
            sub.setCancellationReason("Replaced by a newly assigned package");
        });
        subscriptionRepository.saveAll(subscriptionsToCancel);

        Subscription subscription = new Subscription();
        subscription.setSchoolId(schoolId);
        subscription.setPackageId(pkg.getId());
        subscription.setStatus("ACTIVE");
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setBillingPeriod(pkg.getBillingPeriod());
        subscription.setPrice(pkg.getPrice());
        subscription.setPaymentStatus("PENDING");

        var savedSubscription = subscriptionRepository.save(subscription);

        auditLogService.logForSchool("SUBSCRIPTION_ASSIGN", "Subscription", savedSubscription.getId(),
                "School " + school.getName() + ": transitioned from " + previousDetails
                        + " to ACTIVE package " + pkg.getName() + "; payment PENDING; amount "
                        + pkg.getPrice() + " TZS for " + pkg.getBillingPeriod() + ", starts " + selectedStartDate,
                null, null, null, schoolId);

        return mapToDto(savedSubscription);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'SUPER_ADMIN')")
    public SubscriptionDto changePackage(Long subscriptionId, Long newPackageId) {
        var subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        return assignPackage(subscription.getSchoolId(),
                new AssignPackageRequest(newPackageId, null, LocalDate.now()));
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'SUPER_ADMIN')")
    public SubscriptionDto activateSubscription(Long subscriptionId) {
        var subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        schoolRepository.findByIdForUpdate(subscription.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        if (!Set.of("TRIAL", "ACTIVE", "SUSPENDED").contains(subscription.getStatus())) {
            throw new BadRequestException("Only trial or suspended subscriptions can be activated");
        }
        var now = LocalDateTime.now();
        var otherActiveSubscriptions = subscriptionRepository
                .findBySchoolIdAndStatus(subscription.getSchoolId(), "ACTIVE").stream()
                .filter(active -> !active.getId().equals(subscription.getId()))
                .toList();
        if (otherActiveSubscriptions.stream()
                .anyMatch(active -> active.getEndDate() == null || active.getEndDate().isAfter(now))) {
            throw new ConflictException("Another unexpired active subscription already exists for this school");
        }
        if (subscription.getEndDate() != null && !subscription.getEndDate().isAfter(now)) {
            throw new BadRequestException("An expired subscription cannot be activated; assign a package to renew it");
        }
        String previousStatus = subscription.getStatus();
        otherActiveSubscriptions.forEach(active -> {
            active.setStatus("CANCELLED");
            active.setCancelledAt(now);
            active.setCancellationReason("Expired subscription replaced during activation");
        });
        subscriptionRepository.saveAll(otherActiveSubscriptions);
        subscription.setStatus("ACTIVE");
        subscriptionRepository.save(subscription);

        auditLogService.logForSchool("SUBSCRIPTION_ACTIVATE", "Subscription", subscription.getId(),
                "School " + subscription.getSchoolId() + ": changed subscription " + subscription.getId()
                        + " from " + previousStatus + " to ACTIVE; payment remains "
                        + subscription.getPaymentStatus() + "; package " + packageName(subscription.getPackageId()),
                null, null, null, subscription.getSchoolId());

        return mapToDto(subscription);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'SUPER_ADMIN')")
    public SubscriptionDto suspendSubscription(Long subscriptionId) {
        var subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        String previousStatus = subscription.getStatus();
        subscription.setStatus("SUSPENDED");
        subscriptionRepository.save(subscription);

        auditLogService.logForSchool("SUBSCRIPTION_SUSPEND", "Subscription", subscription.getId(),
                "School " + subscription.getSchoolId() + ": changed subscription " + subscription.getId()
                        + " from " + previousStatus + " to SUSPENDED; package " + packageName(subscription.getPackageId()),
                null, null, null, subscription.getSchoolId());

        return mapToDto(subscription);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'SUPER_ADMIN')")
    public SubscriptionDto cancelSubscription(Long subscriptionId, String reason) {
        var subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        String previousStatus = subscription.getStatus();
        subscription.setStatus("CANCELLED");
        subscription.setCancelledAt(LocalDateTime.now());
        subscription.setCancellationReason(reason);
        subscriptionRepository.save(subscription);

        auditLogService.logForSchool("SUBSCRIPTION_CANCEL", "Subscription", subscription.getId(),
                "School " + subscription.getSchoolId() + ": changed subscription " + subscription.getId()
                        + " from " + previousStatus + " to CANCELLED; package "
                        + packageName(subscription.getPackageId()) + "; reason " + reason,
                null, null, null, subscription.getSchoolId());

        return mapToDto(subscription);
    }

    private SubscriptionDto mapToDto(Subscription subscription) {
        SubscriptionDto dto = new SubscriptionDto();
        dto.setId(subscription.getId());
        dto.setUuid(subscription.getUuid() != null ? subscription.getUuid().toString() : null);
        dto.setSchoolId(subscription.getSchoolId());
        schoolRepository.findById(subscription.getSchoolId())
                .ifPresent(school -> dto.setSchoolName(school.getName()));
        dto.setPackageId(subscription.getPackageId());
        packageRepository.findById(subscription.getPackageId())
                .ifPresent(pkg -> dto.setPackageName(pkg.getDisplayName()));
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

    private LocalDateTime calculateEndDate(LocalDateTime startDate, String billingPeriod) {
        if (billingPeriod == null || billingPeriod.isBlank()) {
            throw new BadRequestException("Package billing period is required");
        }

        return switch (billingPeriod.trim().toUpperCase(Locale.ROOT)) {
            case "MONTH", "MONTHLY" -> startDate.plus(1, ChronoUnit.MONTHS);
            case "QUARTER", "QUARTERLY" -> startDate.plus(3, ChronoUnit.MONTHS);
            case "YEAR", "YEARLY", "ANNUAL", "ANNUALLY" -> startDate.plus(1, ChronoUnit.YEARS);
            default -> throw new BadRequestException("Unsupported package billing period: " + billingPeriod);
        };
    }

    private String packageName(Long packageId) {
        return packageRepository.findById(packageId)
                .map(pkg -> pkg.getName())
                .orElse("unknown");
    }
}
