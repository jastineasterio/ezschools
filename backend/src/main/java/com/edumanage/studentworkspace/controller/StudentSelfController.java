package com.edumanage.studentworkspace.controller;

import com.edumanage.attendance.service.AttendanceService;
import com.edumanage.common.dto.ApiResponse;
import com.edumanage.enrollments.service.StudentEnrollmentService;
import com.edumanage.fees.service.FeeAssignmentService;
import com.edumanage.fees.service.PaymentService;
import com.edumanage.results.service.ResultService;
import com.edumanage.security.CurrentUserService;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.teachertimetables.repository.TeacherTimetableRepository;
import com.edumanage.teachertimetables.entity.TeacherTimetable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/school/student")
@PreAuthorize("hasAuthority('ROLE_STUDENT')")
@RequiredArgsConstructor
public class StudentSelfController {

    private final CurrentUserService currentUserService;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherTimetableRepository teacherTimetableRepository;
    private final StudentEnrollmentService enrollmentService;
    private final AttendanceService attendanceService;
    private final ResultService resultService;
    private final FeeAssignmentService feeAssignmentService;
    private final PaymentService paymentService;

    private StudentProfile resolveStudent() {
        var user = currentUserService.requireCurrentUser();
        return studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Student profile not found"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<StudentProfile>> me() {
        var sp = resolveStudent();
        return ResponseEntity.ok(new ApiResponse<>(true, "Student profile", sp, null, LocalDateTime.now(), "/api/school/student/me"));
    }

    @GetMapping("/enrollments")
    public ResponseEntity<ApiResponse<List<?>>> enrollments() {
        var sp = resolveStudent();
        var list = enrollmentService.listByStudent(sp.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Enrollments", list, null, LocalDateTime.now(), "/api/school/student/enrollments"));
    }

    @GetMapping("/attendance")
    public ResponseEntity<ApiResponse<List<?>>> attendance() {
        var sp = resolveStudent();
        var list = attendanceService.listByStudent(sp.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Attendance", list, null, LocalDateTime.now(), "/api/school/student/attendance"));
    }

    @GetMapping("/results")
    public ResponseEntity<ApiResponse<?>> results(Pageable pageable) {
        var sp = resolveStudent();
        var page = resultService.listByStudent(sp.getId(), pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Results", page, null, LocalDateTime.now(), "/api/school/student/results"));
    }

    @GetMapping("/fees/assignments")
    public ResponseEntity<ApiResponse<List<?>>> feeAssignments() {
        var sp = resolveStudent();
        var list = feeAssignmentService.listByStudent(sp.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Fee assignments", list, null, LocalDateTime.now(), "/api/school/student/fees/assignments"));
    }

    @GetMapping("/fees/payments")
    public ResponseEntity<ApiResponse<List<?>>> payments() {
        var sp = resolveStudent();
        var list = paymentService.listByStudent(sp.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Payments", list, null, LocalDateTime.now(), "/api/school/student/fees/payments"));
    }

    @GetMapping("/timetable")
    public ResponseEntity<ApiResponse<List<TeacherTimetable>>> timetable() {
        var sp = resolveStudent();
        var enrollments = enrollmentService.listByStudent(sp.getId());
        var active = enrollments.stream().filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatus())).findFirst().orElse(null);
        List<TeacherTimetable> all = teacherTimetableRepository.findBySchoolIdAndDeletedAtIsNullOrderByDayOfWeekAscStartTimeAsc(sp.getSchoolId());
        if (active != null) {
            all = all.stream().filter(t -> {
                var ta = t.getTeacherAssignment();
                if (ta == null) return false;
                if (active.getClassId() != null && !active.getClassId().equals(ta.getClazz() != null ? ta.getClazz().getId() : null)) return false;
                if (active.getStreamId() != null && ta.getStream() != null && !active.getStreamId().equals(ta.getStream().getId())) return false;
                return true;
            }).toList();
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Timetable", all, null, LocalDateTime.now(), "/api/school/student/timetable"));
    }
}
