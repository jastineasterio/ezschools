package com.edumanage.teacherprofiles.service;

import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.teacherprofiles.dto.CreateTeacherProfileRequest;
import com.edumanage.teacherprofiles.dto.TeacherProfileDto;
import com.edumanage.teacherprofiles.entity.TeacherProfile;
import com.edumanage.teacherprofiles.repository.TeacherProfileRepository;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import com.edumanage.security.CustomUserDetails;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherProfileService {

    private final TeacherProfileRepository teacherProfileRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public TeacherProfileService(TeacherProfileRepository teacherProfileRepository, UserRepository userRepository,
                                 AuditLogService auditLogService) {
        this.teacherProfileRepository = teacherProfileRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<TeacherProfileDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<TeacherProfile> page;
        if (tenantId != null) {
            page = teacherProfileRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = teacherProfileRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(profile -> {
            User user = userRepository.findById(profile.getUserId()).orElse(null);
            String firstName = user != null ? user.getFirstName() : null;
            String lastName = user != null ? user.getLastName() : null;
            String email = user != null ? user.getEmail() : null;
            return mapToDto(profile, firstName, lastName, email);
        }));
    }

    public TeacherProfileDto getById(Long id) {
        var profile = teacherProfileRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(profile.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        var userName = fetchUserName(profile.getUserId());
        String firstName = null;
        String lastName = null;
        if (userName != null) {
            var parts = userName.split(" ", 2);
            firstName = parts[0];
            lastName = parts.length > 1 ? parts[1] : null;
        }
        return mapToDto(profile, firstName, lastName, null);
    }

    public TeacherProfileDto getMyProfile() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        var currentUser = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (currentUser == null || !currentUser.isAuthenticated()) {
            throw new ForbiddenException("Not authenticated");
        }

        Long userId = ((CustomUserDetails) currentUser.getPrincipal()).id();
        var profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found"));

        if (!tenantId.equals(profile.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        var userName = fetchUserName(profile.getUserId());
        String firstName = null;
        String lastName = null;
        if (userName != null) {
            var parts = userName.split(" ", 2);
            firstName = parts[0];
            lastName = parts.length > 1 ? parts[1] : null;
        }
        return mapToDto(profile, firstName, lastName, null);
    }

    @Transactional
    public TeacherProfileDto create(CreateTeacherProfileRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        var user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean hasTeacherRole = user.getRoles().stream()
                .anyMatch(role -> "TEACHER".equals(role.getName()));
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

        return mapToDto(profile, user.getFirstName(), user.getLastName(), user.getEmail());
    }

    @Transactional
    public TeacherProfileDto update(Long id, CreateTeacherProfileRequest request) {
        var profile = teacherProfileRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(profile.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        var user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean hasTeacherRole = user.getRoles().stream()
                .anyMatch(role -> "TEACHER".equals(role.getName()));
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

        return mapToDto(profile, user.getFirstName(), user.getLastName(), user.getEmail());
    }

    private String fetchUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse(null);
    }

    private TeacherProfileDto mapToDto(TeacherProfile entity, String firstName, String lastName, String email) {
        TeacherProfileDto dto = new TeacherProfileDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setUserId(entity.getUserId());
        dto.setSchoolId(entity.getSchoolId());
        dto.setEmployeeNumber(entity.getEmployeeNumber());
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setEmail(email);
        dto.setGender(entity.getGender());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setEmploymentStatus(entity.getEmploymentStatus());
        dto.setQualification(entity.getQualification());
        dto.setExperienceYears(entity.getExperienceYears());
        dto.setPhotoUrl(entity.getPhotoUrl());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
