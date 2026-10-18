package com.edumanage.fees.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.fees.dto.CreateFeeStructureRequest;
import com.edumanage.fees.dto.FeeStructureDto;
import com.edumanage.fees.entity.FeeStructure;
import com.edumanage.fees.repository.FeeStructureRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class FeeStructureService {

    private final FeeStructureRepository feeStructureRepository;
    private final AuditLogService auditLogService;

    public FeeStructureService(FeeStructureRepository feeStructureRepository, AuditLogService auditLogService) {
        this.feeStructureRepository = feeStructureRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<FeeStructureDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<FeeStructure> page;
        if (tenantId != null) {
            page = feeStructureRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = feeStructureRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public FeeStructureDto getById(Long id) {
        var feeStructure = feeStructureRepository.findById(id)
                .filter(f -> !f.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Fee structure not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(feeStructure.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(feeStructure);
    }

    @Transactional
    public FeeStructureDto create(CreateFeeStructureRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        if (feeStructureRepository.existsBySchoolIdAndName(tenantId, request.getName())) {
            throw new ConflictException("Fee structure with this name already exists");
        }

        FeeStructure feeStructure = new FeeStructure();
        feeStructure.setSchoolId(tenantId);
        feeStructure.setName(request.getName());
        feeStructure.setDescription(request.getDescription());
        feeStructure.setAmount(request.getAmount());
        feeStructure.setDueDate(request.getDueDate());
        feeStructure.setAcademicYearId(request.getAcademicYearId());
        feeStructure.setClassId(request.getClassId());
        feeStructure.setFeeType(request.getFeeType() != null ? request.getFeeType() : "TUITION");
        feeStructure.setIsMandatory(request.getIsMandatory() != null ? request.getIsMandatory() : true);
        feeStructure.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        feeStructureRepository.save(feeStructure);

        auditLogService.log("FEE_STRUCTURE_CREATE", "FeeStructure", feeStructure.getId(),
                "Created fee structure " + feeStructure.getName(), null, null, null);

        return mapToDto(feeStructure);
    }

    @Transactional
    public FeeStructureDto update(Long id, CreateFeeStructureRequest request) {
        var feeStructure = feeStructureRepository.findById(id)
                .filter(f -> !f.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Fee structure not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(feeStructure.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        if (!feeStructure.getName().equals(request.getName()) &&
                feeStructureRepository.existsBySchoolIdAndName(feeStructure.getSchoolId(), request.getName())) {
            throw new ConflictException("Fee structure with this name already exists");
        }

        feeStructure.setName(request.getName());
        feeStructure.setDescription(request.getDescription());
        feeStructure.setAmount(request.getAmount());
        feeStructure.setDueDate(request.getDueDate());
        feeStructure.setAcademicYearId(request.getAcademicYearId());
        feeStructure.setClassId(request.getClassId());
        if (request.getFeeType() != null) {
            feeStructure.setFeeType(request.getFeeType());
        }
        if (request.getIsMandatory() != null) {
            feeStructure.setIsMandatory(request.getIsMandatory());
        }
        if (request.getStatus() != null) {
            feeStructure.setStatus(request.getStatus());
        }

        feeStructureRepository.save(feeStructure);

        auditLogService.log("FEE_STRUCTURE_UPDATE", "FeeStructure", feeStructure.getId(),
                "Updated fee structure " + feeStructure.getName(), null, null, null);

        return mapToDto(feeStructure);
    }

    private FeeStructureDto mapToDto(FeeStructure entity) {
        FeeStructureDto dto = new FeeStructureDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setAmount(entity.getAmount());
        dto.setDueDate(entity.getDueDate());
        dto.setAcademicYearId(entity.getAcademicYearId());
        dto.setClassId(entity.getClassId());
        dto.setFeeType(entity.getFeeType());
        dto.setIsMandatory(entity.getIsMandatory());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
