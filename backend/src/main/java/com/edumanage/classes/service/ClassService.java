package com.edumanage.classes.service;

import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.classes.dto.ClassDto;
import com.edumanage.classes.dto.CreateClassRequest;
import com.edumanage.classes.entity.Class;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassService {

    private final ClassRepository classRepository;
    private final AuditLogService auditLogService;

    public ClassService(ClassRepository classRepository, AuditLogService auditLogService) {
        this.classRepository = classRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<ClassDto> list(Pageable pageable, String search) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null && !TenantContext.isSuperAdmin()) {
            throw new ForbiddenException("A school assignment is required");
        }
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        Page<Class> page = classRepository.search(tenantId, normalizedSearch, pageable);
        return PageResponse.from(page.map(this::mapToDto));
    }

    public ClassDto getById(Long id) {
        var clazz = classRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        TenantContext.assertTenantAccess(clazz.getSchoolId());

        return mapToDto(clazz);
    }

    @Transactional
    public ClassDto create(CreateClassRequest request) {
        Long tenantId = TenantContext.requireTenantId();

        if (classRepository.existsBySchoolIdAndName(tenantId, request.getName())) {
            throw new ConflictException("Class name '" + request.getName() + "' already exists for this school");
        }

        Class clazz = new Class();
        clazz.setSchoolId(tenantId);
        clazz.setName(request.getName());
        clazz.setCode(request.getCode());
        clazz.setDescription(request.getDescription());
        clazz.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        classRepository.save(clazz);

        auditLogService.log("CLASS_CREATE", "Class", clazz.getId(),
                "Created class " + clazz.getName(), null, null, null);

        return mapToDto(clazz);
    }

    @Transactional
    public ClassDto update(Long id, CreateClassRequest request) {
        var clazz = classRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        TenantContext.assertTenantAccess(clazz.getSchoolId());

        if (!clazz.getName().equals(request.getName()) &&
                classRepository.existsBySchoolIdAndName(clazz.getSchoolId(), request.getName())) {
            throw new ConflictException("Class name '" + request.getName() + "' already exists for this school");
        }

        clazz.setName(request.getName());
        if (request.getCode() != null) {
            clazz.setCode(request.getCode());
        }
        clazz.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            clazz.setStatus(request.getStatus());
        }

        classRepository.save(clazz);

        auditLogService.log("CLASS_UPDATE", "Class", clazz.getId(),
                "Updated class " + clazz.getName(), null, null, null);

        return mapToDto(clazz);
    }

    @Transactional
    public ClassDto updateStatus(Long id, String status) {
        var clazz = classRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        TenantContext.assertTenantAccess(clazz.getSchoolId());

        clazz.setStatus(status);
        classRepository.save(clazz);

        auditLogService.log("CLASS_UPDATE_STATUS", "Class", clazz.getId(),
                "Updated class " + clazz.getName() + " status to " + status, null, null, null);

        return mapToDto(clazz);
    }

    private ClassDto mapToDto(Class entity) {
        ClassDto dto = new ClassDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
