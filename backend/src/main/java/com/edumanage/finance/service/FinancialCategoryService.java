package com.edumanage.finance.service;

import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.finance.dto.CreateFinancialCategoryRequest;
import com.edumanage.finance.dto.FinancialCategoryDto;
import com.edumanage.finance.entity.FinancialCategory;
import com.edumanage.finance.repository.FinancialCategoryRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FinancialCategoryService {

    private final FinancialCategoryRepository financialCategoryRepository;
    private final AuditLogService auditLogService;

    public FinancialCategoryService(FinancialCategoryRepository financialCategoryRepository, AuditLogService auditLogService) {
        this.financialCategoryRepository = financialCategoryRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<FinancialCategoryDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<FinancialCategory> page;
        if (tenantId != null) {
            page = financialCategoryRepository.findBySchoolIdAndCategoryType(tenantId, "INCOME", pageable);
        } else {
            page = financialCategoryRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public List<FinancialCategoryDto> listByType(String categoryType) {
        Long tenantId = TenantContext.getTenantId();
        Long schoolId = tenantId != null ? tenantId : null;
        if (schoolId == null) {
            return financialCategoryRepository.findAll().stream()
                    .filter(c -> categoryType.equals(c.getCategoryType()))
                    .map(this::mapToDto)
                    .toList();
        }
        return financialCategoryRepository.findBySchoolIdAndCategoryType(schoolId, categoryType).stream()
                .map(this::mapToDto)
                .toList();
    }

    public FinancialCategoryDto getById(Long id) {
        var category = financialCategoryRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Financial category not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(category.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(category);
    }

    @Transactional
    public FinancialCategoryDto create(CreateFinancialCategoryRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        if (financialCategoryRepository.existsBySchoolIdAndName(tenantId, request.getName())) {
            throw new ConflictException("Category with this name already exists");
        }

        FinancialCategory category = new FinancialCategory();
        category.setSchoolId(tenantId);
        category.setName(request.getName());
        category.setCategoryType(request.getCategoryType());
        category.setDescription(request.getDescription());
        category.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        financialCategoryRepository.save(category);

        auditLogService.log("FINANCIAL_CATEGORY_CREATE", "FinancialCategory", category.getId(),
                "Created financial category " + category.getName(), null, null, null);

        return mapToDto(category);
    }

    @Transactional
    public FinancialCategoryDto update(Long id, CreateFinancialCategoryRequest request) {
        var category = financialCategoryRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Financial category not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(category.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        if (!category.getName().equals(request.getName()) &&
                financialCategoryRepository.existsBySchoolIdAndName(category.getSchoolId(), request.getName())) {
            throw new ConflictException("Category with this name already exists");
        }

        category.setName(request.getName());
        category.setCategoryType(request.getCategoryType());
        category.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }

        financialCategoryRepository.save(category);

        auditLogService.log("FINANCIAL_CATEGORY_UPDATE", "FinancialCategory", category.getId(),
                "Updated financial category " + category.getName(), null, null, null);

        return mapToDto(category);
    }

    private FinancialCategoryDto mapToDto(FinancialCategory entity) {
        FinancialCategoryDto dto = new FinancialCategoryDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setName(entity.getName());
        dto.setCategoryType(entity.getCategoryType());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
