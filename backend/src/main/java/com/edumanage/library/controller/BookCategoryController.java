package com.edumanage.library.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.library.dto.BookCategoryDto;
import com.edumanage.library.dto.CreateBookCategoryRequest;
import com.edumanage.library.service.BookCategoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/school/library/categories")
@PreAuthorize("hasAnyAuthority('ROLE_LIBRARIAN', 'ROLE_MANAGER', 'ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class BookCategoryController {

    private final BookCategoryService bookCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookCategoryDto>>> list(Pageable pageable) {
        var categories = bookCategoryService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Book categories fetched", categories, null, LocalDateTime.now(), "/api/school/library/categories"));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<java.util.List<BookCategoryDto>>> listActive() {
        var categories = bookCategoryService.listActive();
        return ResponseEntity.ok(new ApiResponse<>(true, "Active book categories fetched", categories, null, LocalDateTime.now(), "/api/school/library/categories/active"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookCategoryDto>> get(@PathVariable Long id) {
        var category = bookCategoryService.get(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Book category fetched", category, null, LocalDateTime.now(), "/api/school/library/categories/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookCategoryDto>> create(@Valid @RequestBody CreateBookCategoryRequest request) {
        var category = bookCategoryService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Book category created", category, null, LocalDateTime.now(), "/api/school/library/categories"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BookCategoryDto>> update(@PathVariable Long id,
                                                               @Valid @RequestBody CreateBookCategoryRequest request) {
        var category = bookCategoryService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Book category updated", category, null, LocalDateTime.now(), "/api/school/library/categories/" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        bookCategoryService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Book category deleted", null, null, LocalDateTime.now(), "/api/school/library/categories/" + id));
    }
}
