package com.edumanage.library.service;

import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.library.dto.BookCategoryDto;
import com.edumanage.library.dto.CreateBookCategoryRequest;
import com.edumanage.library.entity.BookCategory;
import com.edumanage.library.repository.BookCategoryRepository;
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
public class BookCategoryService {

    private final BookCategoryRepository bookCategoryRepository;
    private final AuditLogService auditLogService;

    public BookCategoryService(BookCategoryRepository bookCategoryRepository, AuditLogService auditLogService) {
        this.bookCategoryRepository = bookCategoryRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<BookCategoryDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        Page<BookCategory> page = bookCategoryRepository.findBySchoolId(tenantId, pageable);
        return PageResponse.from(page.map(this::mapToDto));
    }

    public List<BookCategoryDto> listActive() {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        return bookCategoryRepository.findBySchoolIdAndStatus(tenantId, "ACTIVE").stream()
                .map(this::mapToDto)
                .toList();
    }

    public BookCategoryDto get(Long id) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        BookCategory category = bookCategoryRepository.findBySchoolIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Book category not found"));
        return mapToDto(category);
    }

    @Transactional
    public BookCategoryDto create(CreateBookCategoryRequest request) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);

        if (bookCategoryRepository.existsBySchoolIdAndNameIgnoreCase(tenantId, request.getName())) {
            throw new com.edumanage.exception.ConflictException("Category name already exists");
        }

        BookCategory category = new BookCategory();
        category.setSchoolId(tenantId);
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        bookCategoryRepository.save(category);
        auditLogService.log("BOOK_CATEGORY_CREATE", "BookCategory", category.getId(),
                "Created book category: " + request.getName(), null, null, null);
        return mapToDto(category);
    }

    @Transactional
    public BookCategoryDto update(Long id, CreateBookCategoryRequest request) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);

        BookCategory category = bookCategoryRepository.findBySchoolIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Book category not found"));

        if (bookCategoryRepository.existsBySchoolIdAndNameIgnoreCaseAndIdNot(tenantId, request.getName(), id)) {
            throw new com.edumanage.exception.ConflictException("Category name already exists");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }

        bookCategoryRepository.save(category);
        auditLogService.log("BOOK_CATEGORY_UPDATE", "BookCategory", category.getId(),
                "Updated book category: " + request.getName(), null, null, null);
        return mapToDto(category);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);

        BookCategory category = bookCategoryRepository.findBySchoolIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Book category not found"));

        category.setDeletedAt(java.time.LocalDateTime.now());
        bookCategoryRepository.save(category);
        auditLogService.log("BOOK_CATEGORY_DELETE", "BookCategory", category.getId(),
                "Deleted book category: " + category.getName(), null, null, null);
    }

    private BookCategoryDto mapToDto(BookCategory entity) {
        BookCategoryDto dto = new BookCategoryDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
