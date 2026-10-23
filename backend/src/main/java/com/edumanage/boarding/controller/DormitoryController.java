package com.edumanage.boarding.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.boarding.dto.CreateDormitoryRequest;
import com.edumanage.boarding.dto.DormitoryDto;
import com.edumanage.boarding.service.DormitoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/school/boarding/dormitories")
@PreAuthorize("hasAnyAuthority('ROLE_WARDEN', 'ROLE_MANAGER', 'ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class DormitoryController {

    private final DormitoryService dormitoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DormitoryDto>>> listDormitories(Pageable pageable) {
        var result = dormitoryService.listDormitories(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Dormitories fetched", result, null, LocalDateTime.now(), "/api/school/boarding/dormitories"));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<DormitoryDto>>> listActiveDormitories() {
        var result = dormitoryService.listActiveDormitories();
        return ResponseEntity.ok(new ApiResponse<>(true, "Active dormitories fetched", result, null, LocalDateTime.now(), "/api/school/boarding/dormitories/active"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DormitoryDto>> getDormitory(@PathVariable Long id) {
        var dormitory = dormitoryService.getDormitory(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Dormitory fetched", dormitory, null, LocalDateTime.now(), "/api/school/boarding/dormitories/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DormitoryDto>> createDormitory(@Valid @RequestBody CreateDormitoryRequest request) {
        var dormitory = dormitoryService.createDormitory(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Dormitory created", dormitory, null, LocalDateTime.now(), "/api/school/boarding/dormitories"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DormitoryDto>> updateDormitory(@PathVariable Long id,
                                                                    @Valid @RequestBody CreateDormitoryRequest request) {
        var dormitory = dormitoryService.updateDormitory(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Dormitory updated", dormitory, null, LocalDateTime.now(), "/api/school/boarding/dormitories/" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDormitory(@PathVariable Long id) {
        dormitoryService.deleteDormitory(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Dormitory deleted", null, null, LocalDateTime.now(), "/api/school/boarding/dormitories/" + id));
    }
}
