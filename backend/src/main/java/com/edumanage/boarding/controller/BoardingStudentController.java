package com.edumanage.boarding.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.boarding.dto.BoardingStudentDto;
import com.edumanage.boarding.dto.CreateBoardingStudentRequest;
import com.edumanage.boarding.service.BoardingStudentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/school/boarding/students")
@PreAuthorize("hasAnyAuthority('ROLE_WARDEN', 'ROLE_MANAGER', 'ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class BoardingStudentController {

    private final BoardingStudentService boardingStudentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BoardingStudentDto>>> listBoardingStudents(Pageable pageable) {
        var result = boardingStudentService.listBoardingStudents(pageable).getContent();
        return ResponseEntity.ok(new ApiResponse<>(true, "Boarding students fetched", result, null, LocalDateTime.now(), "/api/school/boarding/students"));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<BoardingStudentDto>>> listActiveBoardingStudents() {
        var result = boardingStudentService.listActiveBoardingStudents();
        return ResponseEntity.ok(new ApiResponse<>(true, "Active boarding students fetched", result, null, LocalDateTime.now(), "/api/school/boarding/students/active"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BoardingStudentDto>> getBoardingStudent(@PathVariable Long id) {
        var record = boardingStudentService.getBoardingStudent(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Boarding student fetched", record, null, LocalDateTime.now(), "/api/school/boarding/students/" + id));
    }

    @PostMapping("/allocate")
    public ResponseEntity<ApiResponse<BoardingStudentDto>> allocateStudent(@Valid @RequestBody CreateBoardingStudentRequest request) {
        var record = boardingStudentService.allocateStudent(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Student allocated", record, null, LocalDateTime.now(), "/api/school/boarding/students/allocate"));
    }

    @PutMapping("/{id}/transfer")
    public ResponseEntity<ApiResponse<BoardingStudentDto>> transferStudent(@PathVariable Long id,
                                                                           @Valid @RequestBody CreateBoardingStudentRequest request) {
        var record = boardingStudentService.transferStudent(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Student transferred", record, null, LocalDateTime.now(), "/api/school/boarding/students/" + id + "/transfer"));
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<ApiResponse<BoardingStudentDto>> releaseStudent(@PathVariable Long id) {
        var record = boardingStudentService.releaseStudent(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Student released", record, null, LocalDateTime.now(), "/api/school/boarding/students/" + id + "/release"));
    }
}
