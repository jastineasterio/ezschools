package com.edumanage.library.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.library.dto.CreateFineRequest;
import com.edumanage.library.dto.FineDto;
import com.edumanage.library.service.FineService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/school/library/fines")
@PreAuthorize("hasAnyAuthority('ROLE_LIBRARIAN', 'ROLE_MANAGER', 'ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FineDto>>> list(Pageable pageable) {
        var fines = fineService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Fines fetched", fines, null, LocalDateTime.now(), "/api/school/library/fines"));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<java.util.List<FineDto>>> listPending() {
        var fines = fineService.listPending();
        return ResponseEntity.ok(new ApiResponse<>(true, "Pending fines fetched", fines, null, LocalDateTime.now(), "/api/school/library/fines/pending"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FineDto>> get(@PathVariable Long id) {
        var fine = fineService.get(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Fine fetched", fine, null, LocalDateTime.now(), "/api/school/library/fines/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FineDto>> create(@Valid @RequestBody CreateFineRequest request) {
        var fine = fineService.createFineFromRequest(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Fine created", fine, null, LocalDateTime.now(), "/api/school/library/fines"));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<FineDto>> pay(@PathVariable Long id, @RequestParam BigDecimal amount) {
        var fine = fineService.payFine(id, amount);
        return ResponseEntity.ok(new ApiResponse<>(true, "Fine payment recorded", fine, null, LocalDateTime.now(), "/api/school/library/fines/" + id + "/pay"));
    }

    @PostMapping("/{id}/waive")
    public ResponseEntity<ApiResponse<FineDto>> waive(@PathVariable Long id) {
        var fine = fineService.waiveFine(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Fine waived", fine, null, LocalDateTime.now(), "/api/school/library/fines/" + id + "/waive"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        fineService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Fine deleted", null, null, LocalDateTime.now(), "/api/school/library/fines/" + id));
    }
}
