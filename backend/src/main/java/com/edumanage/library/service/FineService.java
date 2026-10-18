package com.edumanage.library.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.library.dto.CreateFineRequest;
import com.edumanage.library.dto.FineDto;
import com.edumanage.library.entity.Fine;
import com.edumanage.library.entity.LibraryMember;
import com.edumanage.library.repository.BorrowingRepository;
import com.edumanage.library.repository.FineRepository;
import com.edumanage.library.repository.LibraryMemberRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import com.edumanage.library.util.LibraryFeatureGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class FineService {

    private final FineRepository fineRepository;
    private final BorrowingRepository borrowingRepository;
    private final LibraryMemberRepository libraryMemberRepository;
    private final AuditLogService auditLogService;

    public FineService(FineRepository fineRepository,
                       BorrowingRepository borrowingRepository,
                       LibraryMemberRepository libraryMemberRepository,
                       AuditLogService auditLogService) {
        this.fineRepository = fineRepository;
        this.borrowingRepository = borrowingRepository;
        this.libraryMemberRepository = libraryMemberRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<FineDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        Page<Fine> page = fineRepository.findBySchoolId(tenantId, pageable);
        return PageResponse.from(page.map(this::mapToDto));
    }

    public List<FineDto> listPending() {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        return fineRepository.findBySchoolIdAndStatus(tenantId, "PENDING").stream()
                .map(this::mapToDto)
                .toList();
    }

    public FineDto get(Long id) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        Fine fine = fineRepository.findBySchoolIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Fine not found"));
        return mapToDto(fine);
    }

    @Transactional
    public FineDto createFine(Long borrowingId, BigDecimal fineAmount, String fineType, String remarks) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);

        var borrowing = borrowingRepository.findBySchoolIdAndId(tenantId, borrowingId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrowing not found"));

        LibraryMember member = libraryMemberRepository.findBySchoolIdAndId(tenantId, borrowing.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Library member not found"));

        if (fineRepository.findBySchoolIdAndBorrowingId(tenantId, borrowingId).stream()
                .anyMatch(f -> !"WAIVED".equals(f.getStatus()) && !"PAID".equals(f.getStatus()))) {
            throw new BadRequestException("An active fine already exists for this borrowing");
        }

        Fine fine = new Fine();
        fine.setSchoolId(tenantId);
        fine.setBorrowingId(borrowingId);
        fine.setMemberId(borrowing.getMemberId());
        fine.setFineAmount(fineAmount);
        fine.setPaidAmount(BigDecimal.ZERO);
        fine.setBalance(fineAmount);
        fine.setFineType(fineType != null ? fineType : "OVERDUE");
        fine.setStatus("PENDING");
        fine.setRemarks(remarks);

        fineRepository.save(fine);
        auditLogService.log("FINE_CREATE", "Fine", fine.getId(),
                "Created fine of " + fineAmount + " for borrowing " + borrowingId, null, null, null);
        return mapToDto(fine);
    }

    @Transactional
    public FineDto createFineFromRequest(CreateFineRequest request) {
        return createFine(request.getBorrowingId(), request.getFineAmount(),
                request.getFineType() != null ? request.getFineType() : "OVERDUE", request.getRemarks());
    }

    @Transactional
    public FineDto payFine(Long id, BigDecimal amount) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);

        Fine fine = fineRepository.findBySchoolIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Fine not found"));

        if ("PAID".equals(fine.getStatus()) || "WAIVED".equals(fine.getStatus())) {
            throw new BadRequestException("Fine is already settled");
        }

        if (amount.compareTo(fine.getBalance()) > 0) {
            throw new BadRequestException("Payment amount exceeds fine balance");
        }

        BigDecimal newPaid = fine.getPaidAmount().add(amount);
        BigDecimal newBalance = fine.getBalance().subtract(amount);

        fine.setPaidAmount(newPaid);
        fine.setBalance(newBalance);

        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            fine.setStatus("PAID");
            fine.setPaidDate(LocalDate.now());
        }

        fineRepository.save(fine);
        auditLogService.log("FINE_PAY", "Fine", fine.getId(),
                "Recorded payment of " + amount + " for fine", null, null, null);
        return mapToDto(fine);
    }

    @Transactional
    public FineDto waiveFine(Long id) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);

        Fine fine = fineRepository.findBySchoolIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Fine not found"));

        if ("PAID".equals(fine.getStatus()) || "WAIVED".equals(fine.getStatus())) {
            throw new BadRequestException("Fine is already settled");
        }

        fine.setStatus("WAIVED");
        fine.setBalance(BigDecimal.ZERO);
        fineRepository.save(fine);
        auditLogService.log("FINE_WAIVE", "Fine", fine.getId(),
                "Waived fine of " + fine.getFineAmount(), null, null, null);
        return mapToDto(fine);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);

        Fine fine = fineRepository.findBySchoolIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Fine not found"));
        fine.setDeletedAt(java.time.LocalDateTime.now());
        fineRepository.save(fine);
        auditLogService.log("FINE_DELETE", "Fine", fine.getId(),
                "Deleted fine for borrowing " + fine.getBorrowingId(), null, null, null);
    }

    private FineDto mapToDto(Fine entity) {
        FineDto dto = new FineDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setBorrowingId(entity.getBorrowingId());
        dto.setMemberId(entity.getMemberId());
        dto.setFineAmount(entity.getFineAmount());
        dto.setPaidAmount(entity.getPaidAmount());
        dto.setBalance(entity.getBalance());
        dto.setFineType(entity.getFineType());
        dto.setStatus(entity.getStatus());
        dto.setPaidDate(entity.getPaidDate());
        dto.setRemarks(entity.getRemarks());
        if (entity.getMemberId() != null) {
            libraryMemberRepository.findById(entity.getMemberId()).ifPresent(m -> {
                dto.setMemberName(m.getMembershipNumber());
            });
        }
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
