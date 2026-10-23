package com.edumanage.library.service;

import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.library.dto.BookCategoryDto;
import com.edumanage.library.dto.BookDto;
import com.edumanage.library.dto.CreateBookRequest;
import com.edumanage.library.entity.Book;
import com.edumanage.library.entity.BookCopy;
import com.edumanage.library.repository.BookCategoryRepository;
import com.edumanage.library.repository.BookCopyRepository;
import com.edumanage.library.repository.BookRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.security.AccessGuard;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import com.edumanage.library.util.LibraryFeatureGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final BookCopyRepository bookCopyRepository;
    private final AuditLogService auditLogService;
    private final AccessGuard accessGuard;

    public BookService(BookRepository bookRepository, BookCategoryRepository bookCategoryRepository,
                       BookCopyRepository bookCopyRepository, AuditLogService auditLogService, AccessGuard accessGuard) {
        this.bookRepository = bookRepository;
        this.bookCategoryRepository = bookCategoryRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.auditLogService = auditLogService;
        this.accessGuard = accessGuard;
    }

    public PageResponse<BookDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        accessGuard.assertLibraryAccess();
        Page<Book> page = bookRepository.findBySchoolId(tenantId, pageable);
        return PageResponse.from(page.map(this::mapToDto));
    }

    public BookDto get(Long id) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        accessGuard.assertLibraryAccess();
        Book book = bookRepository.findBySchoolIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        return mapToDto(book);
    }

    @Transactional
    public BookDto create(CreateBookRequest request) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        accessGuard.assertLibraryAccess();

        if (request.getIsbn() != null && bookRepository.existsBySchoolIdAndIsbn(tenantId, request.getIsbn())) {
            throw new com.edumanage.exception.ConflictException("ISBN already exists");
        }

        if (request.getCategoryId() != null) {
            bookCategoryRepository.findBySchoolIdAndId(tenantId, request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Book category not found"));
        }

        Book book = new Book();
        book.setSchoolId(tenantId);
        book.setCategoryId(request.getCategoryId());
        book.setIsbn(request.getIsbn());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setPublishedDate(request.getPublishedDate());
        book.setEdition(request.getEdition());
        book.setLanguage(request.getLanguage() != null ? request.getLanguage() : "ENGLISH");
        book.setPages(request.getPages());
        book.setShelfLocation(request.getShelfLocation());
        book.setTotalCopies(request.getTotalCopies() != null ? request.getTotalCopies() : 1);
        book.setAvailableCopies(request.getTotalCopies() != null ? request.getTotalCopies() : 1);
        book.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        bookRepository.save(book);
        auditLogService.log("BOOK_CREATE", "Book", book.getId(),
                "Created book: " + request.getTitle(), null, null, null);
        return mapToDto(book);
    }

    @Transactional
    public BookDto update(Long id, CreateBookRequest request) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        accessGuard.assertLibraryAccess();

        Book book = bookRepository.findBySchoolIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        if (request.getIsbn() != null && bookRepository.existsBySchoolIdAndIsbnAndIdNot(tenantId, request.getIsbn(), id)) {
            throw new com.edumanage.exception.ConflictException("ISBN already exists");
        }

        if (request.getCategoryId() != null) {
            bookCategoryRepository.findBySchoolIdAndId(tenantId, request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Book category not found"));
        }

        book.setCategoryId(request.getCategoryId());
        book.setIsbn(request.getIsbn());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setPublishedDate(request.getPublishedDate());
        book.setEdition(request.getEdition());
        if (request.getLanguage() != null) {
            book.setLanguage(request.getLanguage());
        }
        book.setPages(request.getPages());
        book.setShelfLocation(request.getShelfLocation());
        if (request.getTotalCopies() != null) {
            int borrowed = book.getTotalCopies() - book.getAvailableCopies();
            book.setTotalCopies(request.getTotalCopies());
            book.setAvailableCopies(Math.max(0, request.getTotalCopies() - borrowed));
        }
        if (request.getStatus() != null) {
            book.setStatus(request.getStatus());
        }

        bookRepository.save(book);
        auditLogService.log("BOOK_UPDATE", "Book", book.getId(),
                "Updated book: " + request.getTitle(), null, null, null);
        return mapToDto(book);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        accessGuard.assertLibraryAccess();

        Book book = bookRepository.findBySchoolIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        book.setDeletedAt(java.time.LocalDateTime.now());
        bookRepository.save(book);
        auditLogService.log("BOOK_DELETE", "Book", book.getId(),
                "Deleted book: " + book.getTitle(), null, null, null);
    }

    private BookDto mapToDto(Book entity) {
        BookDto dto = new BookDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setCategoryId(entity.getCategoryId());
        if (entity.getCategoryId() != null) {
            bookCategoryRepository.findById(entity.getCategoryId()).ifPresent(c -> dto.setCategoryName(c.getName()));
        }
        dto.setIsbn(entity.getIsbn());
        dto.setTitle(entity.getTitle());
        dto.setAuthor(entity.getAuthor());
        dto.setPublisher(entity.getPublisher());
        dto.setPublishedDate(entity.getPublishedDate());
        dto.setEdition(entity.getEdition());
        dto.setLanguage(entity.getLanguage());
        dto.setPages(entity.getPages());
        dto.setShelfLocation(entity.getShelfLocation());
        dto.setTotalCopies(entity.getTotalCopies());
        dto.setAvailableCopies(entity.getAvailableCopies());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
