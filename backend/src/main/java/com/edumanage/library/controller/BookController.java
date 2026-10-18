package com.edumanage.library.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.library.dto.BookDto;
import com.edumanage.library.dto.CreateBookRequest;
import com.edumanage.library.service.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/school/library/books")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookDto>>> list(Pageable pageable) {
        var books = bookService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Books fetched", books, null, LocalDateTime.now(), "/api/school/library/books"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookDto>> get(@PathVariable Long id) {
        var book = bookService.get(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Book fetched", book, null, LocalDateTime.now(), "/api/school/library/books/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookDto>> create(@Valid @RequestBody CreateBookRequest request) {
        var book = bookService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Book created", book, null, LocalDateTime.now(), "/api/school/library/books"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BookDto>> update(@PathVariable Long id,
                                                       @Valid @RequestBody CreateBookRequest request) {
        var book = bookService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Book updated", book, null, LocalDateTime.now(), "/api/school/library/books/" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Book deleted", null, null, LocalDateTime.now(), "/api/school/library/books/" + id));
    }
}
