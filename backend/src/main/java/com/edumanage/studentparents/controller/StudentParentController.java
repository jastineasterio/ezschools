package com.edumanage.studentparents.controller;

import com.edumanage.studentparents.dto.CreateStudentParentRequest;
import com.edumanage.studentparents.dto.StudentParentDto;
import com.edumanage.studentparents.service.StudentParentService;
import com.edumanage.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/school/student-parents")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class StudentParentController {

    private final StudentParentService studentParentService;

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<StudentParentDto>>> listByStudent(@PathVariable Long studentId) {
        var relations = studentParentService.listByStudent(studentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Student-parent relations fetched", relations, null, LocalDateTime.now(), "/api/school/student-parents/student/" + studentId));
    }

    @GetMapping("/parent/{parentId}")
    public ResponseEntity<ApiResponse<List<StudentParentDto>>> listByParent(@PathVariable Long parentId) {
        var relations = studentParentService.listByParent(parentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Parent-student relations fetched", relations, null, LocalDateTime.now(), "/api/school/student-parents/parent/" + parentId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StudentParentDto>> create(@Valid @RequestBody CreateStudentParentRequest request) {
        var relation = studentParentService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Student-parent relation created", relation, null, LocalDateTime.now(), "/api/school/student-parents"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentParentDto>> update(@PathVariable Long id,
                                                                @Valid @RequestBody CreateStudentParentRequest request) {
        var relation = studentParentService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Student-parent relation updated", relation, null, LocalDateTime.now(), "/api/school/student-parents/" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        studentParentService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Student-parent relation deleted", null, null, LocalDateTime.now(), "/api/school/student-parents/" + id));
    }
}