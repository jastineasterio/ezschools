package com.edumanage.departments.controller;

import com.edumanage.departments.dto.CreateDepartmentRequest;
import com.edumanage.departments.dto.DepartmentDto;
import com.edumanage.departments.service.DepartmentService;
import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/school/departments")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DepartmentDto>>> list(Pageable pageable) {
        var departments = departmentService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Departments fetched", departments, null, LocalDateTime.now(), "/api/school/departments"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentDto>> getById(@PathVariable Long id) {
        var department = departmentService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department fetched", department, null, LocalDateTime.now(), "/api/school/departments/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentDto>> create(@Valid @RequestBody CreateDepartmentRequest request) {
        var department = departmentService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department created", department, null, LocalDateTime.now(), "/api/school/departments"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentDto>> update(@PathVariable Long id,
                                                             @Valid @RequestBody CreateDepartmentRequest request) {
        var department = departmentService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department updated", department, null, LocalDateTime.now(), "/api/school/departments/" + id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DepartmentDto>> updateStatus(@PathVariable Long id,
                                                                   @RequestParam String status) {
        var department = departmentService.updateStatus(id, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department status updated", department, null, LocalDateTime.now(), "/api/school/departments/" + id + "/status"));
    }
}