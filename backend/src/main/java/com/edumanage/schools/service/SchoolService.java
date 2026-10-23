package com.edumanage.schools.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.packages.entity.Package;
import com.edumanage.packages.repository.PackageRepository;
import com.edumanage.schools.dto.CreateSchoolRequest;
import com.edumanage.schools.dto.SchoolDto;
import com.edumanage.schools.dto.UpdateSchoolRequest;
import com.edumanage.schools.entity.School;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.subscriptions.entity.Subscription;
import com.edumanage.subscriptions.repository.SubscriptionRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.tenant.TenantContext;
import com.edumanage.users.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.DateTimeException;
import java.time.ZoneId;

@Service
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PackageRepository packageRepository;
    private final AuditLogService auditLogService;

    public SchoolService(SchoolRepository schoolRepository, UserRepository userRepository,
                         SubscriptionRepository subscriptionRepository, PackageRepository packageRepository,
                         AuditLogService auditLogService) {
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.packageRepository = packageRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public SchoolDto createSchool(CreateSchoolRequest request) {
        if (schoolRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new ConflictException("Registration number already exists");
        }
        if (request.getEmail() != null && schoolRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }

        School school = new School();
        school.setName(request.getName());
        school.setRegistrationNumber(request.getRegistrationNumber());
        school.setEmail(request.getEmail());
        school.setPhone(request.getPhone());
        school.setAddress(request.getAddress());
        school.setRegion(request.getRegion());
        school.setDistrict(request.getDistrict());
        school.setActive(true);

        schoolRepository.save(school);

        auditLogService.log("SCHOOL_CREATE", "School", school.getId(),
                "Created school " + school.getName(), null, null, null);

        return mapToDto(school, null, null, null);
    }

    public PageResponse<SchoolDto> listSchools(Pageable pageable) {
        Page<School> page = schoolRepository.findAll(pageable);
        var schoolIds = page.getContent().stream().map(School::getId).toList();
        var userCounts = schoolIds.isEmpty()
                ? java.util.Map.<Long, Long>of()
                : userRepository.countSchoolUsersBySchoolIds(schoolIds).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                UserRepository.SchoolUserCount::getSchoolId,
                                UserRepository.SchoolUserCount::getUserCount));
        return PageResponse.from(page.map(school -> {
            var dto = mapToDto(school, null, null, null);
            return enrichWithSubscriptionInfo(school, dto, userCounts.getOrDefault(school.getId(), 0L));
        }));
    }

    public SchoolDto getSchool(Long id) {
        var school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(school.getId())) {
            throw new ForbiddenException("Access denied");
        }

        return enrichWithSubscriptionInfo(school, mapToDto(school, null, null, null),
                countSchoolUsers(school.getId()));
    }

    @Transactional
    public SchoolDto updateSchool(Long id, UpdateSchoolRequest request) {
        var school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(school.getId())) {
            throw new ForbiddenException("Access denied");
        }

        if (request.getName() != null) school.setName(request.getName());
        if (request.getEmail() != null) school.setEmail(request.getEmail());
        if (request.getPhone() != null) school.setPhone(request.getPhone());
        if (request.getAddress() != null) school.setAddress(request.getAddress());
        if (request.getRegion() != null) school.setRegion(request.getRegion());
        if (request.getDistrict() != null) school.setDistrict(request.getDistrict());
        if (request.isActive() != school.isActive()) school.setActive(request.isActive());

        schoolRepository.save(school);

        auditLogService.log("SCHOOL_UPDATE", "School", school.getId(),
                "Updated school " + school.getName(), null, null, null);

        return enrichWithSubscriptionInfo(school, mapToDto(school, null, null, null),
                countSchoolUsers(school.getId()));
    }

    @Transactional
    public SchoolDto updateTimeZone(String timeZone) {
        Long tenantId = TenantContext.requireTenantId();
        if (timeZone == null || timeZone.isBlank()) {
            throw new BadRequestException("Time zone is required");
        }
        final String validatedTimeZone;
        try {
            validatedTimeZone = ZoneId.of(timeZone).getId();
        } catch (DateTimeException exception) {
            throw new BadRequestException("Time zone must be a valid IANA time zone");
        }
        var school = schoolRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        school.setTimeZone(validatedTimeZone);
        schoolRepository.save(school);
        auditLogService.log("SCHOOL_TIME_ZONE_UPDATE", "School", school.getId(),
                "Updated school time zone", null, null, null);
        return mapToDto(school, null, null, null);
    }

    @Transactional
    public SchoolDto suspendSchool(Long id, String reason) {
        var school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        school.setSuspended(true);
        school.setSuspendedAt(LocalDateTime.now());
        school.setSuspendedReason(reason);
        schoolRepository.save(school);

        auditLogService.log("SCHOOL_SUSPEND", "School", school.getId(),
                "Suspended school " + school.getName() + ": " + reason, null, null, null);

        return enrichWithSubscriptionInfo(school, mapToDto(school, null, null, null),
                countSchoolUsers(school.getId()));
    }

    @Transactional
    public SchoolDto activateSchool(Long id) {
        var school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        school.setSuspended(false);
        school.setSuspendedAt(null);
        school.setSuspendedReason(null);
        schoolRepository.save(school);

        auditLogService.log("SCHOOL_ACTIVATE", "School", school.getId(),
                "Activated school " + school.getName(), null, null, null);

        return enrichWithSubscriptionInfo(school, mapToDto(school, null, null, null),
                countSchoolUsers(school.getId()));
    }

    private SchoolDto enrichWithSubscriptionInfo(School school, SchoolDto dto, long userCount) {
        dto.setUserCount(userCount);
        var subscriptions = subscriptionRepository.findBySchoolId(school.getId());
        var currentSubscription = subscriptions.stream()
                .filter(subscription -> "ACTIVE".equals(subscription.getStatus()))
                .max(java.util.Comparator.comparing(Subscription::getCreatedAt))
                .orElseGet(() -> subscriptions.stream()
                        .max(java.util.Comparator.comparing(Subscription::getCreatedAt))
                        .orElse(null));
        if (currentSubscription != null) {
            var pkg = packageRepository.findById(currentSubscription.getPackageId()).orElse(null);
            dto.setPackageName(pkg != null ? pkg.getDisplayName() : null);
            dto.setSubscriptionStatus(currentSubscription.getStatus());
            dto.setSubscriptionEndDate(currentSubscription.getEndDate());
        }
        return dto;
    }

    private long countSchoolUsers(Long schoolId) {
        return userRepository.countSchoolUsersBySchoolIds(java.util.List.of(schoolId)).stream()
                .mapToLong(UserRepository.SchoolUserCount::getUserCount)
                .sum();
    }

    private SchoolDto mapToDto(School school, String packageName, String subscriptionStatus, LocalDateTime subscriptionEndDate) {
        SchoolDto dto = new SchoolDto();
        dto.setId(school.getId());
        dto.setUuid(school.getUuid() != null ? school.getUuid().toString() : null);
        dto.setName(school.getName());
        dto.setRegistrationNumber(school.getRegistrationNumber());
        dto.setEmail(school.getEmail());
        dto.setPhone(school.getPhone());
        dto.setAddress(school.getAddress());
        dto.setRegion(school.getRegion());
        dto.setDistrict(school.getDistrict());
        dto.setTimeZone(school.getTimeZone());
        dto.setActive(school.isActive());
        dto.setSuspended(school.isSuspended());
        dto.setSuspendedAt(school.getSuspendedAt());
        dto.setSuspendedReason(school.getSuspendedReason());
        dto.setCreatedAt(school.getCreatedAt());
        dto.setPackageName(packageName);
        dto.setSubscriptionStatus(subscriptionStatus);
        dto.setSubscriptionEndDate(subscriptionEndDate);
        return dto;
    }
}
