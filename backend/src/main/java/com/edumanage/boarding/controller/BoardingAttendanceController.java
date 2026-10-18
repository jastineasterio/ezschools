package com.edumanage.boarding.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.boarding.dto.BoardingAttendanceDto;
import com.edumanage.boarding.dto.CreateBoardingAttendanceRequest;
import com.edumanage.boarding.service.BoardingAttendanceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/school/boarding/attendance")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class BoardingAttendanceController {

    private final BoardingAttendanceService boardingAttendanceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BoardingAttendanceDto>>> listAttendance(Pageable pageable) {
        var result = boardingAttendanceService.listAttendance(pageable).getContent();
        return ResponseEntity.ok(new ApiResponse<>(true, "Boarding attendance fetched", result, null, LocalDateTime.now(), "/api/school/boarding/attendance"));
    }

    @GetMapping("/dormitory/{dormitoryId}/date/{date}")
    public ResponseEntity<ApiResponse<List<BoardingAttendanceDto>>> listAttendanceByDormitoryAndDate(
            @PathVariable Long dormitoryId, @PathVariable LocalDate date) {
        var result = boardingAttendanceService.listAttendanceByDormitoryAndDate(dormitoryId, date);
        return ResponseEntity.ok(new ApiResponse<>(true, "Attendance fetched", result, null, LocalDateTime.now(), "/api/school/boarding/attendance/dormitory/" + dormitoryId + "/date/" + date));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BoardingAttendanceDto>> getAttendance(@PathVariable Long id) {
        var record = boardingAttendanceService.getAttendance(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Attendance fetched", record, null, LocalDateTime.now(), "/api/school/boarding/attendance/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BoardingAttendanceDto>> createAttendance(@Valid @RequestBody CreateBoardingAttendanceRequest request) {
        var record = boardingAttendanceService.createAttendance(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Attendance recorded", record, null, LocalDateTime.now(), "/api/school/boarding/attendance"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BoardingAttendanceDto>> updateAttendance(@PathVariable Long id,
                                                                               @Valid @RequestBody CreateBoardingAttendanceRequest request) {
        var record = boardingAttendanceService.updateAttendance(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Attendance updated", record, null, LocalDateTime.now(), "/api/school/boarding/attendance/" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAttendance(@PathVariable Long id) {
        boardingAttendanceService.deleteAttendance(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Attendance deleted", null, null, LocalDateTime.now(), "/api/school/boarding/attendance/" + id));
    }
}
