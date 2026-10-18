package com.edumanage.library.service;

import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.library.dto.BookCopyDto;
import com.edumanage.library.dto.CreateBookCopyRequest;
import com.edumanage.library.entity.Book;
import com.edumanage.library.entity.BookCopy;
import com.edumanage.library.repository.BookCopyRepository;
import com.edumanage.library.repository.BookRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import com.edumanage.library.util.LibraryFeatureGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookCopyService {

    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;
    private final AuditLogService auditLogService;

    public BookCopyService(BookCopyRepository bookCopyRepository, BookRepository bookRepository,
                           AuditLogService auditLogService) {
        this.bookCopyRepository = bookCopyRepository;
        this.bookRepository = bookRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<BookCopyDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        Page<BookCopy> page = bookCopyRepository.findBySchoolId(tenantId, pageable);
        return PageResponse.from(page.map(this::mapToDto));
    }

    public List<BookCopyDto> listByBook(Long bookId) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        Book book = bookRepository.findBySchoolIdAndId(tenantId, bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        return bookCopyRepository.findBySchoolIdAndBookId(tenantId, bookId).stream()
                .map(this::mapToDto)
                .toList();
    }

    public BookCopyDto get(Long id) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        BookCopy copy = bookCopyRepository.findBySchoolIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Book copy not found"));
        return mapToDto(copy);
    }

    @Transactional
    public BookCopyDto create(CreateBookCopyRequest request) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);

        Book book = bookRepository.findBySchoolIdAndId(tenantId, request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        if (bookCopyRepository.existsBySchoolIdAndBarcode(tenantId, request.getBarcode())) {
            throw new com.edumanage.exception.ConflictException("Barcode already exists");
        }

        BookCopy copy = new BookCopy();
        copy.setSchoolId(tenantId);
        copy.setBookId(request.getBookId());
        copy.setBarcode(request.getBarcode());
        copy.setConditionStatus(request.getConditionStatus() != null ? request.getConditionStatus() : "GOOD");
        copy.setRemarks(request.getRemarks());
        copy.setAcquiredDate(request.getAcquiredDate());
        copy.setStatus(request.getStatus() != null ? request.getStatus() : "AVAILABLE");

        bookCopyRepository.save(copy);

        book.setTotalCopies(book.getTotalCopies() + 1);
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        auditLogService.log("BOOK_COPY_CREATE", "BookCopy", copy.getId(),
                "Created book copy with barcode: " + request.getBarcode(), null, null, null);
        return mapToDto(copy);
    }

    @Transactional
    public BookCopyDto update(Long id, CreateBookCopyRequest request) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);

        BookCopy copy = bookCopyRepository.findBySchoolIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Book copy not found"));

        if (bookCopyRepository.existsBySchoolIdAndBarcodeAndIdNot(tenantId, request.getBarcode(), id)) {
            throw new com.edumanage.exception.ConflictException("Barcode already exists");
        }

        copy.setBarcode(request.getBarcode());
        copy.setConditionStatus(request.getConditionStatus() != null ? request.getConditionStatus() : copy.getConditionStatus());
        copy.setRemarks(request.getRemarks());
        if (request.getAcquiredDate() != null) {
            copy.setAcquiredDate(request.getAcquiredDate());
        }
        if (request.getStatus() != null) {
            copy.setStatus(request.getStatus());
        }

        bookCopyRepository.save(copy);
        auditLogService.log("BOOK_COPY_UPDATE", "BookCopy", copy.getId(),
                "Updated book copy: " + request.getBarcode(), null, null, null);
        return mapToDto(copy);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);

        BookCopy copy = bookCopyRepository.findBySchoolIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Book copy not found"));
        copy.setDeletedAt(java.time.LocalDateTime.now());
        bookCopyRepository.save(copy);
        auditLogService.log("BOOK_COPY_DELETE", "BookCopy", copy.getId(),
                "Deleted book copy: " + copy.getBarcode(), null, null, null);
    }

    private BookCopyDto mapToDto(BookCopy entity) {
        BookCopyDto dto = new BookCopyDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setBookId(entity.getBookId());
        if (entity.getBookId() != null) {
            bookRepository.findById(entity.getBookId()).ifPresent(b -> dto.setBookTitle(b.getTitle()));
        }
        dto.setBarcode(entity.getBarcode());
        dto.setStatus(entity.getStatus());
        dto.setConditionStatus(entity.getConditionStatus());
        dto.setRemarks(entity.getRemarks());
        dto.setAcquiredDate(entity.getAcquiredDate());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
