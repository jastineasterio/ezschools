package com.edumanage.roles.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.roles.dto.CreateRoleRequest;
import com.edumanage.roles.dto.RoleDto;
import com.edumanage.roles.entity.Role;
import com.edumanage.roles.repository.RoleRepository;
import com.edumanage.audit.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;

    public RoleService(RoleRepository roleRepository, AuditLogService auditLogService) {
        this.roleRepository = roleRepository;
        this.auditLogService = auditLogService;
    }

    public List<RoleDto> listRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public RoleDto getRoleByName(String name) {
        var role = roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + name));
        return mapToDto(role);
    }

    @Transactional
    public RoleDto createRole(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new ConflictException("Role name already exists");
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setDisplayName(request.getDisplayName());
        role.setDescription(request.getDescription());
        role.setActive(request.isActive());

        roleRepository.save(role);

        auditLogService.log("ROLE_CREATE", "Role", role.getId(),
                "Created role " + role.getName(), null, null, null);

        return mapToDto(role);
    }

    private RoleDto mapToDto(Role role) {
        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDisplayName(role.getDisplayName());
        dto.setDescription(role.getDescription());
        dto.setSystem(role.isSystem());
        dto.setActive(role.isActive());
        dto.setPermissions(role.getUsers() != null ? role.getUsers().stream().map(u -> u.getEmail()).collect(Collectors.toList()) : List.of());
        return dto;
    }
}
