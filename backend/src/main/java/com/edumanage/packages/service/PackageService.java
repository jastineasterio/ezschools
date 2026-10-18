package com.edumanage.packages.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.packages.dto.CreatePackageRequest;
import com.edumanage.packages.dto.PackageDto;
import com.edumanage.packages.dto.PackageFeatureDto;
import com.edumanage.packages.dto.PackageLimitDto;
import com.edumanage.packages.dto.PackageRoleDto;
import com.edumanage.packages.entity.Package;
import com.edumanage.packages.entity.PackageFeature;
import com.edumanage.packages.entity.PackageLimit;
import com.edumanage.packages.entity.PackageRole;
import com.edumanage.packages.repository.PackageRepository;
import com.edumanage.packages.repository.PackageFeatureRepository;
import com.edumanage.packages.repository.PackageLimitRepository;
import com.edumanage.packages.repository.PackageRoleRepository;
import com.edumanage.audit.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PackageService {

    private final PackageRepository packageRepository;
    private final PackageRoleRepository packageRoleRepository;
    private final PackageFeatureRepository packageFeatureRepository;
    private final PackageLimitRepository packageLimitRepository;
    private final AuditLogService auditLogService;

    public PackageService(PackageRepository packageRepository, PackageRoleRepository packageRoleRepository,
                          PackageFeatureRepository packageFeatureRepository, PackageLimitRepository packageLimitRepository,
                          AuditLogService auditLogService) {
        this.packageRepository = packageRepository;
        this.packageRoleRepository = packageRoleRepository;
        this.packageFeatureRepository = packageFeatureRepository;
        this.packageLimitRepository = packageLimitRepository;
        this.auditLogService = auditLogService;
    }

    public List<PackageDto> listPackages() {
        return packageRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public PackageDto getPackage(Long id) {
        var pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));
        return mapToDto(pkg);
    }

    @Transactional
    public PackageDto createPackage(CreatePackageRequest request) {
        if (packageRepository.existsByName(request.getName())) {
            throw new ConflictException("Package name already exists");
        }

        Package pkg = new Package();
        pkg.setName(request.getName());
        pkg.setDisplayName(request.getDisplayName());
        pkg.setDescription(request.getDescription());
        pkg.setPrice(request.getPrice());
        pkg.setBillingPeriod(request.getBillingPeriod());
        pkg.setActive(request.isActive());
        pkg.setDefault(request.isDefault());
        pkg.setSortOrder(request.getSortOrder());

        packageRepository.save(pkg);

        auditLogService.log("PACKAGE_CREATE", "Package", pkg.getId(),
                "Created package " + pkg.getName(), null, null, null);

        return mapToDto(pkg);
    }

    @Transactional
    public PackageDto updatePackage(Long id, CreatePackageRequest request) {
        var pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        pkg.setDisplayName(request.getDisplayName());
        pkg.setDescription(request.getDescription());
        pkg.setPrice(request.getPrice());
        pkg.setBillingPeriod(request.getBillingPeriod());
        pkg.setActive(request.isActive());
        pkg.setDefault(request.isDefault());
        pkg.setSortOrder(request.getSortOrder());

        packageRepository.save(pkg);

        auditLogService.log("PACKAGE_UPDATE", "Package", pkg.getId(),
                "Updated package " + pkg.getName(), null, null, null);

        return mapToDto(pkg);
    }

    @Transactional
    public PackageDto addRole(Long packageId, String roleName) {
        var pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        var existing = packageRoleRepository.findByPkgId(packageId).stream()
                .filter(r -> roleName.equals(r.getRoleName()))
                .findFirst();
        if (existing.isPresent()) {
            throw new ConflictException("Role already assigned to package");
        }

        PackageRole role = new PackageRole();
        role.setPkg(pkg);
        role.setRoleName(roleName);
        packageRoleRepository.save(role);

        auditLogService.log("PACKAGE_ROLE_ADD", "Package", pkg.getId(),
                "Added role " + roleName + " to package " + pkg.getName(), null, null, null);

        return mapToDto(pkg);
    }

    @Transactional
    public PackageDto addFeature(Long packageId, String featureKey, String featureName, boolean isEnabled) {
        var pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        var existing = packageFeatureRepository.findByPkgId(packageId).stream()
                .filter(f -> featureKey.equals(f.getFeatureKey()))
                .findFirst();
        if (existing.isPresent()) {
            throw new ConflictException("Feature already exists for package");
        }

        PackageFeature feature = new PackageFeature();
        feature.setPkg(pkg);
        feature.setFeatureKey(featureKey);
        feature.setFeatureName(featureName);
        feature.setEnabled(isEnabled);
        packageFeatureRepository.save(feature);

        auditLogService.log("PACKAGE_FEATURE_ADD", "Package", pkg.getId(),
                "Added feature " + featureName + " to package " + pkg.getName(), null, null, null);

        return mapToDto(pkg);
    }

    @Transactional
    public PackageDto addLimit(Long packageId, String limitKey, String limitName, long maxValue, String unit) {
        var pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        var existing = packageLimitRepository.findByPkgId(packageId).stream()
                .filter(l -> limitKey.equals(l.getLimitKey()))
                .findFirst();
        if (existing.isPresent()) {
            throw new ConflictException("Limit already exists for package");
        }

        PackageLimit limit = new PackageLimit();
        limit.setPkg(pkg);
        limit.setLimitKey(limitKey);
        limit.setLimitName(limitName);
        limit.setMaxValue(maxValue);
        limit.setCurrentValue(0);
        limit.setUnit(unit);
        packageLimitRepository.save(limit);

        auditLogService.log("PACKAGE_LIMIT_ADD", "Package", pkg.getId(),
                "Added limit " + limitName + " to package " + pkg.getName(), null, null, null);

        return mapToDto(pkg);
    }

    private PackageDto mapToDto(Package pkg) {
        PackageDto dto = new PackageDto();
        dto.setId(pkg.getId());
        dto.setUuid(pkg.getUuid() != null ? pkg.getUuid().toString() : null);
        dto.setName(pkg.getName());
        dto.setDisplayName(pkg.getDisplayName());
        dto.setDescription(pkg.getDescription());
        dto.setPrice(pkg.getPrice());
        dto.setBillingPeriod(pkg.getBillingPeriod());
        dto.setActive(pkg.isActive());
        dto.setDefault(pkg.isDefault());
        dto.setSortOrder(pkg.getSortOrder());
        dto.setCreatedAt(pkg.getCreatedAt());
        dto.setRoles(packageRoleRepository.findByPkgId(pkg.getId()).stream().map(this::mapRoleDto).collect(Collectors.toList()));
        dto.setFeatures(packageFeatureRepository.findByPkgId(pkg.getId()).stream().map(this::mapFeatureDto).collect(Collectors.toList()));
        dto.setLimits(packageLimitRepository.findByPkgId(pkg.getId()).stream().map(this::mapLimitDto).collect(Collectors.toList()));
        return dto;
    }

    private PackageRoleDto mapRoleDto(PackageRole role) {
        PackageRoleDto dto = new PackageRoleDto();
        dto.setId(role.getId());
        dto.setRoleName(role.getRoleName());
        dto.setCreatedAt(role.getCreatedAt());
        return dto;
    }

    private PackageFeatureDto mapFeatureDto(PackageFeature feature) {
        PackageFeatureDto dto = new PackageFeatureDto();
        dto.setId(feature.getId());
        dto.setFeatureKey(feature.getFeatureKey());
        dto.setFeatureName(feature.getFeatureName());
        dto.setEnabled(feature.isEnabled());
        dto.setCreatedAt(feature.getCreatedAt());
        return dto;
    }

    private PackageLimitDto mapLimitDto(PackageLimit limit) {
        PackageLimitDto dto = new PackageLimitDto();
        dto.setId(limit.getId());
        dto.setLimitKey(limit.getLimitKey());
        dto.setLimitName(limit.getLimitName());
        dto.setMaxValue(limit.getMaxValue());
        dto.setCurrentValue(limit.getCurrentValue());
        dto.setUnit(limit.getUnit());
        dto.setCreatedAt(limit.getCreatedAt());
        dto.setUpdatedAt(limit.getUpdatedAt());
        return dto;
    }
}
