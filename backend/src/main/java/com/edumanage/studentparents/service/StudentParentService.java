package com.edumanage.studentparents.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.studentparents.dto.CreateStudentParentRequest;
import com.edumanage.studentparents.dto.StudentParentDto;
import com.edumanage.studentparents.entity.StudentParent;
import com.edumanage.studentparents.repository.StudentParentRepository;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.parents.repository.ParentRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentParentService {

    private final StudentParentRepository studentParentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ParentRepository parentRepository;
    private final AuditLogService auditLogService;

    public StudentParentService(StudentParentRepository studentParentRepository,
                                StudentProfileRepository studentProfileRepository,
                                ParentRepository parentRepository,
                                AuditLogService auditLogService) {
        this.studentParentRepository = studentParentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.parentRepository = parentRepository;
        this.auditLogService = auditLogService;
    }

    public List<StudentParentDto> listByStudent(Long studentId) {
        var student = studentProfileRepository.findById(studentId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return studentParentRepository.findByStudentId(studentId).stream()
                .map(relation -> mapToDto(relation, null, null))
                .collect(Collectors.toList());
    }

    public List<StudentParentDto> listByParent(Long parentId) {
        var parent = parentRepository.findById(parentId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(parent.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return studentParentRepository.findByParentId(parentId).stream()
                .map(relation -> mapToDto(relation, null, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public StudentParentDto create(CreateStudentParentRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BadRequestException("Tenant context is required");
        }

        var student = studentProfileRepository.findById(request.getStudentId())
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (!tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Student does not belong to this school");
        }

        var parent = parentRepository.findById(request.getParentId())
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found"));

        if (!tenantId.equals(parent.getSchoolId())) {
            throw new ForbiddenException("Parent does not belong to this school");
        }

        if (studentParentRepository.findByStudentIdAndParentIdAndRelationshipType(
                request.getStudentId(), request.getParentId(), request.getRelationshipType()).isPresent()) {
            throw new com.edumanage.exception.ConflictException("Relationship already exists");
        }

        StudentParent relation = new StudentParent();
        relation.setStudentId(request.getStudentId());
        relation.setParentId(request.getParentId());
        relation.setRelationshipType(request.getRelationshipType());
        relation.setPrimary(request.isPrimary());
        relation.setReceivesSms(request.isReceivesSms());
        relation.setReceivesNotifications(request.isReceivesNotifications());

        studentParentRepository.save(relation);

        auditLogService.log("STUDENT_PARENT_CREATE", "StudentParent", relation.getId(),
                "Created student-parent relation", null, null, null);

        return mapToDto(relation, student.getFirstName() + " " + student.getLastName(), parent.getFirstName() + " " + parent.getLastName());
    }

    @Transactional
    public StudentParentDto update(Long id, CreateStudentParentRequest request) {
        var relation = studentParentRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student-parent relation not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BadRequestException("Tenant context is required");
        }

        var student = studentProfileRepository.findById(request.getStudentId())
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (!tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Student does not belong to this school");
        }

        var parent = parentRepository.findById(request.getParentId())
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found"));

        if (!tenantId.equals(parent.getSchoolId())) {
            throw new ForbiddenException("Parent does not belong to this school");
        }

        relation.setStudentId(request.getStudentId());
        relation.setParentId(request.getParentId());
        relation.setRelationshipType(request.getRelationshipType());
        relation.setPrimary(request.isPrimary());
        relation.setReceivesSms(request.isReceivesSms());
        relation.setReceivesNotifications(request.isReceivesNotifications());

        studentParentRepository.save(relation);

        auditLogService.log("STUDENT_PARENT_UPDATE", "StudentParent", relation.getId(),
                "Updated student-parent relation", null, null, null);

        return mapToDto(relation, student.getFirstName() + " " + student.getLastName(), parent.getFirstName() + " " + parent.getLastName());
    }

    @Transactional
    public void delete(Long id) {
        var relation = studentParentRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student-parent relation not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            var student = studentProfileRepository.findById(relation.getStudentId()).orElse(null);
            if (student == null || !tenantId.equals(student.getSchoolId())) {
                throw new ForbiddenException("Access denied");
            }
        }

        relation.setDeletedAt(java.time.LocalDateTime.now());
        studentParentRepository.save(relation);

        auditLogService.log("STUDENT_PARENT_DELETE", "StudentParent", relation.getId(),
                "Deleted student-parent relation", null, null, null);
    }

    private StudentParentDto mapToDto(StudentParent entity, String studentName, String parentName) {
        StudentParentDto dto = new StudentParentDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setStudentId(entity.getStudentId());
        dto.setStudentName(studentName);
        dto.setParentId(entity.getParentId());
        dto.setParentName(parentName);
        dto.setRelationshipType(entity.getRelationshipType());
        dto.setPrimary(entity.isPrimary());
        dto.setReceivesSms(entity.isReceivesSms());
        dto.setReceivesNotifications(entity.isReceivesNotifications());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
