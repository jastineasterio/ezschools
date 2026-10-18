package com.edumanage.studentprofiles.service;

import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.studentprofiles.dto.CreateStudentProfileRequest;
import com.edumanage.studentprofiles.dto.StudentProfileDto;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public StudentProfileService(StudentProfileRepository studentProfileRepository, UserRepository userRepository,
                                 AuditLogService auditLogService) {
        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<StudentProfileDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<StudentProfile> page;
        if (tenantId != null) {
            page = studentProfileRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = studentProfileRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(profile -> mapToDto(profile, profile.getFirstName() + " " + profile.getLastName())));
    }

    public StudentProfileDto getById(Long id) {
        var profile = studentProfileRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(profile.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(profile, profile.getFirstName() + " " + profile.getLastName());
    }

    @Transactional
    public StudentProfileDto create(CreateStudentProfileRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        var user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean hasStudentRole = user.getRoles().stream()
                .anyMatch(role -> "STUDENT".equals(role.getName()));
        if (!hasStudentRole) {
            throw new ForbiddenException("User does not have STUDENT role");
        }

        if (studentProfileRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new ConflictException("Student profile already exists for this user");
        }

        if (studentProfileRepository.existsBySchoolIdAndAdmissionNumber(tenantId, request.getAdmissionNumber())) {
            throw new ConflictException("Admission number already exists for this school");
        }

        StudentProfile profile = new StudentProfile();
        profile.setUserId(request.getUserId());
        profile.setSchoolId(tenantId);
        profile.setAdmissionNumber(request.getAdmissionNumber());
        profile.setFirstName(request.getFirstName());
        profile.setMiddleName(request.getMiddleName());
        profile.setLastName(request.getLastName());
        profile.setGender(request.getGender());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setRegistrationDate(request.getRegistrationDate() != null ? request.getRegistrationDate() : java.time.LocalDate.now());
        profile.setPhone(request.getPhone());
        profile.setEmail(request.getEmail());
        profile.setAddress(request.getAddress());
        profile.setPhotoUrl(request.getPhotoUrl());
        profile.setStudentType(request.getStudentType() != null ? request.getStudentType() : "DAY");
        profile.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        profile.setEmergencyContactName(request.getEmergencyContactName());
        profile.setEmergencyContactPhone(request.getEmergencyContactPhone());

        studentProfileRepository.save(profile);

        auditLogService.log("STUDENT_CREATE", "StudentProfile", profile.getId(),
                "Created student profile for user " + user.getEmail(), null, null, null);

        return mapToDto(profile, profile.getFirstName() + " " + profile.getLastName());
    }

    @Transactional
    public StudentProfileDto update(Long id, CreateStudentProfileRequest request) {
        var profile = studentProfileRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(profile.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        var user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean hasStudentRole = user.getRoles().stream()
                .anyMatch(role -> "STUDENT".equals(role.getName()));
        if (!hasStudentRole) {
            throw new ForbiddenException("User does not have STUDENT role");
        }

        if (!profile.getAdmissionNumber().equals(request.getAdmissionNumber()) &&
                studentProfileRepository.existsBySchoolIdAndAdmissionNumber(profile.getSchoolId(), request.getAdmissionNumber())) {
            throw new ConflictException("Admission number already exists for this school");
        }

        profile.setUserId(request.getUserId());
        profile.setAdmissionNumber(request.getAdmissionNumber());
        profile.setFirstName(request.getFirstName());
        profile.setMiddleName(request.getMiddleName());
        profile.setLastName(request.getLastName());
        profile.setGender(request.getGender());
        profile.setDateOfBirth(request.getDateOfBirth());
        if (request.getRegistrationDate() != null) {
            profile.setRegistrationDate(request.getRegistrationDate());
        }
        profile.setPhone(request.getPhone());
        profile.setEmail(request.getEmail());
        profile.setAddress(request.getAddress());
        profile.setPhotoUrl(request.getPhotoUrl());
        if (request.getStudentType() != null) {
            profile.setStudentType(request.getStudentType());
        }
        if (request.getStatus() != null) {
            profile.setStatus(request.getStatus());
        }
        profile.setEmergencyContactName(request.getEmergencyContactName());
        profile.setEmergencyContactPhone(request.getEmergencyContactPhone());

        studentProfileRepository.save(profile);

        auditLogService.log("STUDENT_UPDATE", "StudentProfile", profile.getId(),
                "Updated student profile for user " + user.getEmail(), null, null, null);

        return mapToDto(profile, profile.getFirstName() + " " + profile.getLastName());
    }

    @Transactional
    public StudentProfileDto updateStatus(Long id, String status) {
        var profile = studentProfileRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(profile.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        profile.setStatus(status);
        studentProfileRepository.save(profile);

        auditLogService.log("STUDENT_UPDATE_STATUS", "StudentProfile", profile.getId(),
                "Updated student " + profile.getFirstName() + " " + profile.getLastName() + " status to " + status, null, null, null);

        return mapToDto(profile, profile.getFirstName() + " " + profile.getLastName());
    }

    private StudentProfileDto mapToDto(StudentProfile entity, String fullName) {
        StudentProfileDto dto = new StudentProfileDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setUserId(entity.getUserId());
        dto.setSchoolId(entity.getSchoolId());
        dto.setAdmissionNumber(entity.getAdmissionNumber());
        dto.setFirstName(entity.getFirstName());
        dto.setMiddleName(entity.getMiddleName());
        dto.setLastName(entity.getLastName());
        dto.setFullName(fullName);
        dto.setGender(entity.getGender());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setRegistrationDate(entity.getRegistrationDate());
        dto.setPhone(entity.getPhone());
        dto.setEmail(entity.getEmail());
        dto.setAddress(entity.getAddress());
        dto.setPhotoUrl(entity.getPhotoUrl());
        dto.setStudentType(entity.getStudentType());
        dto.setStatus(entity.getStatus());
        dto.setEmergencyContactName(entity.getEmergencyContactName());
        dto.setEmergencyContactPhone(entity.getEmergencyContactPhone());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
