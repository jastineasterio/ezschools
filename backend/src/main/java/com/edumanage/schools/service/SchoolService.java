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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PackageRepository packageRepository;
    private final AuditLogService auditLogService;

    public SchoolService(SchoolRepository schoolRepository, SubscriptionRepository subscriptionRepository,
                         PackageRepository packageRepository, AuditLogService auditLogService) {
        this.schoolRepository = schoolRepository;
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
        return PageResponse.from(page.map(school -> {
            var dto = mapToDto(school, null, null, null);
            return enrichWithSubscriptionInfo(school, dto);
        }));
    }

    public SchoolDto getSchool(Long id) {
        var school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(school.getId())) {
            throw new ForbiddenException("Access denied");
        }

        return enrichWithSubscriptionInfo(school, mapToDto(school, null, null, null));
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

        return enrichWithSubscriptionInfo(school, mapToDto(school, null, null, null));
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

        return enrichWithSubscriptionInfo(school, mapToDto(school, null, null, null));
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

        return enrichWithSubscriptionInfo(school, mapToDto(school, null, null, null));
    }

    private SchoolDto enrichWithSubscriptionInfo(School school, SchoolDto dto) {
        var activeSubscription = subscriptionRepository.findBySchoolIdAndStatus(school.getId(), "ACTIVE").orElse(null);
        if (activeSubscription != null) {
            var pkg = packageRepository.findById(activeSubscription.getPackageId()).orElse(null);
            dto.setPackageName(pkg != null ? pkg.getDisplayName() : null);
            dto.setSubscriptionStatus(activeSubscription.getStatus());
            dto.setSubscriptionEndDate(activeSubscription.getEndDate());
        }
        return dto;
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
