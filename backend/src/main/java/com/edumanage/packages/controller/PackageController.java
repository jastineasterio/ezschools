package com.edumanage.packages.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.packages.dto.CreatePackageRequest;
import com.edumanage.packages.dto.PackageDto;
import com.edumanage.packages.service.PackageService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/platform/packages")
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PackageDto>>> listPackages(Pageable pageable) {
        var packages = packageService.listPackages();
        return ResponseEntity.ok(new ApiResponse<>(true, "Packages fetched", packages, null, LocalDateTime.now(), "/api/platform/packages"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PackageDto>> getPackage(@PathVariable Long id) {
        var pkg = packageService.getPackage(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Package fetched", pkg, null, LocalDateTime.now(), "/api/platform/packages/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PackageDto>> createPackage(@Valid @RequestBody CreatePackageRequest request) {
        var pkg = packageService.createPackage(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Package created", pkg, null, LocalDateTime.now(), "/api/platform/packages"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PackageDto>> updatePackage(@PathVariable Long id,
                                                                 @Valid @RequestBody CreatePackageRequest request) {
        var pkg = packageService.updatePackage(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Package updated", pkg, null, LocalDateTime.now(), "/api/platform/packages/" + id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PackageDto>> updatePackageStatus(@PathVariable Long id,
                                                                       @RequestParam boolean active) {
        var pkg = packageService.getPackage(id);
        pkg.setActive(active);
        return ResponseEntity.ok(new ApiResponse<>(true, "Package status updated", pkg, null, LocalDateTime.now(), "/api/platform/packages/" + id + "/status"));
    }
}
