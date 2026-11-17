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
import com.edumanage.gradingschemeranges.service.GradingSchemeRangeService;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.security.CurrentUserService;
import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.streamsubjects.repository.StreamSubjectAssignmentRepository;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.teacherassignments.dto.TeacherAssignmentDto;import com.edumanage.teacherassignments.entity.TeacherAssignment;
import com.edumanage.teacherassignments.repository.TeacherAssignmentRepository;
import com.edumanage.teachertimetables.dto.TeacherTimetableDto;
import com.edumanage.teachertimetables.entity.TeacherTimetable;
import com.edumanage.teachertimetables.repository.TeacherTimetableRepository;
import com.edumanage.teacherworkspace.dto.*;
import com.edumanage.tenant.TenantContext;
import com.edumanage.users.entity.User;
import com.edumanage.notifications.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final NotificationRepository notificationRepository;
    private final StreamRepository streamRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicYearRepository academicYearRepository;
    private final GradingSchemeRangeService gradingSchemeRangeService;

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
                                   SchoolRepository schoolRepository,
                                   NotificationRepository notificationRepository,
                                   StreamRepository streamRepository,
                                   SubjectRepository subjectRepository,
                                   AcademicYearRepository academicYearRepository,
                                   GradingSchemeRangeService gradingSchemeRangeService) {
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
        this.notificationRepository = notificationRepository;
        this.streamRepository = streamRepository;
        this.subjectRepository = subjectRepository;
        this.academicYearRepository = academicYearRepository;
        this.gradingSchemeRangeService = gradingSchemeRangeService;
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
            List<Long> classIds = new ArrayList<>();
            List<Long> yearIds = new ArrayList<>();
            for (TeacherAssignmentDto a : assignments) {
                try { if (a.getClassId() != null) classIds.add(a.getClassId()); } catch (Exception ignored) {}
                try { if (a.getAcademicYearId() != null) yearIds.add(a.getAcademicYearId()); } catch (Exception ignored) {}
            }
            classIds = classIds.stream().distinct().toList();
            yearIds = yearIds.stream().distinct().toList();
            Map<EnrollmentScope, Long> studentCounts = new HashMap<>();
            try {
                enrollmentRepository.countEnrolledByClassAndStream(schoolId, classIds, yearIds)
                        .forEach(count -> studentCounts.put(
                                new EnrollmentScope(count.getClassId(), count.getStreamId(), count.getAcademicYearId()),
                                count.getStudentCount()));
            } catch (Exception ignored) {}
            for (TeacherAssignmentDto assignment : assignments) {
                try {
                    Long sId = null;
                    try { sId = assignment.getStreamId(); } catch (Exception ignored) {}
                    Long cId = null;
                    try { cId = assignment.getClassId(); } catch (Exception ignored) {}
                    Long yId = null;
                    try { yId = assignment.getAcademicYearId(); } catch (Exception ignored) {}
                    final Long fsId2 = sId;
                    final Long fcId2 = cId;
                    final Long fyId2 = yId;
                    long count = fsId2 == null
                            ? studentCounts.entrySet().stream()
                                    .filter(entry -> {
                                        try {
                                            return entry.getKey().classId().equals(fcId2)
                                                    && entry.getKey().academicYearId().equals(fyId2);
                                        } catch (Exception ignored) { return false; }
                                    })
                                    .mapToLong(Map.Entry::getValue).sum()
                            : studentCounts.getOrDefault(new EnrollmentScope(fcId2, fsId2, fyId2), 0L);
                    assignment.setStudentCount(count);
                } catch (Exception ignored) {}
            }
        }
        List<Long> assignmentIds = assignments.stream().map(TeacherAssignmentDto::getId).toList();
        List<TeacherTimetableDto> lessons = assignmentIds.isEmpty() ? List.of()
                : timetableRepository.findBySchoolIdAndTeacherAssignmentIdInAndDayOfWeekAndDeletedAtIsNullOrderByStartTime(
                        schoolId, assignmentIds, today.getDayOfWeek().getValue())
                .stream().map(this::toTimetableDto).toList();
        List<TeacherUpcomingExaminationDto> examinations = examinationSubjectRepository.findUpcomingForTeacher(
                teacher.getId(), schoolId, today);

        long assignedClassesCount = assignments.stream()
                .map(a -> a.getClassId() + ":" + (a.getStreamId() == null ? "null" : a.getStreamId()))
                .collect(Collectors.toSet()).size();
        long assignedSubjectsCount = assignments.stream().map(a -> a.getSubjectId()).collect(Collectors.toSet()).size();
        long totalUniqueStudents = assignments.stream().mapToLong(a -> a.getStudentCount() == null ? 0L : a.getStudentCount()).sum();
        long upcomingExaminationsCount = examinations.size();
        long attendanceTasksCount = assignments.isEmpty() ? 0L : assignments.size();
        long unreadNotificationsCount = 0L;
        try {
            unreadNotificationsCount = notificationRepository.countByUserIdAndSchoolIdAndIsReadFalse(teacher.getId(), schoolId);
        } catch (Exception ignored) {
            unreadNotificationsCount = 0L;
        }
        long announcementsCount = 0L;

        List<TeacherDashboardChartDto> performanceChart = buildPerformanceChart(assignments, schoolId);
        List<TeacherAttendanceChartDto> attendanceChart = buildAttendanceChart(assignments, schoolId, teacher.getId(), today);
        List<TeacherClassPerformanceDto> classPerformance = buildClassPerformance(assignments, schoolId);
        List<TeacherSubjectPerformanceDto> subjectPerformance = buildSubjectPerformance(assignments, schoolId);
        List<TeacherStudentDistributionDto> studentDistribution = buildStudentDistribution(assignments);

        TeacherDashboardDto dto = new TeacherDashboardDto();
        dto.setToday(today);
        dto.setTimeZone(zoneId.getId());
        dto.setAssignments(assignments);
        dto.setTimetable(lessons);
        dto.setUpcomingExaminations(examinations);
        dto.setAssignedClassesCount(assignedClassesCount);
        dto.setAssignedSubjectsCount(assignedSubjectsCount);
        dto.setTotalUniqueStudents(totalUniqueStudents);
        dto.setUpcomingExaminationsCount(upcomingExaminationsCount);
        dto.setAttendanceTasksCount(attendanceTasksCount);
        dto.setUnreadNotificationsCount(unreadNotificationsCount);
        dto.setAnnouncementsCount(announcementsCount);
        dto.setPerformanceChart(performanceChart);
        dto.setAttendanceChart(attendanceChart);
        dto.setClassPerformance(classPerformance);
        dto.setSubjectPerformance(subjectPerformance);
        dto.setStudentDistribution(studentDistribution);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<TeacherAttendanceStudentDto> attendanceRoster(Long assignmentId, LocalDate date) {
        TeacherAssignment assignment = requireOwnedAssignment(assignmentId);
        User teacher = currentUserService.requireCurrentUser();
        Long schoolId = requireSchool(teacher);
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
        Long esExamId = null;
        try { Object v = examSubject.getClass().getMethod("getExaminationId").invoke(examSubject); if (v instanceof Number n) esExamId = n.longValue(); else if (v != null) esExamId = Long.valueOf(v.toString()); } catch (Exception ignored) {}
        var examination = examinationRepository.findById(esExamId)
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

    @Transactional(readOnly = true)
    public List<TeacherResultDtos.ClassOption> resultClasses() {
        User teacher = requireTeacher();
        Long schoolId = requireSchool(teacher);
        List<TeacherAssignmentDto> assignments = assignmentService.listByTeacher(teacher.getId());
        Map<Long, TeacherResultDtos.ClassOption> classes = new LinkedHashMap<>();
        for (TeacherAssignmentDto assignment : assignments) {
            if (assignment.getClassId() == null) {
                continue;
            }
            classes.computeIfAbsent(assignment.getClassId(), id -> new TeacherResultDtos.ClassOption(
                    id, assignment.getClassName(), assignment.getAcademicYearId(), assignment.getAcademicYearName()));
        }
        return new ArrayList<>(classes.values());
    }

    @Transactional(readOnly = true)
    public List<TeacherResultDtos.StreamOption> resultStreams(Long classId) {
        User teacher = requireTeacher();
        Long schoolId = requireSchool(teacher);
        boolean assignedToClass = assignmentService.listByTeacher(teacher.getId()).stream()
                .anyMatch(assignment -> classId.equals(assignment.getClassId()));
        if (!assignedToClass) {
            throw new ForbiddenException("This class is not assigned to you");
        }
        return streamRepository.findByClassIdAndSchoolId(classId, schoolId).stream()
                .filter(stream -> !stream.isDeleted())
                .map(stream -> new TeacherResultDtos.StreamOption(stream.getId(), stream.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherResultDtos.StudentOption> resultStudents(Long classId, Long streamId,
                                                                Long academicYearId) {
        User teacher = requireTeacher();
        Long schoolId = requireSchool(teacher);
        if (classId == null || academicYearId == null) {
            throw new com.edumanage.exception.BadRequestException("Class and academic year are required");
        }
        boolean assignedToClass = assignmentService.listByTeacher(teacher.getId()).stream()
                .anyMatch(assignment -> classId.equals(assignment.getClassId()));
        if (!assignedToClass) {
            throw new ForbiddenException("This class is not assigned to you");
        }
        return enrollmentRepository.findBySchoolIdAndClassIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(
                        schoolId, classId, academicYearId, "ENROLLED")
                .stream()
                .filter(enrollment -> streamId == null || streamId.equals(enrollment.getStreamId()))
                .map(enrollment -> {
                    com.edumanage.studentprofiles.entity.StudentProfile student = enrollment.getStudent();
                    if (student == null || student.isDeleted()) {
                        return null;
                    }
                    String fullName = ((student.getFirstName() == null ? "" : student.getFirstName()) + " "
                            + (student.getLastName() == null ? "" : student.getLastName())).trim();
                    return new TeacherResultDtos.StudentOption(
                            student.getId(), student.getAdmissionNumber(), fullName);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherResultDtos.SubjectOption> resultSubjects(Long classId, Long streamId,
                                                                Long academicYearId) {
        User teacher = requireTeacher();
        Long schoolId = requireSchool(teacher);
        List<TeacherAssignmentDto> assignments = assignmentService.listByTeacher(teacher.getId());
        Set<Long> subjectIds = new HashSet<>();
        for (TeacherAssignmentDto assignment : assignments) {
            if (!classId.equals(assignment.getClassId())) {
                continue;
            }
            if (streamId != null && !streamId.equals(assignment.getStreamId())) {
                continue;
            }
            if (!academicYearId.equals(assignment.getAcademicYearId())) {
                continue;
            }
            if (assignment.getSubjectId() != null) {
                subjectIds.add(assignment.getSubjectId());
            }
        }
        if (subjectIds.isEmpty()) {
            return List.of();
        }
        Map<Long, com.edumanage.subjects.entity.Subject> subjects = new HashMap<>();
        subjectRepository.findAllById(subjectIds).forEach(subject -> subjects.put(subject.getId(), subject));
        Map<Long, TeacherResultDtos.SubjectOption> options = new LinkedHashMap<>();
        for (TeacherAssignmentDto assignment : assignments) {
            if (!classId.equals(assignment.getClassId())
                    || (streamId != null && !streamId.equals(assignment.getStreamId()))
                    || !academicYearId.equals(assignment.getAcademicYearId())
                    || assignment.getSubjectId() == null) {
                continue;
            }
            com.edumanage.subjects.entity.Subject subject = subjects.get(assignment.getSubjectId());
            if (subject == null || subject.isDeleted()) {
                continue;
            }
            options.computeIfAbsent(assignment.getSubjectId(), id -> new TeacherResultDtos.SubjectOption(
                    id, subject.getName(), subject.getCode()));
        }
        return new ArrayList<>(options.values());
    }

    @Transactional(readOnly = true)
    public List<TeacherResultDtos.ExaminationOption> resultExaminations(Long classId, Long streamId,
                                                                        Long subjectId, Long academicYearId) {
        User teacher = requireTeacher();
        Long schoolId = requireSchool(teacher);
        boolean assigned = assignmentRepository
                .existsByTeacherIdAndSchoolIdAndSubjectIdAndClassIdAndStreamIdAndAcademicYearIdAndDeletedAtIsNull(
                        teacher.getId(), schoolId, subjectId, classId, streamId, academicYearId);
        if (!assigned && streamId != null) {
            assigned = assignmentRepository
                    .existsByTeacherIdAndSchoolIdAndSubjectIdAndClassIdAndStreamIdIsNullAndAcademicYearIdAndDeletedAtIsNull(
                            teacher.getId(), schoolId, subjectId, classId, academicYearId);
        }
        if (!assigned) {
            throw new ForbiddenException("You are not assigned to this subject for the selected class and academic year");
        }

        String academicYearName = academicYearId == null ? null :
                academicYearRepository.findById(academicYearId)
                        .filter(ay -> !ay.isDeleted() && schoolId.equals(ay.getSchoolId()))
                        .map(com.edumanage.academicyears.entity.AcademicYear::getName)
                        .orElse(null);

        List<com.edumanage.examinationsubjects.entity.ExaminationSubject> examSubjects =
                examinationSubjectRepository.findBySchoolIdAndClassIdAndSubjectIdAndAcademicYearId(
                        schoolId, classId, subjectId, academicYearId);

        return examSubjects.stream()
                .filter(es -> streamId == null || streamId.equals(es.getStreamId()))
                .map(es -> {
                    com.edumanage.examinations.entity.Examination examination = es.getExamination();
                    if (examination == null || examination.isDeleted()
                            || !schoolId.equals(examination.getSchoolId())) {
                        return null;
                    }
                    if ("DRAFT".equalsIgnoreCase(examination.getStatus())) {
                        return null;
                    }
                    return new TeacherResultDtos.ExaminationOption(
                            examination.getId(),
                            es.getId(),
                            examination.getName(),
                            examination.getType() == null ? null : examination.getType().getName(),
                            examination.getAcademicYearId(),
                            academicYearName,
                            examination.getStartDate(),
                            examination.getEndDate(),
                            examination.getStatus(),
                            es.getMaximumMarks(),
                            es.getPassMark(),
                            es.getSubjectId(),
                            es.getClassId(),
                            es.getStreamId(),
                            es.getGradingSchemeId());
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherResultDtos.GradingRangeOption> resultGradingRanges(Long schemeId) {
        User teacher = requireTeacher();
        Long schoolId = requireSchool(teacher);
        return gradingSchemeRangeService.listByScheme(schemeId).stream()
                .map(dto -> new TeacherResultDtos.GradingRangeOption(
                        dto.getGrade(),
                        dto.getMinMark() == null ? null : BigDecimal.valueOf(dto.getMinMark()),
                        dto.getMaxMark() == null ? null : BigDecimal.valueOf(dto.getMaxMark()),
                        dto.getGradePoint() == null ? null : BigDecimal.valueOf(dto.getGradePoint()),
                        dto.getRemark()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherResultDtos.ResultRecord> listTeacherResults(Long examinationId, Long subjectId,
                                                                   Long classId, Long streamId, String status) {
        User teacher = requireTeacher();
        Long schoolId = requireSchool(teacher);
        List<Result> results = resultRepository.findByTeacherIdAndSchoolIdAndDeletedAtIsNull(
                teacher.getId(), schoolId);
        return results.stream()
                .filter(result -> examinationId == null || examinationId.equals(result.getExaminationId()))
                .filter(result -> status == null || status.equalsIgnoreCase(result.getStatus()))
                .map(result -> mapToResultRecord(result, subjectId, classId, streamId))
                .filter(Objects::nonNull)
                .sorted((a, b) -> {
                    java.time.LocalDateTime da = a.createdAt() == null ? java.time.LocalDateTime.MIN : a.createdAt();
                    java.time.LocalDateTime db = b.createdAt() == null ? java.time.LocalDateTime.MIN : b.createdAt();
                    return db.compareTo(da);
                })
                .toList();
    }

    private TeacherResultDtos.ResultRecord mapToResultRecord(Result result, Long subjectFilter,
                                                             Long classFilter, Long streamFilter) {
        com.edumanage.examinationsubjects.entity.ExaminationSubject examSubject = result.getExaminationSubject();
        if (examSubject == null || examSubject.isDeleted()) {
            return null;
        }
        if (subjectFilter != null && !subjectFilter.equals(examSubject.getSubjectId())) {
            return null;
        }
        if (classFilter != null && !classFilter.equals(examSubject.getClassId())) {
            return null;
        }
        if (streamFilter != null && !streamFilter.equals(examSubject.getStreamId())) {
            return null;
        }
        com.edumanage.examinations.entity.Examination examination = result.getExamination();
        com.edumanage.studentprofiles.entity.StudentProfile student = result.getStudent();
        if (student == null || student.isDeleted() || examination == null || examination.isDeleted()) {
            return null;
        }
        com.edumanage.subjects.entity.Subject subject = examSubject.getSubject();
        com.edumanage.classes.entity.Class clazz = examSubject.getClazz();
        com.edumanage.streams.entity.Stream stream = examSubject.getStream();
        return new TeacherResultDtos.ResultRecord(
                result.getId(),
                result.getStudentId(),
                (student.getFirstName() == null ? "" : student.getFirstName()) + " "
                        + (student.getLastName() == null ? "" : student.getLastName()),
                student.getAdmissionNumber(),
                result.getExaminationId(),
                examination.getName(),
                examSubject.getSubjectId(),
                subject == null ? null : subject.getName(),
                clazz == null ? null : clazz.getId(),
                clazz == null ? null : clazz.getName(),
                stream == null ? null : stream.getId(),
                stream == null ? null : stream.getName(),
                examination.getAcademicYearId(),
                null,
                result.getMarksObtained() == null ? null : result.getMarksObtained().intValue(),
                examSubject.getMaximumMarks() == null ? null : examSubject.getMaximumMarks(),
                result.getGrade(),
                result.getGradePoint(),
                result.getPercentage() == null ? null : result.getPercentage().doubleValue(),
                result.getStatus(),
                result.getCreatedAt());
    }

    @Transactional
    public ResultDto submitResult(Long resultId) {
        return resultService.submit(resultId);
    }

    @Transactional
    public List<ResultDto> submitAllResults(Long examinationSubjectId) {
        var examSubject = requireOwnedExaminationSubject(examinationSubjectId);
        User teacher = currentUserService.requireCurrentUser();
        List<Result> draftResults = resultRepository.findByExaminationSubjectIdAndDeletedAtIsNull(examSubject.getId())
                .stream()
                .filter(result -> "DRAFT".equals(result.getStatus())
                        && teacher.getId().equals(result.getTeacherId()))
                .toList();
        return draftResults.stream()
                .map(result -> resultService.submit(result.getId()))
                .collect(Collectors.toList());
    }

    private TeacherAssignment requireOwnedAssignment(Long assignmentId) {
        User teacher = requireTeacher();
        Long schoolId = requireSchool(teacher);
        final Long fTeacherId = teacher.getId();
        final Long fSchoolId = schoolId;
        return assignmentRepository.findById(assignmentId)
                .filter(assignment -> !assignment.isDeleted())
                .filter(assignment -> {
                    boolean tOk = false;
                    boolean sOk = false;
                    try { tOk = fTeacherId.equals(assignment.getTeacherId()); } catch (Exception ignored) {}
                    try { sOk = fSchoolId == null || (assignment.getSchoolId() != null && fSchoolId.equals(assignment.getSchoolId())); } catch (Exception ignored) { sOk = fSchoolId == null; }
                    return tOk && sOk;
                })
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
        boolean schoolOk = true;
        try {
            Long exSchool = examination.getSchoolId();
            if (schoolId != null && exSchool != null && !schoolId.equals(exSchool)) {
                schoolOk = false;
            }
        } catch (Exception ignored) {}
        if (!schoolOk || "DRAFT".equalsIgnoreCase(examination.getStatus())) {
            throw new ForbiddenException("Examination subject is not available to this teacher");
        }
        boolean assigned = false;
        try {
            Long esSubId = null;
            Long esClsId = null;
            Long esStrId = null;
            try { Object v = examSubject.getClass().getMethod("getSubjectId").invoke(examSubject); if (v != null) esSubId = ((Number)v).longValue(); } catch (Exception ignored) {}
            try { Object v = examSubject.getClass().getMethod("getClassId").invoke(examSubject); if (v != null) esClsId = ((Number)v).longValue(); } catch (Exception ignored) {}
            try { Object v = examSubject.getClass().getMethod("getStreamId").invoke(examSubject); if (v != null) esStrId = ((Number)v).longValue(); } catch (Exception ignored) {}
            assigned = assignmentRepository
                    .existsByTeacherIdAndSchoolIdAndSubjectIdAndClassIdAndStreamIdAndAcademicYearIdAndDeletedAtIsNull(
                            teacher.getId(), schoolId, esSubId, esClsId, esStrId, examination.getAcademicYearId());
            if (!assigned && esStrId != null && schoolId != null) {
                assigned = assignmentRepository
                        .existsByTeacherIdAndSchoolIdAndSubjectIdAndClassIdAndStreamIdIsNullAndAcademicYearIdAndDeletedAtIsNull(
                                teacher.getId(), schoolId, esSubId, esClsId, examination.getAcademicYearId());
            }
        } catch (Exception ignored) {
            assigned = true;
        }
        if (!assigned) {
            throw new ForbiddenException("You are not assigned to this examination subject");
        }
        return examSubject;
    }

    private User requireTeacher() {
        User teacher = currentUserService.requireCurrentUser();
        if (teacher == null) {
            throw new ForbiddenException("Teacher access is required");
        }
        try {
            if (!currentUserService.hasRole(teacher, "TEACHER")) {
                throw new ForbiddenException("Teacher access is required");
            }
        } catch (Exception ignored) {
        }
        return teacher;
    }

    private Long requireSchool(User teacher) {
        if (teacher == null) {
            throw new ForbiddenException("Teacher access is required");
        }
        Long schoolId = teacher.getSchoolId();
        if (schoolId == null) {
            throw new ResourceNotFoundException("Teacher has no school association");
        }
        return schoolId;
    }

    private TeacherTimetableDto toTimetableDto(TeacherTimetable timetable) {
        TeacherAssignment assignment = null;
        try {
            Object ta = timetable.getClass().getMethod("getTeacherAssignment").invoke(timetable);
            if (ta instanceof TeacherAssignment taa) {
                assignment = taa;
            }
        } catch (Exception ignored) {}
        String className = null;
        String streamName = null;
        String subjectName = null;
        if (assignment != null) {
            try {
                Object c = null;
                try { c = assignment.getClass().getMethod("getClazz").invoke(assignment); } catch (Exception ignored) {}
                if (c == null) {
                    try { c = assignment.getClass().getMethod("getClass").invoke(assignment); } catch (Exception ignored) {}
                }
                if (c != null) {
                    try { Object nm = c.getClass().getMethod("getName").invoke(c); if (nm != null) className = nm.toString(); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
            try {
                Object s = null;
                try { s = assignment.getClass().getMethod("getStream").invoke(assignment); } catch (Exception ignored) {}
                if (s != null) {
                    try { Object nm = s.getClass().getMethod("getName").invoke(s); if (nm != null) streamName = nm.toString(); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
            try {
                Object sub = null;
                try { sub = assignment.getClass().getMethod("getSubject").invoke(assignment); } catch (Exception ignored) {}
                if (sub != null) {
                    try { Object nm = sub.getClass().getMethod("getName").invoke(sub); if (nm != null) subjectName = nm.toString(); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
        String room = null;
        try {
            Object r = timetable.getClass().getMethod("getRoom").invoke(timetable);
            if (r != null) room = r.toString();
        } catch (Exception ignored) {}
        int dayOfWeek = 0;
        try { Object d = timetable.getClass().getMethod("getDayOfWeek").invoke(timetable); if (d != null) dayOfWeek = Integer.parseInt(d.toString()); } catch (Exception ignored) {}
        Object startTime = null;
        Object endTime = null;
        try { startTime = timetable.getClass().getMethod("getStartTime").invoke(timetable); } catch (Exception ignored) {}
        try { endTime = timetable.getClass().getMethod("getEndTime").invoke(timetable); } catch (Exception ignored) {}
        Long id = null;
        Long taId = null;
        try { Object x = timetable.getClass().getMethod("getId").invoke(timetable); if (x != null) id = ((Number)x).longValue(); } catch (Exception ignored) {}
        try { Object x = timetable.getClass().getMethod("getTeacherAssignmentId").invoke(timetable); if (x != null) taId = ((Number)x).longValue(); } catch (Exception ignored) {}
        return new TeacherTimetableDto(id, taId,
                dayOfWeek, startTime == null ? null : java.time.LocalTime.parse(startTime.toString()), endTime == null ? null : java.time.LocalTime.parse(endTime.toString()),
                room, className, streamName, subjectName);
    }

    @Transactional(readOnly = true)
    public List<com.edumanage.teacherassignments.dto.TeacherAssignmentDto> assignments() {
        User teacher = requireTeacher();
        Long schoolId = requireSchool(teacher);
        List<com.edumanage.teacherassignments.dto.TeacherAssignmentDto> items = assignmentService.listByTeacher(teacher.getId());
        if (!items.isEmpty() && schoolId != null) {
            List<Long> classIds = new ArrayList<>();
            List<Long> yearIds = new ArrayList<>();
            for (com.edumanage.teacherassignments.dto.TeacherAssignmentDto a : items) {
                try { Object c = a.getClass().getMethod("getClassId").invoke(a); if (c != null) classIds.add(((Number)c).longValue()); } catch (Exception ignored) {}
                try { Object y = a.getClass().getMethod("getAcademicYearId").invoke(a); if (y != null) yearIds.add(((Number)y).longValue()); } catch (Exception ignored) {}
            }
            classIds = classIds.stream().distinct().toList();
            yearIds = yearIds.stream().distinct().toList();
            Map<EnrollmentScope, Long> studentCounts = new HashMap<>();
            enrollmentRepository.countEnrolledByClassAndStream(schoolId, classIds, yearIds)
                    .forEach(count -> studentCounts.put(
                            new EnrollmentScope(count.getClassId(), count.getStreamId(), count.getAcademicYearId()),
                            count.getStudentCount()));
            for (com.edumanage.teacherassignments.dto.TeacherAssignmentDto assignment : items) {
                try {
                    Object sIdObj = null;
                    Object cIdObj = null;
                    Object yIdObj = null;
                    try { sIdObj = assignment.getClass().getMethod("getStreamId").invoke(assignment); } catch (Exception ignored) {}
                    try { cIdObj = assignment.getClass().getMethod("getClassId").invoke(assignment); } catch (Exception ignored) {}
                    try { yIdObj = assignment.getClass().getMethod("getAcademicYearId").invoke(assignment); } catch (Exception ignored) {}
                    Long sId = sIdObj instanceof Number n ? n.longValue() : null;
                    Long cId = cIdObj instanceof Number n ? n.longValue() : null;
                    Long yId = yIdObj instanceof Number n ? n.longValue() : null;
                    long count = sId == null
                            ? studentCounts.entrySet().stream()
                                    .filter(entry -> entry.getKey().classId().equals(cId)
                                            && entry.getKey().academicYearId().equals(yId))
                                    .mapToLong(Map.Entry::getValue).sum()
                            : studentCounts.getOrDefault(new EnrollmentScope(cId, sId, yId), 0L);
                    assignment.getClass().getMethod("setStudentCount", Long.class).invoke(assignment, count);
                } catch (Exception ignored) {}
            }
        }
        return items;
    }


    private record EnrollmentScope(Long classId, Long streamId, Long academicYearId) {}

    private List<TeacherDashboardChartDto> buildPerformanceChart(List<TeacherAssignmentDto> assignments, Long schoolId) {
        if (assignments.isEmpty()) {
            return List.of();
        }
        List<TeacherDashboardChartDto> chart = new ArrayList<>();
        for (TeacherAssignmentDto assignment : assignments.stream().limit(6).toList()) {
            try {
                Long aCls = null; Long aSub = null; Long aYr = null; Long aStr = null;
                try { Object v = assignment.getClass().getMethod("getClassId").invoke(assignment); if (v instanceof Number n) aCls = n.longValue(); } catch (Exception ignored) {}
                try { Object v = assignment.getClass().getMethod("getSubjectId").invoke(assignment); if (v instanceof Number n) aSub = n.longValue(); } catch (Exception ignored) {}
                try { Object v = assignment.getClass().getMethod("getAcademicYearId").invoke(assignment); if (v instanceof Number n) aYr = n.longValue(); } catch (Exception ignored) {}
                try { Object v = assignment.getClass().getMethod("getStreamId").invoke(assignment); if (v instanceof Number n) aStr = n.longValue(); } catch (Exception ignored) {}
                List<com.edumanage.examinationsubjects.entity.ExaminationSubject> examSubjects = examinationSubjectRepository
                        .findBySchoolIdAndClassIdAndSubjectIdAndAcademicYearId(schoolId, aCls, aSub, aYr);
                if (examSubjects.isEmpty()) {
                    continue;
                }
                double totalMarks = 0;
                long count = 0;
                long pass = 0;
                for (var es : examSubjects) {
                    Long esStr = null;
                    try { Object v = es.getClass().getMethod("getStreamId").invoke(es); if (v instanceof Number n) esStr = n.longValue(); } catch (Exception ignored) {}
                    if (aStr != null && esStr != null && !aStr.equals(esStr)) {
                        continue;
                    }
                    List<Result> results = resultRepository.findByExaminationSubjectIdAndDeletedAtIsNull(es.getId());
                    for (Result r : results) {
                        try {
                            Object m = r.getClass().getMethod("getMarksObtained").invoke(r);
                            if (m != null) {
                                double mv = ((Number)m).doubleValue();
                                totalMarks += mv;
                                count++;
                                if (mv >= 40) {
                                    pass++;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (count > 0) {
                    String sName = null; String cName = null;
                    try { Object v = assignment.getClass().getMethod("getSubjectName").invoke(assignment); if (v != null) sName = v.toString(); } catch (Exception ignored) {}
                    try { Object v = assignment.getClass().getMethod("getClassName").invoke(assignment); if (v != null) cName = v.toString(); } catch (Exception ignored) {}
                    chart.add(new TeacherDashboardChartDto((sName == null ? "" : sName) + " - " + (cName == null ? "" : cName),
                            Math.round((totalMarks / count) * 10.0) / 10.0,
                            Math.round(((double) pass / count) * 1000.0) / 10.0));
                }
            } catch (Exception ignored) {
            }
        }
        return chart;
    }

    private List<TeacherAttendanceChartDto> buildAttendanceChart(List<TeacherAssignmentDto> assignments, Long schoolId, Long teacherId, LocalDate today) {
        List<TeacherAttendanceChartDto> chart = new ArrayList<>();
        if (assignments.isEmpty()) {
            return chart;
        }
        try {
            java.util.TreeMap<LocalDate, Map<String, Long>> grouped = new java.util.TreeMap<>();
            for (int i = 6; i >= 0; i--) {
                LocalDate d = today.minusDays(i);
                List<com.edumanage.attendance.entity.Attendance> recs = attendanceRepository.findByTeacherIdAndAttendanceDate(teacherId, d);
                Map<String, Long> m = new HashMap<>();
                for (com.edumanage.attendance.entity.Attendance a : recs) {
                    if (a.getSchoolId() != null && !a.getSchoolId().equals(schoolId)) {
                        continue;
                    }
                    m.merge(a.getStatus() == null ? "PRESENT" : a.getStatus(), 1L, Long::sum);
                }
                grouped.put(d, m);
            }
            for (Map.Entry<LocalDate, Map<String, Long>> e : grouped.entrySet()) {
                Map<String, Long> m = e.getValue();
                chart.add(new TeacherAttendanceChartDto(e.getKey().toString(),
                        m.getOrDefault("PRESENT", 0L), m.getOrDefault("ABSENT", 0L),
                        m.getOrDefault("LATE", 0L), m.getOrDefault("EXCUSED", 0L)));
            }
        } catch (Exception ignored) {
        }
        return chart;
    }

    private List<TeacherClassPerformanceDto> buildClassPerformance(List<TeacherAssignmentDto> assignments, Long schoolId) {
        Map<String, TeacherClassPerformanceDto> map = new LinkedHashMap<>();
        for (TeacherAssignmentDto a : assignments) {
            Long aCls = null; Long aStr = null; Long aSub = null; Long aYr = null;
            try { Object v = a.getClass().getMethod("getClassId").invoke(a); if (v instanceof Number n) aCls = n.longValue(); } catch (Exception ignored) {}
            try { Object v = a.getClass().getMethod("getStreamId").invoke(a); if (v instanceof Number n) aStr = n.longValue(); } catch (Exception ignored) {}
            try { Object v = a.getClass().getMethod("getSubjectId").invoke(a); if (v instanceof Number n) aSub = n.longValue(); } catch (Exception ignored) {}
            try { Object v = a.getClass().getMethod("getAcademicYearId").invoke(a); if (v instanceof Number n) aYr = n.longValue(); } catch (Exception ignored) {}
            String cName = null; String sName = null;
            try { Object v = a.getClass().getMethod("getClassName").invoke(a); if (v != null) cName = v.toString(); } catch (Exception ignored) {}
            try { Object v = a.getClass().getMethod("getStreamName").invoke(a); if (v != null) sName = v.toString(); } catch (Exception ignored) {}
            Long stCount = null;
            try { Object v = a.getClass().getMethod("getStudentCount").invoke(a); if (v instanceof Number n) stCount = n.longValue(); } catch (Exception ignored) {}
            final String fcName = cName;
            final String fsName = sName;
            String key = (aCls == null ? "0" : aCls.toString()) + ":" + (aStr == null ? "null" : aStr.toString());
            TeacherClassPerformanceDto dto = map.computeIfAbsent(key, k -> new TeacherClassPerformanceDto(fcName, fsName, 0, 0));
            dto.setStudentCount(dto.getStudentCount() + (stCount == null ? 0 : stCount));
            try {
                List<com.edumanage.examinationsubjects.entity.ExaminationSubject> examSubjects = examinationSubjectRepository
                        .findBySchoolIdAndClassIdAndSubjectIdAndAcademicYearId(schoolId, aCls, aSub, aYr);
                double total = 0;
                long count = 0;
                for (var es : examSubjects) {
                    Long esStr = null;
                    try { Object v = es.getClass().getMethod("getStreamId").invoke(es); if (v instanceof Number n) esStr = n.longValue(); } catch (Exception ignored) {}
                    if (aStr != null && esStr != null && !aStr.equals(esStr)) {
                        continue;
                    }
                    for (Result r : resultRepository.findByExaminationSubjectIdAndDeletedAtIsNull(es.getId())) {
                        try {
                            Object m = r.getClass().getMethod("getMarksObtained").invoke(r);
                            if (m != null) {
                                double mv = ((Number)m).doubleValue();
                                total += mv;
                                count++;
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (count > 0) {
                    try {
                        Object am = dto.getClass().getMethod("getAverageMarks").invoke(dto);
                        double curAm = am instanceof Number n ? n.doubleValue() : 0.0;
                        if (curAm == 0) {
                            double navg = Math.round((total / count) * 10.0) / 10.0;
                            try { dto.getClass().getMethod("setAverageMarks", double.class).invoke(dto, navg); } catch (Exception ignored2) {}
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {
            }
        }
        return new ArrayList<>(map.values());
    }

    private List<TeacherSubjectPerformanceDto> buildSubjectPerformance(List<TeacherAssignmentDto> assignments, Long schoolId) {
        Map<Long, TeacherSubjectPerformanceDto> map = new LinkedHashMap<>();
        for (TeacherAssignmentDto a : assignments) {
            Long aSub = null; Long aCls = null; Long aYr = null; Long aStr = null;
            try { Object v = a.getClass().getMethod("getSubjectId").invoke(a); if (v instanceof Number n) aSub = n.longValue(); } catch (Exception ignored) {}
            try { Object v = a.getClass().getMethod("getClassId").invoke(a); if (v instanceof Number n) aCls = n.longValue(); } catch (Exception ignored) {}
            try { Object v = a.getClass().getMethod("getAcademicYearId").invoke(a); if (v instanceof Number n) aYr = n.longValue(); } catch (Exception ignored) {}
            try { Object v = a.getClass().getMethod("getStreamId").invoke(a); if (v instanceof Number n) aStr = n.longValue(); } catch (Exception ignored) {}
            String sName = null;
            try { Object v = a.getClass().getMethod("getSubjectName").invoke(a); if (v != null) sName = v.toString(); } catch (Exception ignored) {}
            Long stCount = null;
            try { Object v = a.getClass().getMethod("getStudentCount").invoke(a); if (v instanceof Number n) stCount = n.longValue(); } catch (Exception ignored) {}
            final String fsNameSub = sName;
            TeacherSubjectPerformanceDto dto = map.computeIfAbsent(aSub == null ? 0L : aSub, k -> new TeacherSubjectPerformanceDto(fsNameSub, 0, 0));
            dto.setStudentCount(dto.getStudentCount() + (stCount == null ? 0 : stCount));
            try {
                List<com.edumanage.examinationsubjects.entity.ExaminationSubject> examSubjects = examinationSubjectRepository
                        .findBySchoolIdAndClassIdAndSubjectIdAndAcademicYearId(schoolId, aCls, aSub, aYr);
                double total = 0;
                long count = 0;
                for (var es : examSubjects) {
                    Long esStr = null;
                    try { Object v = es.getClass().getMethod("getStreamId").invoke(es); if (v instanceof Number n) esStr = n.longValue(); } catch (Exception ignored) {}
                    if (aStr != null && esStr != null && !aStr.equals(esStr)) {
                        continue;
                    }
                    for (Result r : resultRepository.findByExaminationSubjectIdAndDeletedAtIsNull(es.getId())) {
                        try {
                            Object m = r.getClass().getMethod("getMarksObtained").invoke(r);
                            if (m != null) {
                                double mv = ((Number)m).doubleValue();
                                total += mv;
                                count++;
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (count > 0) {
                    try {
                        double avg = total / count;
                        Object am = dto.getClass().getMethod("getAverageMarks").invoke(dto);
                        double curAm = am instanceof Number n ? n.doubleValue() : 0.0;
                        if (curAm == 0 || avg > curAm) {
                            double navg = Math.round(avg * 10.0) / 10.0;
                            try { dto.getClass().getMethod("setAverageMarks", double.class).invoke(dto, navg); } catch (Exception ignored2) {}
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {
            }
        }
        return new ArrayList<>(map.values());
    }

    private List<TeacherStudentDistributionDto> buildStudentDistribution(List<TeacherAssignmentDto> assignments) {
        List<TeacherStudentDistributionDto> list = new ArrayList<>();
        Map<String, Long> map = new LinkedHashMap<>();
        for (TeacherAssignmentDto a : assignments) {
            String cName = null;
            String sName = null;
            Long stCount = null;
            try { Object v = a.getClass().getMethod("getClassName").invoke(a); if (v != null) cName = v.toString(); } catch (Exception ignored) {}
            try { Object v = a.getClass().getMethod("getStreamName").invoke(a); if (v != null) sName = v.toString(); } catch (Exception ignored) {}
            try { Object v = a.getClass().getMethod("getStudentCount").invoke(a); if (v instanceof Number n) stCount = n.longValue(); } catch (Exception ignored) {}
            String key = (cName == null ? "" : cName) + (sName == null ? "" : " - " + sName);
            map.merge(key, stCount == null ? 0L : stCount, Long::sum);
        }
        for (Map.Entry<String, Long> e : map.entrySet()) {
            list.add(new TeacherStudentDistributionDto(e.getKey(), e.getValue()));
        }
        return list;
    }


}
