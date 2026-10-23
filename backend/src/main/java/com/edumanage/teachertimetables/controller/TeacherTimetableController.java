package com.edumanage.teachertimetables.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.teachertimetables.dto.CreateTeacherTimetableRequest;
import com.edumanage.teachertimetables.dto.TeacherTimetableDto;
import com.edumanage.teachertimetables.service.TeacherTimetableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/school/timetables")
@PreAuthorize("hasAuthority('ROLE_MANAGER')")
@RequiredArgsConstructor
public class TeacherTimetableController {
    private final TeacherTimetableService timetableService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TeacherTimetableDto>>> list() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Timetable fetched",
                timetableService.listForSchool(), null, LocalDateTime.now(), "/api/school/timetables"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TeacherTimetableDto>> create(
            @Valid @RequestBody CreateTeacherTimetableRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Timetable entry created",
                timetableService.create(request), null, LocalDateTime.now(), "/api/school/timetables"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        timetableService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Timetable entry deleted", null, null,
                LocalDateTime.now(), "/api/school/timetables/" + id));
    }
}
