package com.edumanage.teacherworkspace.service;

import com.edumanage.attendance.dto.AttendanceDto;
import com.edumanage.attendance.dto.CreateAttendanceRequest;
import com.edumanage.attendance.repository.AttendanceRepository;
import com.edumanage.attendance.service.AttendanceService;
import com.edumanage.enrollments.repository.StudentEnrollmentRepository;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.exception.ConflictException;
import com.edumanage.results.dto.CreateResultRequest;
import com.edumanage.results.dto.ResultDto;
import com.edumanage.results.entity.Result;
import com.edumanage.results.repository.ResultRepository;
import com.edumanage.results.service.ResultService;
import com.edumanage.examinations.repository.ExaminationRepository;
import com.edumanage.examinationsubjects.repository.ExaminationSubjectRepository;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.security.CurrentUserService;
import com.edumanage.streamsubjects.repository.StreamSubjectAssignmentRepository;
import com.edumanage.teacherassignments.dto.TeacherAssignmentDto;
import com.edumanage.teacherassignments.entity.TeacherAssignment;
import com.edumanage.teacherassignments.repository.TeacherAssignmentRepository;
import com.edumanage.teachertimetables.dto.TeacherTimetableDto;
import com.edumanage.teachertimetables.entity.TeacherTimetable;
import com.edumanage.teachertimetables.repository.TeacherTimetableRepository;
import com.edumanage.teacherworkspace.dto.TeacherAttendanceStudentDto;
import com.edumanage.teacherworkspace.dto.TeacherAttendanceSubmissionDto;
import com.edumanage.teacherworkspace.dto.TeacherDashboardDto;
import com.edumanage.teacherworkspace.dto.TeacherExamStudentDto;
import com.edumanage.teacherworkspace.dto.TeacherResultSubmissionDto;
import com.edumanage.teacherworkspace.dto.TeacherUpcomingExaminationDto;
import com.edumanage.tenant.TenantContext;
import com.edumanage.users.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TeacherWorkspaceService {
    private final CurrentUserService currentUserService;
    private final TeacherAssignmentRepository assignmentRepository;
    private final com.edumanage.teacherassignments.service.TeacherAssignmentService assignmentService;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StreamSubjectAssignmentRepository streamSubjectAssignmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceService attendanceService;
    private final TeacherTimetableRepository timetableRepository;
    private final ExaminationSubjectRepository examinationSubjectRepository;
    private final ExaminationRepository examinationRepository;
    private final ResultRepository resultRepository;
    private final ResultService resultService;
    private final SchoolRepository schoolRepository;

    public TeacherWorkspaceService(CurrentUserService currentUserService,
                                   TeacherAssignmentRepository assignmentRepository,
                                   com.edumanage.teacherassignments.service.TeacherAssignmentService assignmentService,
                                   StudentEnrollmentRepository enrollmentRepository,
                                   StreamSubjectAssignmentRepository streamSubjectAssignmentRepository,
                                   AttendanceRepository attendanceRepository,
                                   AttendanceService attendanceService,
                                   TeacherTimetableRepository timetableRepository,
                                   ExaminationSubjectRepository examinationSubjectRepository,
                                   ExaminationRepository examinationRepository,
                                   ResultRepository resultRepository,
                                   ResultService resultService,
                                   SchoolRepository schoolRepository) {
        this.currentUserService = currentUserService;
        this.assignmentRepository = assignmentRepository;
        this.assignmentService = assignmentService;
        this.enrollmentRepository = enrollmentRepository;
        this.streamSubjectAssignmentRepository = streamSubjectAssignmentRepository;
        this.attendanceRepository = attendanceRepository;
        this.attendanceService = attendanceService;
        this.timetableRepository = timetableRepository;
        this.examinationSubjectRepository = examinationSubjectRepository;
        this.examinationRepository = examinationRepository;
        this.resultRepository = resultRepository;
        this.resultService = resultService;
        this.schoolRepository = schoolRepository;
    }

    @Transactional(readOnly = true)
    public TeacherDashboardDto dashboard() {
        User teacher = requireTeacher();
        Long schoolId = requireSchool(teacher);
        var school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        ZoneId zoneId = ZoneId.of(school.getTimeZone());
        LocalDate today = LocalDate.now(zoneId);

        List<TeacherAssignmentDto> assignments = assignmentService.listByTeacher(teacher.getId());
        if (!assignments.isEmpty()) {
            List<Long> classIds = assignments.stream().map(TeacherAssignmentDto::getClassId).distinct().toList();
            List<Long> yearIds = assignments.stream().map(TeacherAssignmentDto::getAcademicYearId).distinct().toList();
            Map<EnrollmentScope, Long> studentCounts = new HashMap<>();
            enrollmentRepository.countEnrolledByClassAndStream(schoolId, classIds, yearIds)
                    .forEach(count -> studentCounts.put(
                            new EnrollmentScope(count.getClassId(), count.getStreamId(), count.getAcademicYearId()),
                            count.getStudentCount()));
            assignments.forEach(assignment -> {
                long count = assignment.getStreamId() == null
                        ? studentCounts.entrySet().stream()
                                .filter(entry -> entry.getKey().classId().equals(assignment.getClassId())
                                        && entry.getKey().academicYearId().equals(assignment.getAcademicYearId()))
                                .mapToLong(Map.Entry::getValue).sum()
                        : studentCounts.getOrDefault(new EnrollmentScope(assignment.getClassId(),
                                assignment.getStreamId(), assignment.getAcademicYearId()), 0L);
                assignment.setStudentCount(count);
            });
        }
        List<Long> assignmentIds = assignments.stream().map(TeacherAssignmentDto::getId).toList();
        List<TeacherTimetableDto> lessons = assignmentIds.isEmpty() ? List.of()
                : timetableRepository.findBySchoolIdAndTeacherAssignmentIdInAndDayOfWeekAndDeletedAtIsNullOrderByStartTime(
                        schoolId, assignmentIds, today.getDayOfWeek().getValue())
                .stream().map(this::toTimetableDto).toList();
        List<TeacherUpcomingExaminationDto> examinations = examinationSubjectRepository.findUpcomingForTeacher(
                teacher.getId(), schoolId, today);

        return new TeacherDashboardDto(today, zoneId.getId(), assignments, lessons, examinations);
    }

    @Transactional(readOnly = true)
    public List<TeacherAttendanceStudentDto> attendanceRoster(Long assignmentId, LocalDate date) {
        TeacherAssignment assignment = requireOwnedAssignment(assignmentId);
        Long schoolId = requireSchool(currentUserService.requireCurrentUser());
        Set<Long> configuredStreamIds = assignment.getStreamId() == null
                ? streamSubjectAssignmentRepository
                        .findBySchoolIdAndClassIdAndAcademicYearIdAndSubjectId(
                                schoolId, assignment.getClassId(), assignment.getAcademicYearId(),
                                assignment.getSubjectId())
                        .stream().map(com.edumanage.streamsubjects.entity.StreamSubjectAssignment::getStreamId)
                        .collect(java.util.stream.Collectors.toSet())
                : Set.of(assignment.getStreamId());
        var enrollments = enrollmentRepository.findBySchoolIdAndClassIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(
                        schoolId, assignment.getClassId(), assignment.getAcademicYearId(), "ENROLLED")
                .stream()
                .filter(enrollment -> enrollment.getStreamId() == null
                        || configuredStreamIds.contains(enrollment.getStreamId()))
                .toList();
        Map<Long, com.edumanage.attendance.entity.Attendance> existing = new HashMap<>();
        attendanceRepository.findBySchoolIdAndClassIdAndAcademicYearIdAndAttendanceDateAndDeletedAtIsNull(
                        schoolId, assignment.getClassId(), assignment.getAcademicYearId(), date)
                .stream()
                .filter(record -> assignment.getSubjectId().equals(record.getSubjectId()))
                .filter(record -> assignment.getStreamId() == null
                        || assignment.getStreamId().equals(record.getStreamId()))
                .forEach(record -> existing.put(record.getStudentId(), record));

        return enrollments.stream().map(enrollment -> {
            var student = enrollment.getStudent();
            var record = existing.get(enrollment.getStudentId());
            return new TeacherAttendanceStudentDto(
                    enrollment.getStudentId(),
                    student.getFirstName() + " " + student.getLastName(),
                    student.getAdmissionNumber(),
                    enrollment.getStreamId(),
                    enrollment.getStream() == null ? null : enrollment.getStream().getName(),
                    record == null ? null : record.getStatus(),
                    record == null ? null : record.getRemarks());
        }).toList();
    }

    @Transactional
    public List<AttendanceDto> recordAttendance(Long assignmentId, LocalDate date,
                                                List<TeacherAttendanceSubmissionDto> submissions) {
        TeacherAssignment assignment = requireOwnedAssignment(assignmentId);
        User teacher = currentUserService.requireCurrentUser();
        Map<Long, TeacherAttendanceStudentDto> roster = new HashMap<>();
        attendanceRoster(assignmentId, date)
                .forEach(student -> roster.put(student.getStudentId(), student));
        Set<Long> studentIds = new HashSet<>();
        List<AttendanceDto> created = new ArrayList<>();

        for (TeacherAttendanceSubmissionDto submission : submissions) {
            if (!studentIds.add(submission.getStudentId())) {
                throw new com.edumanage.exception.BadRequestException(
                        "A student may only appear once in an attendance submission");
            }
            var student = roster.get(submission.getStudentId());
            if (student == null) {
                throw new ForbiddenException("Student is not enrolled in this assigned class and academic year");
            }
            if (student.getStatus() != null) {
                throw new ConflictException("Attendance has already been recorded for this student");
            }
            if (assignment.getStreamId() != null
                    && !assignment.getStreamId().equals(student.getStreamId())) {
                throw new ForbiddenException("Student is outside the assigned stream");
            }

            CreateAttendanceRequest request = new CreateAttendanceRequest();
            request.setStudentId(submission.getStudentId());
            request.setClassId(assignment.getClassId());
            request.setStreamId(student.getStreamId());
            request.setAcademicYearId(assignment.getAcademicYearId());
            request.setSubjectId(assignment.getSubjectId());
            request.setTeacherId(teacher.getId());
            request.setAttendanceDate(date);
            request.setStatus(submission.getStatus());
            request.setRemarks(submission.getRemarks());
            created.add(attendanceService.create(request));
        }
        return created;
    }

    @Transactional(readOnly = true)
    public List<TeacherExamStudentDto> examinationRoster(Long examinationSubjectId) {
        var examSubject = requireOwnedExaminationSubject(examinationSubjectId);
        User teacher = currentUserService.requireCurrentUser();
        Long schoolId = requireSchool(teacher);
        var examination = examinationRepository.findById(examSubject.getExaminationId())
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found"));
        List<Result> results = resultRepository.findByExaminationSubjectIdAndDeletedAtIsNull(examSubject.getId());
        Map<Long, Result> visibleResults = new HashMap<>();
        results.stream()
                .filter(result -> teacher.getId().equals(result.getTeacherId())
                        || "PUBLISHED".equals(result.getStatus()))
                .forEach(result -> visibleResults.put(result.getStudentId(), result));

        return enrollmentRepository.findBySchoolIdAndClassIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(
                        schoolId, examSubject.getClassId(), examination.getAcademicYearId(), "ENROLLED")
                .stream()
                .filter(enrollment -> examSubject.getStreamId() == null
                        || examSubject.getStreamId().equals(enrollment.getStreamId()))
                .map(enrollment -> {
                    var result = visibleResults.get(enrollment.getStudentId());
                    return new TeacherExamStudentDto(
                            enrollment.getStudentId(),
                            enrollment.getStudent().getFirstName() + " " + enrollment.getStudent().getLastName(),
                            enrollment.getStudent().getAdmissionNumber(),
                            result == null ? null : result.getId(),
                            result == null ? null : result.getMarksObtained(),
                            result == null ? null : result.getStatus(),
                            result != null && teacher.getId().equals(result.getTeacherId())
                                    && "DRAFT".equals(result.getStatus()));
                }).toList();
    }

    @Transactional
    public List<ResultDto> recordExaminationResults(Long examinationSubjectId,
                                                    List<TeacherResultSubmissionDto> submissions) {
        var examSubject = requireOwnedExaminationSubject(examinationSubjectId);
        User teacher = currentUserService.requireCurrentUser();
        Set<Long> eligibleStudentIds = examinationRoster(examinationSubjectId).stream()
                .map(TeacherExamStudentDto::getStudentId)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, Result> existingResults = new HashMap<>();
        resultRepository.findByExaminationSubjectIdAndDeletedAtIsNull(examSubject.getId())
                .forEach(result -> existingResults.put(result.getStudentId(), result));
        Set<Long> seenStudents = new HashSet<>();
        List<ResultDto> saved = new ArrayList<>();

        for (TeacherResultSubmissionDto submission : submissions) {
            if (!seenStudents.add(submission.getStudentId())) {
                throw new com.edumanage.exception.BadRequestException(
                        "A student may only appear once in a marks submission");
            }
            if (!eligibleStudentIds.contains(submission.getStudentId())) {
                throw new ForbiddenException("Student is outside the assigned examination class");
            }

            CreateResultRequest request = new CreateResultRequest(
                    submission.getStudentId(), examSubject.getId(),
                    submission.getMarksObtained(), submission.getRemarks());
            Result existing = existingResults.get(submission.getStudentId());
            if (existing == null) {
                saved.add(resultService.create(request));
            } else if (teacher.getId().equals(existing.getTeacherId())
                    && "DRAFT".equals(existing.getStatus())) {
                saved.add(resultService.update(existing.getId(), request));
            } else {
                throw new ConflictException("A result already exists and is no longer editable");
            }
        }
        return saved;
    }

    @Transactional
    public ResultDto submitResult(Long resultId) {
        return resultService.submit(resultId);
    }

    private TeacherAssignment requireOwnedAssignment(Long assignmentId) {
        User teacher = requireTeacher();
        Long schoolId = requireSchool(teacher);
        return assignmentRepository.findById(assignmentId)
                .filter(assignment -> !assignment.isDeleted())
                .filter(assignment -> teacher.getId().equals(assignment.getTeacherId())
                        && schoolId.equals(assignment.getSchoolId()))
                .orElseThrow(() -> new ForbiddenException("Assignment is not available to this teacher"));
    }

    private com.edumanage.examinationsubjects.entity.ExaminationSubject requireOwnedExaminationSubject(
            Long examinationSubjectId) {
        User teacher = requireTeacher();
        Long schoolId = requireSchool(teacher);
        var examSubject = examinationSubjectRepository.findById(examinationSubjectId)
                .filter(value -> !value.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Examination subject not found"));
        var examination = examinationRepository.findById(examSubject.getExaminationId())
                .filter(value -> !value.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found"));
        if (!schoolId.equals(examination.getSchoolId())
                || "DRAFT".equalsIgnoreCase(examination.getStatus())) {
            throw new ForbiddenException("Examination subject is not available to this teacher");
        }
        boolean assigned = assignmentRepository
                .existsByTeacherIdAndSchoolIdAndSubjectIdAndClassIdAndStreamIdAndAcademicYearIdAndDeletedAtIsNull(
                        teacher.getId(), schoolId, examSubject.getSubjectId(), examSubject.getClassId(),
                        examSubject.getStreamId(), examination.getAcademicYearId());
        if (!assigned && examSubject.getStreamId() != null) {
            assigned = assignmentRepository
                    .existsByTeacherIdAndSchoolIdAndSubjectIdAndClassIdAndStreamIdIsNullAndAcademicYearIdAndDeletedAtIsNull(
                            teacher.getId(), schoolId, examSubject.getSubjectId(), examSubject.getClassId(),
                            examination.getAcademicYearId());
        }
        if (!assigned) {
            throw new ForbiddenException("You are not assigned to this examination subject");
        }
        return examSubject;
    }

    private User requireTeacher() {
        User teacher = currentUserService.requireCurrentUser();
        if (!currentUserService.hasRole(teacher, "TEACHER")) {
            throw new ForbiddenException("Teacher access is required");
        }
        return teacher;
    }

    private Long requireSchool(User teacher) {
        Long schoolId = TenantContext.requireTenantId();
        if (!schoolId.equals(teacher.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }
        return schoolId;
    }

    private TeacherTimetableDto toTimetableDto(TeacherTimetable timetable) {
        TeacherAssignment assignment = timetable.getTeacherAssignment();
        return new TeacherTimetableDto(timetable.getId(), timetable.getTeacherAssignmentId(),
                timetable.getDayOfWeek(), timetable.getStartTime(), timetable.getEndTime(),
                timetable.getRoom(), assignment.getClazz().getName(),
                assignment.getStream() == null ? null : assignment.getStream().getName(),
                assignment.getSubject().getName());
    }

    private record EnrollmentScope(Long classId, Long streamId, Long academicYearId) {}
}
