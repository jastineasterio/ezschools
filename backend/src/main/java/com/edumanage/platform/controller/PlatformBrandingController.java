package com.edumanage.platform.controller;

import com.edumanage.branding.dto.SchoolBrandingDto;
import com.edumanage.branding.service.SchoolBrandingService;
import com.edumanage.branding.storage.FileStorageService;
import com.edumanage.common.dto.ApiResponse;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.schools.repository.SchoolRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/platform/schools/{schoolId}/branding")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class PlatformBrandingController {

    private final SchoolBrandingService brandingService;
    private final FileStorageService fileStorageService;
    private final SchoolRepository schoolRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<SchoolBrandingDto>> getBranding(@PathVariable Long schoolId) {
        requireSchool(schoolId);
        var branding = brandingService.getBrandingForSchool(schoolId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Branding fetched", branding, null,
                LocalDateTime.now(), "/api/platform/schools/" + schoolId + "/branding"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<SchoolBrandingDto>> updateBranding(@PathVariable Long schoolId,
                                                                         @RequestBody SchoolBrandingDto request) {
        requireSchool(schoolId);
        var branding = brandingService.updateBrandingForSchool(schoolId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Branding updated", branding, null,
                LocalDateTime.now(), "/api/platform/schools/" + schoolId + "/branding"));
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SchoolBrandingDto>> uploadLogo(@PathVariable Long schoolId,
                                                                     @RequestParam("file") MultipartFile file,
                                                                     HttpServletRequest request) {
        requireSchool(schoolId);
        // Store the file first, but remove it if database persistence fails so no
        // orphaned logo file is left behind.
        String fileName = null;
        try {
            fileName = fileStorageService.storeLogo(file);
            String logoUrl = buildLogoUrl(request, fileName);
            var branding = brandingService.uploadLogo(schoolId, logoUrl);
            return ResponseEntity.ok(new ApiResponse<>(true, "Logo uploaded", branding, null,
                    LocalDateTime.now(), "/api/platform/schools/" + schoolId + "/branding/logo"));
        } catch (Exception ex) {
            if (fileName != null) {
                fileStorageService.deleteLogo(fileName);
            }
            throw ex;
        }
    }

    @DeleteMapping("/logo")
    public ResponseEntity<ApiResponse<SchoolBrandingDto>> removeLogo(@PathVariable Long schoolId) {
        requireSchool(schoolId);
        var branding = brandingService.removeLogo(schoolId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Logo removed", branding, null,
                LocalDateTime.now(), "/api/platform/schools/" + schoolId + "/branding/logo"));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<SchoolBrandingDto>> resetBranding(@PathVariable Long schoolId) {
        requireSchool(schoolId);
        var branding = brandingService.resetBranding(schoolId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Branding restored to default", branding, null,
                LocalDateTime.now(), "/api/platform/schools/" + schoolId + "/branding/reset"));
    }

    private void requireSchool(Long schoolId) {
        if (!schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException("School not found");
        }
    }

    private String buildLogoUrl(HttpServletRequest request, String fileName) {
        String contextPath = request.getContextPath();
        return UriComponentsBuilder.fromUriString(request.getRequestURL().toString())
                .replacePath(contextPath + "/uploads/logos/" + fileName)
                .build()
                .toUriString();
    }
}
