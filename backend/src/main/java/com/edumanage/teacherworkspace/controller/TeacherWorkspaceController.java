package com.edumanage.teacherworkspace.controller;

import com.edumanage.attendance.dto.AttendanceDto;
import com.edumanage.common.dto.ApiResponse;
import com.edumanage.teacherworkspace.dto.TeacherAttendanceStudentDto;
import com.edumanage.teacherworkspace.dto.TeacherAttendanceSubmissionDto;
import com.edumanage.teacherworkspace.dto.TeacherDashboardDto;
import com.edumanage.teacherworkspace.dto.TeacherExamStudentDto;
import com.edumanage.teacherworkspace.dto.TeacherResultSubmissionDto;
import com.edumanage.teacherworkspace.service.TeacherWorkspaceService;
import com.edumanage.results.dto.ResultDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/school/teacher-workspace")
@PreAuthorize("hasAuthority('ROLE_TEACHER')")
@RequiredArgsConstructor
public class TeacherWorkspaceController {
    private final TeacherWorkspaceService teacherWorkspaceService;

    @GetMapping
    public ResponseEntity<ApiResponse<TeacherDashboardDto>> dashboard() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Teacher dashboard fetched",
                teacherWorkspaceService.dashboard(), null, LocalDateTime.now(),
                "/api/school/teacher-workspace"));
    }

    @GetMapping("/assignments/{assignmentId}/attendance")
    public ResponseEntity<ApiResponse<List<TeacherAttendanceStudentDto>>> attendanceRoster(
            @PathVariable Long assignmentId, @RequestParam LocalDate date) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Attendance roster fetched",
                teacherWorkspaceService.attendanceRoster(assignmentId, date), null, LocalDateTime.now(),
                "/api/school/teacher-workspace/assignments/" + assignmentId + "/attendance"));
    }

    @PostMapping("/assignments/{assignmentId}/attendance")
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> recordAttendance(
            @PathVariable Long assignmentId,
            @RequestParam LocalDate date,
            @NotEmpty @Valid @RequestBody List<@Valid TeacherAttendanceSubmissionDto> submissions) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Attendance recorded",
                teacherWorkspaceService.recordAttendance(assignmentId, date, submissions), null,
                LocalDateTime.now(),
                "/api/school/teacher-workspace/assignments/" + assignmentId + "/attendance"));
    }

    @GetMapping("/examination-subjects/{examinationSubjectId}/roster")
    public ResponseEntity<ApiResponse<List<TeacherExamStudentDto>>> examinationRoster(
            @PathVariable Long examinationSubjectId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Examination roster fetched",
                teacherWorkspaceService.examinationRoster(examinationSubjectId), null, LocalDateTime.now(),
                "/api/school/teacher-workspace/examination-subjects/" + examinationSubjectId + "/roster"));
    }

    @PostMapping("/examination-subjects/{examinationSubjectId}/results")
    public ResponseEntity<ApiResponse<List<ResultDto>>> recordResults(
            @PathVariable Long examinationSubjectId,
            @NotEmpty @Valid @RequestBody List<@Valid TeacherResultSubmissionDto> submissions) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Examination results saved",
                teacherWorkspaceService.recordExaminationResults(examinationSubjectId, submissions), null,
                LocalDateTime.now(),
                "/api/school/teacher-workspace/examination-subjects/" + examinationSubjectId + "/results"));
    }

    @PostMapping("/results/{resultId}/submit")
    public ResponseEntity<ApiResponse<ResultDto>> submitResult(@PathVariable Long resultId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Result submitted for approval",
                teacherWorkspaceService.submitResult(resultId), null, LocalDateTime.now(),
                "/api/school/teacher-workspace/results/" + resultId + "/submit"));
    }
}
