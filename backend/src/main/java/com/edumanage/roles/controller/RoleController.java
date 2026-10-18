package com.edumanage.roles.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.roles.dto.CreateRoleRequest;
import com.edumanage.roles.dto.RoleDto;
import com.edumanage.roles.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/platform/roles")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDto>>> listRoles() {
        var roles = roleService.listRoles();
        return ResponseEntity.ok(new ApiResponse<>(true, "Roles fetched", roles, null, LocalDateTime.now(), "/api/platform/roles"));
    }

    @GetMapping("/{name}")
    public ResponseEntity<ApiResponse<RoleDto>> getRole(@PathVariable String name) {
        var role = roleService.getRoleByName(name);
        return ResponseEntity.ok(new ApiResponse<>(true, "Role fetched", role, null, LocalDateTime.now(), "/api/platform/roles/" + name));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoleDto>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        var role = roleService.createRole(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Role created", role, null, LocalDateTime.now(), "/api/platform/roles"));
    }
}
