package com.edumanage.parents.service;

import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.parents.dto.CreateParentRequest;
import com.edumanage.parents.dto.ParentDto;
import com.edumanage.parents.entity.Parent;
import com.edumanage.parents.repository.ParentRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.security.AccessGuard;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParentService {

    private final ParentRepository parentRepository;
    private final AuditLogService auditLogService;
    private final AccessGuard accessGuard;

    public ParentService(ParentRepository parentRepository, AuditLogService auditLogService, AccessGuard accessGuard) {
        this.parentRepository = parentRepository;
        this.auditLogService = auditLogService;
        this.accessGuard = accessGuard;
    }

    public PageResponse<ParentDto> list(Pageable pageable) {
        accessGuard.assertCanListAllStudents();
        Long tenantId = TenantContext.getTenantId();
        Page<Parent> page;
        if (tenantId != null) {
            page = parentRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = parentRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(parent -> mapToDto(parent, parent.getFirstName() + " " + parent.getLastName())));
    }

    public ParentDto getById(Long id) {
        accessGuard.assertCanAccessParent(id);
        var parent = parentRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(parent.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(parent, parent.getFirstName() + " " + parent.getLastName());
    }

    @Transactional
    public ParentDto create(CreateParentRequest request) {
        accessGuard.assertAnyRole("MANAGER", "SUPER_ADMIN");
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        if (parentRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new ConflictException("Parent already exists for this user");
        }

        Parent parent = new Parent();
        parent.setSchoolId(tenantId);
        parent.setFirstName(request.getFirstName());
        parent.setLastName(request.getLastName());
        parent.setEmail(request.getEmail());
        parent.setPhone(request.getPhone());
        parent.setGender(request.getGender());
        parent.setOccupation(request.getOccupation());
        parent.setAddress(request.getAddress());
        parent.setUserId(request.getUserId());

        parentRepository.save(parent);

        auditLogService.log("PARENT_CREATE", "Parent", parent.getId(),
                "Created parent " + parent.getFirstName() + " " + parent.getLastName(), null, null, null);

        return mapToDto(parent, parent.getFirstName() + " " + parent.getLastName());
    }

    @Transactional
    public ParentDto update(Long id, CreateParentRequest request) {
        accessGuard.assertAnyRole("MANAGER", "SUPER_ADMIN");
        var parent = parentRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(parent.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        parent.setFirstName(request.getFirstName());
        parent.setLastName(request.getLastName());
        parent.setEmail(request.getEmail());
        parent.setPhone(request.getPhone());
        parent.setGender(request.getGender());
        parent.setOccupation(request.getOccupation());
        parent.setAddress(request.getAddress());
        parent.setUserId(request.getUserId());

        parentRepository.save(parent);

        auditLogService.log("PARENT_UPDATE", "Parent", parent.getId(),
                "Updated parent " + parent.getFirstName() + " " + parent.getLastName(), null, null, null);

        return mapToDto(parent, parent.getFirstName() + " " + parent.getLastName());
    }

    private ParentDto mapToDto(Parent entity, String fullName) {
        ParentDto dto = new ParentDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setFullName(fullName);
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setGender(entity.getGender());
        dto.setOccupation(entity.getOccupation());
        dto.setAddress(entity.getAddress());
        dto.setUserId(entity.getUserId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
