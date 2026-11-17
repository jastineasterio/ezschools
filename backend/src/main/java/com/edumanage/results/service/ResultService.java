package com.edumanage.results.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.gradingschemes.repository.GradingSchemeRepository;
import com.edumanage.gradingschemeranges.entity.GradingSchemeRange;
import com.edumanage.gradingschemeranges.repository.GradingSchemeRangeRepository;
import com.edumanage.results.dto.CreateResultRequest;
import com.edumanage.results.dto.ResultDto;
import com.edumanage.results.entity.GradeSnapshot;
import com.edumanage.results.entity.Result;
import com.edumanage.examinations.entity.Examination;
import com.edumanage.examinationsubjects.entity.ExaminationSubject;
import com.edumanage.results.repository.GradeSnapshotRepository;
import com.edumanage.results.repository.ResultRepository;
import com.edumanage.resultapprovals.entity.ResultApproval;
import com.edumanage.resultapprovals.repository.ResultApprovalRepository;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.examinationsubjects.repository.ExaminationSubjectRepository;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.examinations.repository.ExaminationRepository;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.security.AccessGuard;
import com.edumanage.security.CurrentUserService;
import com.edumanage.teacherassignments.repository.TeacherAssignmentRepository;
import com.edumanage.enrollments.repository.StudentEnrollmentRepository;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ResultService {

    private final ResultRepository resultRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ExaminationSubjectRepository examinationSubjectRepository;
    private final ExaminationRepository examinationRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final AccessGuard accessGuard;
    private final CurrentUserService currentUserService;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final GradingSchemeRepository gradingSchemeRepository;
    private final GradingSchemeRangeRepository gradingSchemeRangeRepository;
    private final GradeSnapshotRepository gradeSnapshotRepository;
    private final ResultApprovalRepository resultApprovalRepository;
    private final ClassRepository classRepository;
    private final StreamRepository streamRepository;
    private final SubjectRepository subjectRepository;
    private final ObjectMapper objectMapper;

    public ResultService(ResultRepository resultRepository,
                         StudentProfileRepository studentProfileRepository,
                         ExaminationSubjectRepository examinationSubjectRepository,
                         ExaminationRepository examinationRepository,
                         UserRepository userRepository,
                         AuditLogService auditLogService,
                         AccessGuard accessGuard,
                         CurrentUserService currentUserService,
                         TeacherAssignmentRepository teacherAssignmentRepository,
                         StudentEnrollmentRepository enrollmentRepository,
                         GradingSchemeRepository gradingSchemeRepository,
                         GradingSchemeRangeRepository gradingSchemeRangeRepository,
                         GradeSnapshotRepository gradeSnapshotRepository,
                         ResultApprovalRepository resultApprovalRepository,
                         ClassRepository classRepository,
                         StreamRepository streamRepository,
                         SubjectRepository subjectRepository,
                         ObjectMapper objectMapper) {
        this.resultRepository = resultRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.examinationSubjectRepository = examinationSubjectRepository;
        this.examinationRepository = examinationRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.accessGuard = accessGuard;
        this.currentUserService = currentUserService;
        this.teacherAssignmentRepository = teacherAssignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.gradingSchemeRepository = gradingSchemeRepository;
        this.gradingSchemeRangeRepository = gradingSchemeRangeRepository;
        this.gradeSnapshotRepository = gradeSnapshotRepository;
        this.resultApprovalRepository = resultApprovalRepository;
        this.classRepository = classRepository;
        this.streamRepository = streamRepository;
        this.subjectRepository = subjectRepository;
        this.objectMapper = objectMapper;
    }

    public PageResponse<ResultDto> list(Pageable pageable) {
        return list(pageable, null, null, null, null);
    }

    public PageResponse<ResultDto> list(Pageable pageable,
                                        Long examinationId, Long classId, Long streamId, Long subjectId) {
        Long tenantId = TenantContext.getTenantId();
        validateFilters(tenantId, examinationId, classId, streamId, subjectId);
        Page<Result> page;
        if (tenantId != null) {
            page = resultRepository.findByFilters(
                    tenantId, examinationId, classId, streamId, subjectId, pageable);
        } else {
            page = resultRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public PageResponse<ResultDto> listByStudent(Long studentId, Pageable pageable) {
        accessGuard.assertCanAccessStudent(studentId);
        var student = studentProfileRepository.findById(studentId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        User currentUser = currentUserService.requireCurrentUser();
        Page<Result> page = currentUserService.hasAnyRole(currentUser, "MANAGER", "HOD", "SUPER_ADMIN")
                ? resultRepository.findByStudentIdAndSchoolId(studentId, tenantId, pageable)
                : resultRepository.findByStudentIdAndSchoolIdAndStatusAndDeletedAtIsNull(
                        studentId, tenantId, "PUBLISHED", pageable);
        return PageResponse.from(page.map(this::mapToDto));
    }

    public PageResponse<ResultDto> listByExamination(Long examinationId, Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            validateFilters(tenantId, examinationId, null, null, null);
        }
        Page<Result> page = resultRepository.findByExaminationIdAndSchoolId(examinationId, tenantId != null ? tenantId : null, pageable);
        return PageResponse.from(page.map(this::mapToDto));
    }

    public ResultDto getById(Long id) {
        var result = resultRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Result not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(result.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(result);
    }

    @Transactional
    public ResultDto create(CreateResultRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        var student = studentProfileRepository.findById(request.getStudentId())
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (!tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Student does not belong to this school");
        }

        var examSubject = examinationSubjectRepository.findById(request.getExaminationSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Examination subject not found"));

        var examination = examinationRepository.findById(examSubject.getExaminationId()).orElse(null);
        if (examination == null || !tenantId.equals(examination.getSchoolId())) {
            throw new ForbiddenException("Examination subject does not belong to this school");
        }
        User actor = currentUserService.requireCurrentUser();
        if (!currentUserService.hasAnyRole(actor, "TEACHER", "MANAGER")) {
            throw new ForbiddenException("You are not authorised to enter results");
        }
        validateTeacherAssignment(actor, student.getId(), examSubject, examination);
        validateMarks(request.getMarksObtained(), examSubject.getMaximumMarks());
        var gradeCalculation = calculateGrade(
                request.getMarksObtained() != null ? BigDecimal.valueOf(request.getMarksObtained()) : null,
                examSubject.getGradingSchemeId(), tenantId);

        var existingResult = resultRepository.findByStudentIdAndExaminationSubjectId(request.getStudentId(), request.getExaminationSubjectId());
        if (existingResult.isPresent()) {
            throw new com.edumanage.exception.ConflictException("Result already exists for this student and examination subject");
        }

        Long teacherId = actor.getId();

        var teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));

        var gradingScheme = gradingSchemeRepository.findById(examSubject.getGradingSchemeId())
                .filter(s -> !s.isDeleted())
                .map(scheme -> new GradingSchemeRef(scheme.getId(), scheme.getName()))
                .orElse(null);

        Result result = new Result();
        result.setSchoolId(tenantId);
        result.setStudentId(request.getStudentId());
        result.setExaminationSubjectId(request.getExaminationSubjectId());
        result.setExaminationId(examSubject.getExaminationId());
        result.setTeacherId(teacherId);
        result.setMarksObtained(request.getMarksObtained() != null ? new BigDecimal(request.getMarksObtained()) : null);
        result.setPercentage(toPercentage(request.getMarksObtained(), examSubject.getMaximumMarks()));
        if (gradeCalculation != null) {
            result.setGrade(gradeCalculation.grade());
            result.setGradePoint(gradeCalculation.gradePoint() != null ? gradeCalculation.gradePoint() : null);
        }
        if (gradingScheme != null) {
            result.setGradingSchemeId(gradingScheme.id());
            result.setGradingSchemeName(gradingScheme.name());
        }
        result.setRemarks(request.getRemarks());
        result.setStatus("DRAFT");

        result.setStudent(student);
        result.setExaminationSubject(examSubject);
        result.setTeacher(teacher);

        resultRepository.save(result);

        auditLogService.log("RESULT_CREATE", "Result", result.getId(),
                "Created result for student", null, null, null);

        return mapToDto(result);
    }

    @Transactional
    public ResultDto update(Long id, CreateResultRequest request) {
        var result = resultRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Result not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(result.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        if (!"DRAFT".equals(result.getStatus())) {
            throw new BadRequestException("Only DRAFT results can be updated");
        }
        User actor = currentUserService.requireCurrentUser();
        if (!currentUserService.hasAnyRole(actor, "MANAGER", "TEACHER")) {
            throw new ForbiddenException("You are not authorised to update results");
        }
        if (currentUserService.hasRole(actor, "TEACHER")
                && !currentUserService.hasRole(actor, "MANAGER")) {
            if (!actor.getId().equals(result.getTeacherId())
                    || !result.getStudentId().equals(request.getStudentId())) {
                throw new ForbiddenException("Teachers may only update their own draft results");
            }
            var examSubject = examinationSubjectRepository.findById(result.getExaminationSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Examination subject not found"));
            var examination = examinationRepository.findById(result.getExaminationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Examination not found"));
            validateTeacherAssignment(actor, result.getStudentId(), examSubject, examination);
        }

        var student = studentProfileRepository.findById(request.getStudentId())
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (!result.getStudentId().equals(student.getId())
                || !result.getExaminationSubjectId().equals(request.getExaminationSubjectId())) {
            throw new BadRequestException("A result cannot be reassigned to another student or examination subject");
        }
        var examSubject = examinationSubjectRepository.findById(result.getExaminationSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Examination subject not found"));
        validateMarks(request.getMarksObtained(), examSubject.getMaximumMarks());
        var gradeCalculation = calculateGrade(
                request.getMarksObtained() != null ? BigDecimal.valueOf(request.getMarksObtained()) : null,
                examSubject.getGradingSchemeId(), result.getSchoolId());

        var gradingScheme = gradingSchemeRepository.findById(examSubject.getGradingSchemeId())
                .filter(s -> !s.isDeleted())
                .map(scheme -> new GradingSchemeRef(scheme.getId(), scheme.getName()))
                .orElse(null);

        result.setMarksObtained(request.getMarksObtained() != null ? new BigDecimal(request.getMarksObtained()) : null);
        result.setPercentage(toPercentage(request.getMarksObtained(), examSubject.getMaximumMarks()));
        if (gradeCalculation != null) {
            result.setGrade(gradeCalculation.grade());
            result.setGradePoint(gradeCalculation.gradePoint() != null ? gradeCalculation.gradePoint() : null);
        }
        if (gradingScheme != null) {
            result.setGradingSchemeId(gradingScheme.id());
            result.setGradingSchemeName(gradingScheme.name());
        }
        result.setRemarks(request.getRemarks());

        result.setStudent(student);
        result.setExaminationSubject(examSubject);
        var teacher = userRepository.findById(result.getTeacherId()).orElse(null);
        result.setTeacher(teacher);

        resultRepository.save(result);

        auditLogService.log("RESULT_UPDATE", "Result", result.getId(),
                "Updated result", null, null, null);

        return mapToDto(result);
    }

    @Transactional
    public ResultDto submit(Long id) {
        var result = resultRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Result not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(result.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        if (!"DRAFT".equals(result.getStatus())) {
            throw new BadRequestException("Only DRAFT results can be submitted");
        }
        User actor = currentUserService.requireCurrentUser();
        if (currentUserService.hasRole(actor, "TEACHER")
                && !currentUserService.hasRole(actor, "MANAGER")
                && !actor.getId().equals(result.getTeacherId())) {
            throw new ForbiddenException("Teachers may only submit their own draft results");
        }
        if (!currentUserService.hasAnyRole(actor, "TEACHER", "MANAGER")) {
            throw new ForbiddenException("You are not authorised to submit results");
        }

        result.setStatus("SUBMITTED");
        result.setSubmittedAt(LocalDateTime.now());
        resultRepository.save(result);
        createGradeSnapshot(result);

        auditLogService.log("RESULT_SUBMIT", "Result", result.getId(),
                "Submitted result", null, null, null);

        return mapToDto(result);
    }

    @Transactional
    public ResultDto reopen(Long id) {
        var result = resultRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Result not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(result.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        User actor = currentUserService.requireCurrentUser();
        if (!currentUserService.hasRole(actor, "MANAGER")) {
            throw new ForbiddenException("Only a Manager may reopen results");
        }

        if (!"APPROVED".equals(result.getStatus()) && !"PUBLISHED".equals(result.getStatus())) {
            throw new BadRequestException("Only APPROVED or PUBLISHED results can be reopened");
        }

        String previousStatus = result.getStatus();
        result.setStatus("DRAFT");
        result.setApprovedAt(null);
        result.setPublishedAt(null);
        result.setReviewedAt(null);
        resultRepository.save(result);

        recordResultApproval(result, "REOPENED", actor, previousStatus, "DRAFT",
                "Result reopened for correction by Manager");

        auditLogService.log("RESULT_REOPEN", "Result", result.getId(),
                "Reopened result from " + previousStatus + " to DRAFT", null, null, null);

        return mapToDto(result);
    }

    private void validateTeacherAssignment(User actor, Long studentId,
                                           com.edumanage.examinationsubjects.entity.ExaminationSubject examSubject,
                                           com.edumanage.examinations.entity.Examination examination) {
        if (!currentUserService.hasRole(actor, "TEACHER")
                || currentUserService.hasRole(actor, "MANAGER")) {
            return;
        }
        Long schoolId = TenantContext.requireTenantId();
        boolean assigned = teacherAssignmentRepository
                .existsByTeacherIdAndSchoolIdAndSubjectIdAndClassIdAndStreamIdAndAcademicYearIdAndDeletedAtIsNull(
                        actor.getId(), schoolId, examSubject.getSubjectId(), examSubject.getClassId(),
                        examSubject.getStreamId(), examination.getAcademicYearId());
        if (!assigned && examSubject.getStreamId() != null) {
            assigned = teacherAssignmentRepository
                    .existsByTeacherIdAndSchoolIdAndSubjectIdAndClassIdAndStreamIdIsNullAndAcademicYearIdAndDeletedAtIsNull(
                            actor.getId(), schoolId, examSubject.getSubjectId(), examSubject.getClassId(),
                            examination.getAcademicYearId());
        }
        if (!assigned) {
            throw new ForbiddenException("You are not assigned to this examination subject");
        }
        enrollmentRepository.findByStudentIdAndAcademicYearId(studentId,
                        examination.getAcademicYearId())
                .filter(value -> !value.isDeleted() && "ENROLLED".equals(value.getStatus())
                        && schoolId.equals(value.getSchoolId())
                        && examSubject.getClassId().equals(value.getClassId())
                        && (examSubject.getStreamId() == null
                        || examSubject.getStreamId().equals(value.getStreamId())))
                .orElseThrow(() -> new ForbiddenException("Student is outside the assigned examination class"));
    }

    private void validateMarks(Integer marks, BigDecimal maximumMarks) {
        if (maximumMarks == null || maximumMarks.signum() <= 0) {
            throw new BadRequestException("Examination maximum marks must be greater than zero");
        }
        if (marks != null && (marks < 0 || BigDecimal.valueOf(marks).compareTo(maximumMarks) > 0)) {
            throw new BadRequestException("Marks must be between 0 and the examination maximum");
        }
    }

    private BigDecimal toPercentage(Integer marks, BigDecimal maximumMarks) {
        return marks == null ? null : BigDecimal.valueOf(marks)
                .multiply(BigDecimal.valueOf(100))
                .divide(maximumMarks, 2, RoundingMode.HALF_UP);
    }

    @Transactional
    public ResultDto approve(Long id) {
        var result = resultRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Result not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(result.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        if (!"SUBMITTED".equals(result.getStatus()) && !"UNDER_REVIEW".equals(result.getStatus())) {
            throw new BadRequestException("Only SUBMITTED or UNDER_REVIEW results can be approved");
        }

        User actor = currentUserService.requireCurrentUser();
        if (!currentUserService.hasAnyRole(actor, "MANAGER", "HOD")) {
            throw new ForbiddenException("You are not authorised to approve results");
        }

        String previousStatus = result.getStatus();
        result.setStatus("APPROVED");
        result.setApprovedAt(LocalDateTime.now());
        resultRepository.save(result);

        recordResultApproval(result, "APPROVED", actor, previousStatus, "APPROVED", null);

        auditLogService.log("RESULT_APPROVE", "Result", result.getId(),
                "Approved result", null, null, null);

        return mapToDto(result);
    }

    @Transactional
    public ResultDto reject(Long id, String comment) {
        var result = resultRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Result not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(result.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        if (!"SUBMITTED".equals(result.getStatus()) && !"UNDER_REVIEW".equals(result.getStatus())) {
            throw new BadRequestException("Only SUBMITTED or UNDER_REVIEW results can be rejected");
        }

        User actor = currentUserService.requireCurrentUser();
        if (!currentUserService.hasAnyRole(actor, "MANAGER", "HOD")) {
            throw new ForbiddenException("You are not authorised to reject results");
        }

        String previousStatus = result.getStatus();
        result.setStatus("REJECTED");
        result.setRemarks(comment);
        resultRepository.save(result);

        recordResultApproval(result, "REJECTED", actor, previousStatus, "REJECTED", comment);

        auditLogService.log("RESULT_REJECT", "Result", result.getId(),
                "Rejected result", null, null, null);

        return mapToDto(result);
    }

    @Transactional
    public ResultDto publish(Long id) {
        var result = resultRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Result not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(result.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        if (!"APPROVED".equals(result.getStatus())) {
            throw new BadRequestException("Only APPROVED results can be published");
        }

        User actor = currentUserService.requireCurrentUser();
        if (!currentUserService.hasAnyRole(actor, "MANAGER", "HOD")) {
            throw new ForbiddenException("You are not authorised to publish results");
        }

        String previousStatus = result.getStatus();
        result.setStatus("PUBLISHED");
        result.setPublishedAt(LocalDateTime.now());
        resultRepository.save(result);

        recordResultApproval(result, "PUBLISHED", actor, previousStatus, "PUBLISHED", null);

        auditLogService.log("RESULT_PUBLISH", "Result", result.getId(),
                "Published result", null, null, null);

        return mapToDto(result);
    }

    private ResultDto mapToDto(Result entity) {
        ResultDto dto = new ResultDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setStudentId(entity.getStudentId());

        StudentProfile student = entity.getStudent();
        if (student != null) {
            dto.setStudentName(fullName(student));
            dto.setAdmissionNumber(student.getAdmissionNumber());
        }

        dto.setExaminationSubjectId(entity.getExaminationSubjectId());
        dto.setExaminationId(entity.getExaminationId());
        dto.setTeacherId(entity.getTeacherId());
        User teacher = entity.getTeacher();
        if (teacher != null) {
            dto.setTeacherName(currentUserService.displayName(teacher));
        }

        ExaminationSubject examinationSubject = entity.getExaminationSubject();
        if (examinationSubject != null) {
            dto.setSubjectId(examinationSubject.getSubjectId());
            if (examinationSubject.getSubject() != null) {
                dto.setSubjectName(examinationSubject.getSubject().getName());
            }
            dto.setClassId(examinationSubject.getClassId());
            if (examinationSubject.getClazz() != null) {
                dto.setClassName(examinationSubject.getClazz().getName());
            }
            dto.setStreamId(examinationSubject.getStreamId());
            if (examinationSubject.getStream() != null) {
                dto.setStreamName(examinationSubject.getStream().getName());
            }
            Examination examination = examinationSubject.getExamination();
            if (examination != null) {
                dto.setExaminationId(examination.getId());
                dto.setExaminationName(examination.getName());
            }
        }

        dto.setMarksObtained(entity.getMarksObtained() != null ? entity.getMarksObtained().intValue() : null);
        dto.setPercentage(entity.getPercentage() != null ? entity.getPercentage().doubleValue() : null);
        dto.setGrade(entity.getGrade());
        dto.setGradePoint(entity.getGradePoint() != null ? entity.getGradePoint().doubleValue() : null);
        dto.setRemarks(entity.getRemarks());
        dto.setStatus(entity.getStatus());
        dto.setSubmittedAt(entity.getSubmittedAt());
        dto.setReviewedAt(entity.getReviewedAt());
        dto.setApprovedAt(entity.getApprovedAt());
        dto.setPublishedAt(entity.getPublishedAt());
        dto.setGradingSchemeId(entity.getGradingSchemeId());
        dto.setGradingSchemeName(entity.getGradingSchemeName());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private static String fullName(StudentProfile student) {
        String first = student.getFirstName() == null ? "" : student.getFirstName().trim();
        String middle = student.getMiddleName() == null ? "" : student.getMiddleName().trim();
        String last = student.getLastName() == null ? "" : student.getLastName().trim();
        String name = (first + (middle.isEmpty() ? "" : " " + middle) + " " + last).trim();
        return name.isEmpty() ? String.valueOf(student.getId()) : name;
    }

    private void validateFilters(Long schoolId, Long examinationId, Long classId,
                                 Long streamId, Long subjectId) {
        if (schoolId == null) {
            return;
        }
        if (examinationId != null) {
            var examination = examinationRepository.findById(examinationId)
                    .filter(e -> !e.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Examination not found"));
            if (!schoolId.equals(examination.getSchoolId())) {
                throw new ForbiddenException("Examination does not belong to this school");
            }
        }
        if (classId != null) {
            var clazz = classRepository.findBySchoolIdAndId(schoolId, classId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
            if (clazz.isDeleted()) {
                throw new ResourceNotFoundException("Class not found");
            }
        }
        if (streamId != null) {
            var stream = streamRepository.findBySchoolIdAndId(schoolId, streamId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stream not found"));
            if (stream.isDeleted()) {
                throw new ResourceNotFoundException("Stream not found");
            }
            if (classId != null && !classId.equals(stream.getClassId())) {
                throw new BadRequestException("Selected stream does not belong to the selected class");
            }
        }
        if (subjectId != null) {
            var subject = subjectRepository.findBySchoolIdAndId(schoolId, subjectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
            if (subject.isDeleted()) {
                throw new ResourceNotFoundException("Subject not found");
            }
        }
        if (examinationId != null && classId != null && subjectId != null) {
            boolean compatible = examinationSubjectRepository
                    .findByExaminationIdAndSubjectIdAndClassIdAndStreamId(
                            examinationId, subjectId, classId, streamId)
                    .filter(es -> !es.isDeleted())
                    .isPresent();
            if (!compatible) {
                throw new BadRequestException("The selected examination, class, stream and subject are not compatible");
            }
        }
    }

    private void createGradeSnapshot(Result result) {
        if (result.getGradingSchemeId() == null) {
            return;
        }
        var scheme = gradingSchemeRepository.findById(result.getGradingSchemeId())
                .filter(s -> !s.isDeleted())
                .orElse(null);
        if (scheme == null) {
            return;
        }
        List<GradeSnapshot.RangeEntry> entries = gradingSchemeRangeRepository
                .findBySchemeIdOrderByMinMarkDesc(result.getGradingSchemeId()).stream()
                .map(range -> new GradeSnapshot.RangeEntry(
                        range.getGrade(),
                        range.getMinMark(),
                        range.getMaxMark(),
                        range.getGradePoint(),
                        range.getRemark()))
                .toList();
        JsonNode rangesJson;
        try {
            rangesJson = objectMapper.valueToTree(entries);
        } catch (Exception e) {
            rangesJson = objectMapper.valueToTree(List.of());
        }
        java.math.BigDecimal minMark = entries.stream()
                .map(GradeSnapshot.RangeEntry::getMinMark)
                .filter(java.util.Objects::nonNull)
                .min(java.math.BigDecimal::compareTo)
                .orElse(null);
        java.math.BigDecimal maxMark = entries.stream()
                .map(GradeSnapshot.RangeEntry::getMaxMark)
                .filter(java.util.Objects::nonNull)
                .max(java.math.BigDecimal::compareTo)
                .orElse(null);

        GradeSnapshot snapshot = new GradeSnapshot();
        snapshot.setResultId(result.getId());
        snapshot.setSchoolId(result.getSchoolId());
        snapshot.setGradingSchemeId(result.getGradingSchemeId());
        snapshot.setGradingSchemeName(result.getGradingSchemeName());
        snapshot.setMinMark(minMark);
        snapshot.setMaxMark(maxMark);
        snapshot.setGradeRanges(rangesJson);
        snapshot.setCreatedAt(LocalDateTime.now());
        gradeSnapshotRepository.save(snapshot);
    }

    private void recordResultApproval(Result result, String action, User actor,
                                      String oldStatus, String newStatus, String comment) {
        ResultApproval approval = new ResultApproval();
        approval.setResultId(result.getId());
        approval.setSchoolId(result.getSchoolId());
        approval.setAction(action);
        approval.setActorUserId(actor.getId());
        approval.setActorRole(currentUserService.rolesOf(actor).stream()
                .sorted().findFirst().orElse("UNKNOWN"));
        approval.setComment(comment);
        approval.setOldStatus(oldStatus);
        approval.setNewStatus(newStatus);
        resultApprovalRepository.save(approval);

        auditLogService.log("RESULT_" + action, "ResultApproval", approval.getId(),
                "Result " + action.toLowerCase() + ": " + oldStatus + " -> " + newStatus, null, null, null);
    }

    private record GradingSchemeRef(Long id, String name) {}

    private GradeCalculation calculateGrade(BigDecimal marks, Long schemeId, Long schoolId) {
        if (marks == null) {
            return null;
        }
        if (schemeId == null) {
            throw new BadRequestException("No grading scheme is configured for this examination subject. "
                    + "Ask the Head of Department to configure grading ranges before recording results.");
        }
        var scheme = gradingSchemeRepository.findById(schemeId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new BadRequestException("Grading scheme not found"));
        if (schoolId != null && !schoolId.equals(scheme.getSchoolId())) {
            throw new ForbiddenException("Grading scheme does not belong to this school");
        }
        if (!"ACTIVE".equalsIgnoreCase(scheme.getStatus())) {
            throw new BadRequestException("Grading scheme is not active");
        }
        List<GradingSchemeRange> ranges = gradingSchemeRangeRepository.findBySchemeIdOrderByMinMarkDesc(schemeId);
        if (ranges.isEmpty()) {
            throw new BadRequestException("No grading ranges are configured for this subject's grading scheme. "
                    + "Ask the Head of Department to configure them before recording results.");
        }
        GradeCalculation matched = null;
        for (GradingSchemeRange range : ranges) {
            BigDecimal min = range.getMinMark();
            BigDecimal max = range.getMaxMark();
            if (min == null || max == null) {
                continue;
            }
            boolean inRange = marks.compareTo(min) >= 0 && marks.compareTo(max) <= 0;
            if (inRange) {
                if (matched != null) {
                    throw new BadRequestException("Overlapping grading ranges detected for grade '"
                            + range.getGrade() + "'. Contact the Head of Department to fix the grading configuration.");
                }
                matched = new GradeCalculation(range.getGrade(), range.getGradePoint());
            }
        }
        if (matched == null) {
            throw new BadRequestException("Entered marks " + marks.stripTrailingZeros().toPlainString()
                    + " do not fall within any configured grading range for this subject. "
                    + "Contact the Head of Department to review the grading configuration.");
        }
        return matched;
    }

    private record GradeCalculation(String grade, BigDecimal gradePoint) {}
}
