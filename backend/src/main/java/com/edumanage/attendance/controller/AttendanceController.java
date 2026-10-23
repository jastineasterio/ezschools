package com.edumanage.attendance.controller;

import com.edumanage.attendance.dto.CreateAttendanceRequest;
import com.edumanage.attendance.dto.AttendanceDto;
import com.edumanage.attendance.service.AttendanceService;
import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
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
@RequestMapping("/school/attendance")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_HOD')")
    public ResponseEntity<ApiResponse<PageResponse<AttendanceDto>>> list(
            Pageable pageable,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) LocalDate date) {
        var attendance = attendanceService.list(pageable, classId, date);
        return ResponseEntity.ok(new ApiResponse<>(true, "Attendance fetched", attendance, null, LocalDateTime.now(), "/api/school/attendance"));
    }

    @GetMapping("/class/{classId}/date/{date}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_HOD')")
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> listByClassAndDate(@PathVariable Long classId,
                                                                              @PathVariable LocalDate date) {
        var attendance = attendanceService.listByClassAndDate(classId, date);
        return ResponseEntity.ok(new ApiResponse<>(true, "Attendance fetched by class and date", attendance, null, LocalDateTime.now(), "/api/school/attendance/class/" + classId + "/date/" + date));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> listByStudent(@PathVariable Long studentId) {
        var attendance = attendanceService.listByStudent(studentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Attendance fetched by student", attendance, null, LocalDateTime.now(), "/api/school/attendance/student/" + studentId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<AttendanceDto>> create(@Valid @RequestBody CreateAttendanceRequest request) {
        var attendance = attendanceService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Attendance created", attendance, null, LocalDateTime.now(), "/api/school/attendance"));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> bulkCreate(@Valid @RequestBody List<CreateAttendanceRequest> requests) {
        var attendance = attendanceService.bulkCreate(requests);
        return ResponseEntity.ok(new ApiResponse<>(true, "Attendance bulk created", attendance, null, LocalDateTime.now(), "/api/school/attendance/bulk"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<AttendanceDto>> update(@PathVariable Long id,
                                                             @Valid @RequestBody CreateAttendanceRequest request) {
        var attendance = attendanceService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Attendance updated", attendance, null, LocalDateTime.now(), "/api/school/attendance/" + id));
    }
}