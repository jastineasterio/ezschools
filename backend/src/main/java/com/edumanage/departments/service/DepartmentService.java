package com.edumanage.departments.service;

import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.departments.dto.CreateDepartmentRequest;
import com.edumanage.departments.dto.DepartmentDto;
import com.edumanage.departments.entity.Department;
import com.edumanage.departments.repository.DepartmentRepository;
import com.edumanage.users.repository.UserRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public DepartmentService(DepartmentRepository departmentRepository, UserRepository userRepository,
                             AuditLogService auditLogService) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<DepartmentDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<Department> page;
        if (tenantId != null) {
            page = departmentRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = departmentRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(dept -> mapToDto(dept, null)));
    }

    public DepartmentDto getById(Long id) {
        var department = departmentRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(department.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(department, fetchHeadOfDepartmentName(department.getHeadOfDepartmentId()));
    }

    @Transactional
    public DepartmentDto create(CreateDepartmentRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        if (departmentRepository.existsBySchoolIdAndName(tenantId, request.getName())) {
            throw new ConflictException("Department name already exists for this school");
        }
        if (request.getCode() != null && departmentRepository.existsBySchoolIdAndCode(tenantId, request.getCode())) {
            throw new ConflictException("Department code already exists for this school");
        }

        Department department = new Department();
        department.setSchoolId(tenantId);
        department.setName(request.getName());
        department.setCode(request.getCode());
        department.setDescription(request.getDescription());
        department.setHeadOfDepartmentId(request.getHeadOfDepartmentId());
        department.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        departmentRepository.save(department);

        auditLogService.log("DEPARTMENT_CREATE", "Department", department.getId(),
                "Created department " + department.getName(), null, null, null);

        return mapToDto(department, fetchHeadOfDepartmentName(department.getHeadOfDepartmentId()));
    }

    @Transactional
    public DepartmentDto update(Long id, CreateDepartmentRequest request) {
        var department = departmentRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(department.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        if (!department.getName().equals(request.getName()) &&
                departmentRepository.existsBySchoolIdAndName(department.getSchoolId(), request.getName())) {
            throw new ConflictException("Department name already exists for this school");
        }
        if (request.getCode() != null && !request.getCode().equals(department.getCode()) &&
                departmentRepository.existsBySchoolIdAndCode(department.getSchoolId(), request.getCode())) {
            throw new ConflictException("Department code already exists for this school");
        }

        department.setName(request.getName());
        if (request.getCode() != null) {
            department.setCode(request.getCode());
        }
        department.setDescription(request.getDescription());
        department.setHeadOfDepartmentId(request.getHeadOfDepartmentId());
        if (request.getStatus() != null) {
            department.setStatus(request.getStatus());
        }

        departmentRepository.save(department);

        auditLogService.log("DEPARTMENT_UPDATE", "Department", department.getId(),
                "Updated department " + department.getName(), null, null, null);

        return mapToDto(department, fetchHeadOfDepartmentName(department.getHeadOfDepartmentId()));
    }

    @Transactional
    public DepartmentDto updateStatus(Long id, String status) {
        var department = departmentRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(department.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        department.setStatus(status);
        departmentRepository.save(department);

        auditLogService.log("DEPARTMENT_UPDATE_STATUS", "Department", department.getId(),
                "Updated department " + department.getName() + " status to " + status, null, null, null);

        return mapToDto(department, fetchHeadOfDepartmentName(department.getHeadOfDepartmentId()));
    }

    private String fetchHeadOfDepartmentName(Long headOfDepartmentId) {
        if (headOfDepartmentId == null) {
            return null;
        }
        return userRepository.findById(headOfDepartmentId)
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse(null);
    }

    private DepartmentDto mapToDto(Department entity, String headOfDepartmentName) {
        DepartmentDto dto = new DepartmentDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setDescription(entity.getDescription());
        dto.setHeadOfDepartmentId(entity.getHeadOfDepartmentId());
        dto.setHeadOfDepartmentName(headOfDepartmentName);
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
