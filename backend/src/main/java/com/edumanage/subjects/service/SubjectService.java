package com.edumanage.subjects.service;

import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.subjects.dto.CreateSubjectRequest;
import com.edumanage.subjects.dto.SubjectDto;
import com.edumanage.subjects.entity.Subject;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.departments.repository.DepartmentRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditLogService auditLogService;

    public SubjectService(SubjectRepository subjectRepository, DepartmentRepository departmentRepository,
                          AuditLogService auditLogService) {
        this.subjectRepository = subjectRepository;
        this.departmentRepository = departmentRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<SubjectDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<Subject> page;
        if (tenantId != null) {
            page = subjectRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = subjectRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(subject -> mapToDto(subject, null)));
    }

    public SubjectDto getById(Long id) {
        var subject = subjectRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(subject.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(subject, fetchDepartmentName(subject.getDepartmentId()));
    }

    @Transactional
    public SubjectDto create(CreateSubjectRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        if (subjectRepository.existsBySchoolIdAndName(tenantId, request.getName())) {
            throw new ConflictException("Subject name already exists for this school");
        }
        if (request.getCode() != null && subjectRepository.existsBySchoolIdAndCode(tenantId, request.getCode())) {
            throw new ConflictException("Subject code already exists for this school");
        }

        Subject subject = new Subject();
        subject.setSchoolId(tenantId);
        subject.setName(request.getName());
        subject.setCode(request.getCode());
        subject.setDescription(request.getDescription());
        subject.setDepartmentId(request.getDepartmentId());
        subject.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        subjectRepository.save(subject);

        auditLogService.log("SUBJECT_CREATE", "Subject", subject.getId(),
                "Created subject " + subject.getName(), null, null, null);

        return mapToDto(subject, fetchDepartmentName(subject.getDepartmentId()));
    }

    @Transactional
    public SubjectDto update(Long id, CreateSubjectRequest request) {
        var subject = subjectRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(subject.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        if (!subject.getName().equals(request.getName()) &&
                subjectRepository.existsBySchoolIdAndName(subject.getSchoolId(), request.getName())) {
            throw new ConflictException("Subject name already exists for this school");
        }
        if (request.getCode() != null && !request.getCode().equals(subject.getCode()) &&
                subjectRepository.existsBySchoolIdAndCode(subject.getSchoolId(), request.getCode())) {
            throw new ConflictException("Subject code already exists for this school");
        }

        subject.setName(request.getName());
        if (request.getCode() != null) {
            subject.setCode(request.getCode());
        }
        subject.setDescription(request.getDescription());
        subject.setDepartmentId(request.getDepartmentId());
        if (request.getStatus() != null) {
            subject.setStatus(request.getStatus());
        }

        subjectRepository.save(subject);

        auditLogService.log("SUBJECT_UPDATE", "Subject", subject.getId(),
                "Updated subject " + subject.getName(), null, null, null);

        return mapToDto(subject, fetchDepartmentName(subject.getDepartmentId()));
    }

    @Transactional
    public SubjectDto updateStatus(Long id, String status) {
        var subject = subjectRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(subject.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        subject.setStatus(status);
        subjectRepository.save(subject);

        auditLogService.log("SUBJECT_UPDATE_STATUS", "Subject", subject.getId(),
                "Updated subject " + subject.getName() + " status to " + status, null, null, null);

        return mapToDto(subject, fetchDepartmentName(subject.getDepartmentId()));
    }

    private String fetchDepartmentName(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departmentRepository.findById(departmentId)
                .map(com.edumanage.departments.entity.Department::getName)
                .orElse(null);
    }

    private SubjectDto mapToDto(Subject entity, String departmentName) {
        SubjectDto dto = new SubjectDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setDescription(entity.getDescription());
        dto.setDepartmentId(entity.getDepartmentId());
        dto.setDepartmentName(departmentName);
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
