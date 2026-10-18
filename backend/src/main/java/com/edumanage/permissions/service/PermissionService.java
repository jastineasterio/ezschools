package com.edumanage.permissions.service;

import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.permissions.dto.PermissionDto;
import com.edumanage.permissions.entity.Permission;
import com.edumanage.permissions.repository.PermissionRepository;
import com.edumanage.audit.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final AuditLogService auditLogService;

    public PermissionService(PermissionRepository permissionRepository, AuditLogService auditLogService) {
        this.permissionRepository = permissionRepository;
        this.auditLogService = auditLogService;
    }

    public List<PermissionDto> listPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public PermissionDto getPermissionByName(String name) {
        var permission = permissionRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + name));
        return mapToDto(permission);
    }

    @Transactional
    public PermissionDto createPermission(PermissionDto request) {
        if (permissionRepository.existsByName(request.getName())) {
            throw new ConflictException("Permission name already exists");
        }

        Permission permission = new Permission();
        permission.setName(request.getName());
        permission.setDisplayName(request.getDisplayName());
        permission.setDescription(request.getDescription());
        permission.setCategory(request.getCategory());

        permissionRepository.save(permission);

        auditLogService.log("PERMISSION_CREATE", "Permission", permission.getId(),
                "Created permission " + permission.getName(), null, null, null);

        return mapToDto(permission);
    }

    private PermissionDto mapToDto(Permission permission) {
        PermissionDto dto = new PermissionDto();
        dto.setId(permission.getId());
        dto.setName(permission.getName());
        dto.setDisplayName(permission.getDisplayName());
        dto.setDescription(permission.getDescription());
        dto.setCategory(permission.getCategory());
        return dto;
    }
}
