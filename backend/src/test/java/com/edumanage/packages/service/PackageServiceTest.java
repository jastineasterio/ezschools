package com.edumanage.packages.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.packages.dto.CreatePackageRequest;
import com.edumanage.packages.entity.Package;
import com.edumanage.packages.repository.PackageFeatureRepository;
import com.edumanage.packages.repository.PackageLimitRepository;
import com.edumanage.packages.repository.PackageRepository;
import com.edumanage.packages.repository.PackageRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PackageServiceTest {

    private PackageRepository packageRepository;
    private PackageRoleRepository packageRoleRepository;
    private PackageFeatureRepository packageFeatureRepository;
    private PackageLimitRepository packageLimitRepository;
    private PackageService packageService;

    @BeforeEach
    void setUp() {
        packageRepository = mock(PackageRepository.class);
        packageRoleRepository = mock(PackageRoleRepository.class);
        packageFeatureRepository = mock(PackageFeatureRepository.class);
        packageLimitRepository = mock(PackageLimitRepository.class);
        packageService = new PackageService(
                packageRepository,
                packageRoleRepository,
                packageFeatureRepository,
                packageLimitRepository,
                mock(AuditLogService.class));
    }

    @Test
    void createsPackageWithRequestedActiveAndDefaultValues() {
        var request = new CreatePackageRequest();
        request.setName("starter");
        request.setDisplayName("Starter");
        request.setPrice(new BigDecimal("10.00"));
        request.setBillingPeriod("MONTHLY");
        request.setActive(false);
        request.setDefault(true);

        when(packageRepository.existsByName("starter")).thenReturn(false);
        when(packageRepository.save(any(Package.class))).thenAnswer(invocation -> {
            Package saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });
        when(packageRoleRepository.findByPkgId(42L)).thenReturn(List.of());
        when(packageFeatureRepository.findByPkgId(42L)).thenReturn(List.of());
        when(packageLimitRepository.findByPkgId(42L)).thenReturn(List.of());

        var response = packageService.createPackage(request);

        assertFalse(response.isActive());
        assertTrue(response.isDefault());
        var savedPackage = org.mockito.ArgumentCaptor.forClass(Package.class);
        verify(packageRepository).save(savedPackage.capture());
        assertFalse(savedPackage.getValue().isActive());
        assertTrue(savedPackage.getValue().isDefault());
    }
}
