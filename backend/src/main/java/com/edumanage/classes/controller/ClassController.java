package com.edumanage.classes.controller;

import com.edumanage.classes.dto.ClassDto;
import com.edumanage.classes.dto.CreateClassRequest;
import com.edumanage.classes.service.ClassService;
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
@RequestMapping("/school/classes")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ClassDto>>> list(
            Pageable pageable, @RequestParam(required = false) String search) {
        var classes = classService.list(pageable, search);
        return ResponseEntity.ok(new ApiResponse<>(true, "Classes fetched", classes, null, LocalDateTime.now(), "/api/school/classes"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassDto>> getById(@PathVariable Long id) {
        var clazz = classService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Class fetched", clazz, null, LocalDateTime.now(), "/api/school/classes/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClassDto>> create(@Valid @RequestBody CreateClassRequest request) {
        var clazz = classService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Class created", clazz, null, LocalDateTime.now(), "/api/school/classes"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassDto>> update(@PathVariable Long id,
                                                        @Valid @RequestBody CreateClassRequest request) {
        var clazz = classService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Class updated", clazz, null, LocalDateTime.now(), "/api/school/classes/" + id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ClassDto>> updateStatus(@PathVariable Long id,
                                                              @RequestParam String status) {
        var clazz = classService.updateStatus(id, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Class status updated", clazz, null, LocalDateTime.now(), "/api/school/classes/" + id + "/status"));
    }
}