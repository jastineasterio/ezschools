package com.edumanage.library.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.library.dto.BorrowingDto;
import com.edumanage.library.dto.CreateBorrowingRequest;
import com.edumanage.library.service.BorrowingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/school/library/borrowings")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class BorrowingController {

    private final BorrowingService borrowingService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BorrowingDto>>> list(Pageable pageable) {
        var borrowings = borrowingService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Borrowings fetched", borrowings, null, LocalDateTime.now(), "/api/school/library/borrowings"));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponse<java.util.List<BorrowingDto>>> listByMember(@PathVariable Long memberId) {
        var borrowings = borrowingService.listByMember(memberId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Borrowings fetched", borrowings, null, LocalDateTime.now(), "/api/school/library/borrowings/member/" + memberId));
    }

    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<java.util.List<BorrowingDto>>> listOverdue() {
        var borrowings = borrowingService.listOverdue();
        return ResponseEntity.ok(new ApiResponse<>(true, "Overdue borrowings fetched", borrowings, null, LocalDateTime.now(), "/api/school/library/borrowings/overdue"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BorrowingDto>> get(@PathVariable Long id) {
        var borrowing = borrowingService.get(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Borrowing fetched", borrowing, null, LocalDateTime.now(), "/api/school/library/borrowings/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BorrowingDto>> create(@Valid @RequestBody CreateBorrowingRequest request) {
        var borrowing = borrowingService.borrow(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Book borrowed successfully", borrowing, null, LocalDateTime.now(), "/api/school/library/borrowings"));
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<ApiResponse<BorrowingDto>> returnBook(@PathVariable Long id) {
        var borrowing = borrowingService.returnBook(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Book returned successfully", borrowing, null, LocalDateTime.now(), "/api/school/library/borrowings/" + id + "/return"));
    }

    @PostMapping("/detect-overdue")
    public ResponseEntity<ApiResponse<Void>> detectOverdue() {
        borrowingService.detectOverdue();
        return ResponseEntity.ok(new ApiResponse<>(true, "Overdue detection completed", null, null, LocalDateTime.now(), "/api/school/library/borrowings/detect-overdue"));
    }
}
