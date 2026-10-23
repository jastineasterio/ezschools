package com.edumanage.finance.service;

import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.finance.dto.CreateFinancialTransactionRequest;
import com.edumanage.finance.dto.FinancialTransactionDto;
import com.edumanage.finance.entity.FinancialCategory;
import com.edumanage.finance.entity.FinancialTransaction;
import com.edumanage.finance.repository.FinancialCategoryRepository;
import com.edumanage.finance.repository.FinancialTransactionRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.security.AccessGuard;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class FinancialTransactionService {

    private final FinancialTransactionRepository financialTransactionRepository;
    private final FinancialCategoryRepository financialCategoryRepository;
    private final AuditLogService auditLogService;
    private final AccessGuard accessGuard;

    public FinancialTransactionService(FinancialTransactionRepository financialTransactionRepository,
                                       FinancialCategoryRepository financialCategoryRepository,
                                       AuditLogService auditLogService,
                                       AccessGuard accessGuard) {
        this.financialTransactionRepository = financialTransactionRepository;
        this.financialCategoryRepository = financialCategoryRepository;
        this.auditLogService = auditLogService;
        this.accessGuard = accessGuard;
    }

    public PageResponse<FinancialTransactionDto> list(Pageable pageable) {
        accessGuard.assertFinancialAccess();
        Long tenantId = TenantContext.getTenantId();
        Page<FinancialTransaction> page;
        if (tenantId != null) {
            page = financialTransactionRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = financialTransactionRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    @Transactional
    public FinancialTransactionDto create(CreateFinancialTransactionRequest request) {
        accessGuard.assertFinancialAccess();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        var category = financialCategoryRepository.findById(request.getCategoryId())
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Financial category not found"));

        if (!tenantId.equals(category.getSchoolId())) {
            throw new ForbiddenException("Financial category does not belong to this school");
        }

        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setSchoolId(tenantId);
        transaction.setCategoryId(request.getCategoryId());
        transaction.setAmount(request.getAmount());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setReferenceNumber(request.getReferenceNumber());
        transaction.setDescription(request.getDescription());
        transaction.setPaymentId(request.getPaymentId());
        transaction.setFeeAssignmentId(request.getFeeAssignmentId());
        transaction.setStatus("COMPLETED");

        financialTransactionRepository.save(transaction);

        auditLogService.log("FINANCIAL_TRANSACTION_CREATE", "FinancialTransaction", transaction.getId(),
                "Created financial transaction of " + request.getAmount(), null, null, null);

        return mapToDto(transaction);
    }

    private FinancialTransactionDto mapToDto(FinancialTransaction entity) {
        FinancialTransactionDto dto = new FinancialTransactionDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setCategoryId(entity.getCategoryId());
        dto.setAmount(entity.getAmount());
        dto.setTransactionType(entity.getTransactionType());
        dto.setTransactionDate(entity.getTransactionDate());
        dto.setReferenceNumber(entity.getReferenceNumber());
        dto.setDescription(entity.getDescription());
        dto.setPaymentId(entity.getPaymentId());
        dto.setFeeAssignmentId(entity.getFeeAssignmentId());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        if (entity.getCategoryId() != null) {
            financialCategoryRepository.findById(entity.getCategoryId())
                    .ifPresent(c -> dto.setCategoryName(c.getName()));
        }
        return dto;
    }
}
