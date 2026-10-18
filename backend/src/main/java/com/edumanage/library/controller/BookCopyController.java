package com.edumanage.library.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.library.dto.BookCopyDto;
import com.edumanage.library.dto.CreateBookCopyRequest;
import com.edumanage.library.service.BookCopyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/school/library/copies")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class BookCopyController {

    private final BookCopyService bookCopyService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookCopyDto>>> list(Pageable pageable) {
        var copies = bookCopyService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Book copies fetched", copies, null, LocalDateTime.now(), "/api/school/library/copies"));
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<ApiResponse<java.util.List<BookCopyDto>>> listByBook(@PathVariable Long bookId) {
        var copies = bookCopyService.listByBook(bookId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Book copies fetched", copies, null, LocalDateTime.now(), "/api/school/library/copies/book/" + bookId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookCopyDto>> get(@PathVariable Long id) {
        var copy = bookCopyService.get(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Book copy fetched", copy, null, LocalDateTime.now(), "/api/school/library/copies/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookCopyDto>> create(@Valid @RequestBody CreateBookCopyRequest request) {
        var copy = bookCopyService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Book copy created", copy, null, LocalDateTime.now(), "/api/school/library/copies"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BookCopyDto>> update(@PathVariable Long id,
                                                           @Valid @RequestBody CreateBookCopyRequest request) {
        var copy = bookCopyService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Book copy updated", copy, null, LocalDateTime.now(), "/api/school/library/copies/" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        bookCopyService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Book copy deleted", null, null, LocalDateTime.now(), "/api/school/library/copies/" + id));
    }
}
