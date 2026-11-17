package com.edumanage.teacherprofiles.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.exception.UnauthorizedException;
import com.edumanage.schools.entity.School;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.teacherprofiles.dto.TeacherProfileDto;
import com.edumanage.teacherprofiles.dto.UpdateTeacherProfileRequest;
import com.edumanage.teacherprofiles.entity.TeacherProfile;
import com.edumanage.teacherprofiles.repository.TeacherProfileRepository;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import com.edumanage.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class TeacherProfileService {

    private final TeacherProfileRepository teacherProfileRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final AuditLogService auditLogService;

    public TeacherProfileService(TeacherProfileRepository teacherProfileRepository,
                                 UserRepository userRepository,
                                 SchoolRepository schoolRepository,
                                 AuditLogService auditLogService) {
        this.teacherProfileRepository = teacherProfileRepository;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<TeacherProfileDto> list(Pageable pageable) {
        Long tenantId = com.edumanage.tenant.TenantContext.getTenantId();
        Page<TeacherProfile> page;
        if (tenantId != null) {
            page = teacherProfileRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = teacherProfileRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(profile -> {
            User user = userRepository.findById(profile.getUserId()).orElse(null);
            School school = schoolRepository.findById(profile.getSchoolId()).orElse(null);
            return buildDto(profile, user, school);
        }));
    }

    public TeacherProfileDto getById(Long id) {
        var profile = teacherProfileRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found"));

        Long tenantId = com.edumanage.tenant.TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(profile.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        User user = userRepository.findById(profile.getUserId()).orElse(null);
        School school = schoolRepository.findById(profile.getSchoolId()).orElse(null);
        return buildDto(profile, user, school);
    }

    public TeacherProfileDto getMyProfile() {
        Long tenantId = com.edumanage.tenant.TenantContext.requireTenantId();

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }

        Long userId = ((CustomUserDetails) authentication.getPrincipal()).id();
        var profile = teacherProfileRepository.findByUserId(userId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found"));

        if (!tenantId.equals(profile.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        var user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var school = schoolRepository.findById(profile.getSchoolId()).orElse(null);

        return buildDto(profile, user, school);
    }

    @Transactional
    public TeacherProfileDto updateMyProfile(UpdateTeacherProfileRequest request) {
        Long tenantId = com.edumanage.tenant.TenantContext.requireTenantId();

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }

        Long userId = ((CustomUserDetails) authentication.getPrincipal()).id();

        var profile = teacherProfileRepository.findByUserId(userId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found"));

        if (!tenantId.equals(profile.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        var user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
                throw new ConflictException("Email already registered");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        userRepository.save(user);

        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getPhotoUrl() != null) {
            profile.setPhotoUrl(request.getPhotoUrl());
        }

        teacherProfileRepository.save(profile);

        auditLogService.log("TEACHER_PROFILE_UPDATE", "TeacherProfile", profile.getId(),
                "Updated teacher profile for user " + user.getEmail(), null, null, null);

        var school = schoolRepository.findById(profile.getSchoolId()).orElse(null);
        return buildDto(profile, user, school);
    }

    @Transactional
    public TeacherProfileDto create(com.edumanage.teacherprofiles.dto.CreateTeacherProfileRequest request) {
        Long tenantId = com.edumanage.tenant.TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        var user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean hasTeacherRole = user.getRoles().stream()
                .map(role -> role.getName() == null ? "" : role.getName().trim().toUpperCase())
                .anyMatch("TEACHER"::equals);
        if (!hasTeacherRole) {
            throw new ForbiddenException("User does not have TEACHER role");
        }

        if (teacherProfileRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new ConflictException("Teacher profile already exists for this user");
        }

        TeacherProfile profile = new TeacherProfile();
        profile.setUserId(request.getUserId());
        profile.setSchoolId(tenantId);
        profile.setEmployeeNumber(request.getEmployeeNumber());
        profile.setGender(request.getGender());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setEmploymentStatus(request.getEmploymentStatus() != null ? request.getEmploymentStatus() : "ACTIVE");
        profile.setQualification(request.getQualification());
        profile.setExperienceYears(request.getExperienceYears());
        profile.setPhotoUrl(request.getPhotoUrl());

        teacherProfileRepository.save(profile);

        auditLogService.log("TEACHER_CREATE", "TeacherProfile", profile.getId(),
                "Created teacher profile for user " + user.getEmail(), null, null, null);

        var school = schoolRepository.findById(profile.getSchoolId()).orElse(null);
        return buildDto(profile, user, school);
    }

    @Transactional
    public TeacherProfileDto update(Long id, com.edumanage.teacherprofiles.dto.CreateTeacherProfileRequest request) {
        var profile = teacherProfileRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found"));

        Long tenantId = com.edumanage.tenant.TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(profile.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        var user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean hasTeacherRole = user.getRoles().stream()
                .map(role -> role.getName() == null ? "" : role.getName().trim().toUpperCase())
                .anyMatch("TEACHER"::equals);
        if (!hasTeacherRole) {
            throw new ForbiddenException("User does not have TEACHER role");
        }

        profile.setUserId(request.getUserId());
        profile.setEmployeeNumber(request.getEmployeeNumber());
        profile.setGender(request.getGender());
        profile.setDateOfBirth(request.getDateOfBirth());
        if (request.getEmploymentStatus() != null) {
            profile.setEmploymentStatus(request.getEmploymentStatus());
        }
        profile.setQualification(request.getQualification());
        profile.setExperienceYears(request.getExperienceYears());
        profile.setPhotoUrl(request.getPhotoUrl());

        teacherProfileRepository.save(profile);

        auditLogService.log("TEACHER_UPDATE", "TeacherProfile", profile.getId(),
                "Updated teacher profile for user " + user.getEmail(), null, null, null);

        var school = schoolRepository.findById(profile.getSchoolId()).orElse(null);
        return buildDto(profile, user, school);
    }

    private TeacherProfileDto buildDto(TeacherProfile entity, User user, School school) {
        TeacherProfileDto dto = new TeacherProfileDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setUserId(entity.getUserId());
        dto.setSchoolId(entity.getSchoolId());
        dto.setSchoolName(school != null ? school.getName() : null);
        dto.setSchoolCode(school != null ? school.getRegistrationNumber() : null);
        dto.setRole("TEACHER");
        dto.setEmployeeNumber(entity.getEmployeeNumber());
        dto.setFirstName(user != null ? user.getFirstName() : null);
        dto.setLastName(user != null ? user.getLastName() : null);
        dto.setEmail(user != null ? user.getEmail() : null);
        dto.setPhone(user != null ? user.getPhone() : null);
        dto.setGender(entity.getGender());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setEmploymentStatus(entity.getEmploymentStatus());
        dto.setActive(user != null && user.isActive());
        dto.setLastLogin(user != null ? user.getLastLogin() : null);
        dto.setQualification(entity.getQualification());
        dto.setExperienceYears(entity.getExperienceYears());
        dto.setPhotoUrl(entity.getPhotoUrl());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
