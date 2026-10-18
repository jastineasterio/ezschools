package com.edumanage.permissions.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.permissions.dto.PermissionDto;
import com.edumanage.permissions.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/platform/permissions")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PermissionDto>>> listPermissions() {
        var permissions = permissionService.listPermissions();
        return ResponseEntity.ok(new ApiResponse<>(true, "Permissions fetched", permissions, null, LocalDateTime.now(), "/api/platform/permissions"));
    }

    @GetMapping("/{name}")
    public ResponseEntity<ApiResponse<PermissionDto>> getPermission(@PathVariable String name) {
        var permission = permissionService.getPermissionByName(name);
        return ResponseEntity.ok(new ApiResponse<>(true, "Permission fetched", permission, null, LocalDateTime.now(), "/api/platform/permissions/" + name));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PermissionDto>> createPermission(@Valid @RequestBody PermissionDto request) {
        var permission = permissionService.createPermission(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Permission created", permission, null, LocalDateTime.now(), "/api/platform/permissions"));
    }
}
