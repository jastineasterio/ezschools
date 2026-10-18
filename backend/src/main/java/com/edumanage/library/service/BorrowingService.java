package com.edumanage.library.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.library.dto.BorrowingDto;
import com.edumanage.library.dto.CreateBorrowingRequest;
import com.edumanage.library.entity.Book;
import com.edumanage.library.entity.BookCopy;
import com.edumanage.library.entity.Borrowing;
import com.edumanage.library.entity.LibraryMember;
import com.edumanage.library.repository.BookCopyRepository;
import com.edumanage.library.repository.BookRepository;
import com.edumanage.library.repository.BorrowingRepository;
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
public class BorrowingService {

    private static final BigDecimal DAILY_FINE_RATE = BigDecimal.ONE;

    private final BorrowingRepository borrowingRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;
    private final LibraryMemberRepository libraryMemberRepository;
    private final FineService fineService;
    private final AuditLogService auditLogService;

    public BorrowingService(BorrowingRepository borrowingRepository,
                            BookCopyRepository bookCopyRepository,
                            BookRepository bookRepository,
                            LibraryMemberRepository libraryMemberRepository,
                            FineService fineService,
                            AuditLogService auditLogService) {
        this.borrowingRepository = borrowingRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.bookRepository = bookRepository;
        this.libraryMemberRepository = libraryMemberRepository;
        this.fineService = fineService;
        this.auditLogService = auditLogService;
    }

    public PageResponse<BorrowingDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        Page<Borrowing> page = borrowingRepository.findBySchoolId(tenantId, pageable);
        return PageResponse.from(page.map(this::mapToDto));
    }

    public List<BorrowingDto> listByMember(Long memberId) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        return borrowingRepository.findBySchoolIdAndMemberId(tenantId, memberId).stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<BorrowingDto> listOverdue() {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        return borrowingRepository.findBySchoolIdAndDueDateBeforeAndStatus(tenantId, LocalDate.now(), "BORROWED").stream()
                .map(this::mapToDto)
                .toList();
    }

    public BorrowingDto get(Long id) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        Borrowing borrowing = borrowingRepository.findBySchoolIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Borrowing not found"));
        return mapToDto(borrowing);
    }

    @Transactional
    public BorrowingDto borrow(CreateBorrowingRequest request) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);

        BookCopy bookCopy = bookCopyRepository.findBySchoolIdAndId(tenantId, request.getBookCopyId())
                .orElseThrow(() -> new ResourceNotFoundException("Book copy not found"));

        if (!"AVAILABLE".equals(bookCopy.getStatus())) {
            throw new BadRequestException("Book copy is not available for borrowing");
        }

        if (borrowingRepository.existsBySchoolIdAndBookCopyIdAndStatus(tenantId, request.getBookCopyId(), "BORROWED")) {
            throw new BadRequestException("Book copy is already borrowed");
        }

        LibraryMember member = libraryMemberRepository.findBySchoolIdAndId(tenantId, request.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Library member not found"));

        if (!"ACTIVE".equals(member.getStatus())) {
            throw new BadRequestException("Library member is not active");
        }

        Borrowing borrowing = new Borrowing();
        borrowing.setSchoolId(tenantId);
        borrowing.setBookCopyId(request.getBookCopyId());
        borrowing.setMemberId(request.getMemberId());
        borrowing.setBorrowDate(request.getBorrowDate());
        borrowing.setDueDate(request.getDueDate());
        borrowing.setRemarks(request.getRemarks());
        borrowing.setStatus("BORROWED");

        borrowingRepository.save(borrowing);

        bookCopy.setStatus("BORROWED");
        bookCopyRepository.save(bookCopy);

        auditLogService.log("BORROWING_CREATE", "Borrowing", borrowing.getId(),
                "Borrowed book copy " + bookCopy.getBarcode() + " by member " + member.getMembershipNumber(), null, null, null);

        return mapToDto(borrowing);
    }

    @Transactional
    public BorrowingDto returnBook(Long id) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);

        Borrowing borrowing = borrowingRepository.findBySchoolIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Borrowing not found"));

        if (!"BORROWED".equals(borrowing.getStatus())) {
            throw new BadRequestException("Borrowing is not in borrowed status");
        }

        borrowing.setReturnDate(LocalDate.now());
        borrowing.setStatus("RETURNED");

        borrowingRepository.save(borrowing);

        BookCopy bookCopy = bookCopyRepository.findBySchoolIdAndId(tenantId, borrowing.getBookCopyId())
                .orElseThrow(() -> new ResourceNotFoundException("Book copy not found"));
        bookCopy.setStatus("AVAILABLE");
        bookCopyRepository.save(bookCopy);

        if (borrowing.getDueDate() != null && borrowing.getReturnDate().isAfter(borrowing.getDueDate())) {
            long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(borrowing.getDueDate(), borrowing.getReturnDate());
            BigDecimal fineAmount = DAILY_FINE_RATE.multiply(BigDecimal.valueOf(overdueDays));
            fineService.createFine(borrowing.getId(), fineAmount, "OVERDUE", "Auto-generated overdue fine for " + overdueDays + " days");
        }

        auditLogService.log("BORROWING_RETURN", "Borrowing", borrowing.getId(),
                "Returned book copy for borrowing", null, null, null);

        return mapToDto(borrowing);
    }

    @Transactional
    public void detectOverdue() {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);

        List<Borrowing> overdueBorrowings = borrowingRepository.findBySchoolIdAndDueDateBeforeAndStatus(tenantId, LocalDate.now(), "BORROWED");

        for (Borrowing borrowing : overdueBorrowings) {
            if (borrowing.getDueDate() != null) {
                long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(borrowing.getDueDate(), LocalDate.now());
                if (overdueDays > 0) {
                    BigDecimal fineAmount = DAILY_FINE_RATE.multiply(BigDecimal.valueOf(overdueDays));
                    fineService.createFine(borrowing.getId(), fineAmount, "OVERDUE", "Auto-generated overdue fine for " + overdueDays + " days");
                }
            }
        }

        auditLogService.log("OVERDUE_DETECT", "Borrowing", null,
                "Detected " + overdueBorrowings.size() + " overdue borrowings", null, null, null);
    }

    private BorrowingDto mapToDto(Borrowing entity) {
        BorrowingDto dto = new BorrowingDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setBookCopyId(entity.getBookCopyId());
        dto.setMemberId(entity.getMemberId());
        dto.setBorrowDate(entity.getBorrowDate());
        dto.setDueDate(entity.getDueDate());
        dto.setReturnDate(entity.getReturnDate());
        dto.setStatus(entity.getStatus());
        dto.setRemarks(entity.getRemarks());
        if (entity.getBookCopyId() != null) {
            bookCopyRepository.findById(entity.getBookCopyId()).ifPresent(c -> {
                dto.setBarcode(c.getBarcode());
                if (c.getBookId() != null) {
                    bookRepository.findById(c.getBookId()).ifPresent(b -> dto.setBookTitle(b.getTitle()));
                }
            });
        }
        if (entity.getMemberId() != null) {
            libraryMemberRepository.findById(entity.getMemberId()).ifPresent(m -> dto.setMemberName(m.getMembershipNumber()));
        }
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
