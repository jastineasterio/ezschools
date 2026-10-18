package com.edumanage.teacherprofiles.controller;

import com.edumanage.teacherprofiles.dto.CreateTeacherProfileRequest;
import com.edumanage.teacherprofiles.dto.TeacherProfileDto;
import com.edumanage.teacherprofiles.service.TeacherProfileService;
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
@RequestMapping("/school/teachers")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class TeacherProfileController {

    private final TeacherProfileService teacherProfileService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TeacherProfileDto>>> list(Pageable pageable) {
        var teachers = teacherProfileService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Teachers fetched", teachers, null, LocalDateTime.now(), "/api/school/teachers"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeacherProfileDto>> getById(@PathVariable Long id) {
        var teacher = teacherProfileService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Teacher fetched", teacher, null, LocalDateTime.now(), "/api/school/teachers/" + id));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<TeacherProfileDto>> getMyProfile() {
        var teacher = teacherProfileService.getMyProfile();
        return ResponseEntity.ok(new ApiResponse<>(true, "Teacher profile fetched", teacher, null, LocalDateTime.now(), "/api/school/teachers/me"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TeacherProfileDto>> create(@Valid @RequestBody CreateTeacherProfileRequest request) {
        var teacher = teacherProfileService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Teacher created", teacher, null, LocalDateTime.now(), "/api/school/teachers"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TeacherProfileDto>> update(@PathVariable Long id,
                                                                 @Valid @RequestBody CreateTeacherProfileRequest request) {
        var teacher = teacherProfileService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Teacher updated", teacher, null, LocalDateTime.now(), "/api/school/teachers/" + id));
    }
}