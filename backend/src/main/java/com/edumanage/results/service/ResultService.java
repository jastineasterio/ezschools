package com.edumanage.results.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.results.dto.CreateResultRequest;
import com.edumanage.results.dto.ResultDto;
import com.edumanage.results.entity.Result;
import com.edumanage.results.repository.ResultRepository;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.examinationsubjects.repository.ExaminationSubjectRepository;
import com.edumanage.examinations.repository.ExaminationRepository;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.security.AccessGuard;
import com.edumanage.security.CurrentUserService;
import com.edumanage.teacherassignments.repository.TeacherAssignmentRepository;
import com.edumanage.enrollments.repository.StudentEnrollmentRepository;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    public ResultService(ResultRepository resultRepository,
                         StudentProfileRepository studentProfileRepository,
                         ExaminationSubjectRepository examinationSubjectRepository,
                         ExaminationRepository examinationRepository,
                         UserRepository userRepository,
                         AuditLogService auditLogService,
                         AccessGuard accessGuard,
                         CurrentUserService currentUserService,
                         TeacherAssignmentRepository teacherAssignmentRepository,
                         StudentEnrollmentRepository enrollmentRepository) {
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
    }

    public PageResponse<ResultDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<Result> page;
        if (tenantId != null) {
            page = resultRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = resultRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(result -> mapToDto(result, null, null, null, null, null, null)));
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
        return PageResponse.from(page.map(result -> mapToDto(result, student.getFirstName() + " " + student.getLastName(), student.getAdmissionNumber(), null, null, null, null)));
    }

    public PageResponse<ResultDto> listByExamination(Long examinationId, Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<Result> page = resultRepository.findByExaminationIdAndSchoolId(examinationId, tenantId != null ? tenantId : null, pageable);
        return PageResponse.from(page.map(result -> mapToDto(result, null, null, null, null, null, null)));
    }

    public ResultDto getById(Long id) {
        var result = resultRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Result not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(result.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(result, null, null, null, null, null, null);
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

        var existingResult = resultRepository.findByStudentIdAndExaminationSubjectId(request.getStudentId(), request.getExaminationSubjectId());
        if (existingResult.isPresent()) {
            throw new com.edumanage.exception.ConflictException("Result already exists for this student and examination subject");
        }

        Long teacherId = actor.getId();

        var teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));

        Result result = new Result();
        result.setSchoolId(tenantId);
        result.setStudentId(request.getStudentId());
        result.setExaminationSubjectId(request.getExaminationSubjectId());
        result.setExaminationId(examSubject.getExaminationId());
        result.setTeacherId(teacherId);
        result.setMarksObtained(request.getMarksObtained() != null ? new BigDecimal(request.getMarksObtained()) : null);
        result.setPercentage(toPercentage(request.getMarksObtained(), examSubject.getMaximumMarks()));
        result.setRemarks(request.getRemarks());
        result.setStatus("DRAFT");

        resultRepository.save(result);

        auditLogService.log("RESULT_CREATE", "Result", result.getId(),
                "Created result for student", null, null, null);

        return mapToDto(result, student.getFirstName() + " " + student.getLastName(), student.getAdmissionNumber(), examination.getName(), null, null, teacher.getFirstName() + " " + teacher.getLastName());
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

        result.setMarksObtained(request.getMarksObtained() != null ? new BigDecimal(request.getMarksObtained()) : null);
        result.setPercentage(toPercentage(request.getMarksObtained(), examSubject.getMaximumMarks()));
        result.setRemarks(request.getRemarks());

        resultRepository.save(result);

        auditLogService.log("RESULT_UPDATE", "Result", result.getId(),
                "Updated result", null, null, null);

        return mapToDto(result, student.getFirstName() + " " + student.getLastName(), student.getAdmissionNumber(), null, null, null, null);
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
        result.setSubmittedAt(java.time.LocalDateTime.now());
        resultRepository.save(result);

        auditLogService.log("RESULT_SUBMIT", "Result", result.getId(),
                "Submitted result", null, null, null);

        return mapToDto(result, null, null, null, null, null, null);
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

        result.setStatus("APPROVED");
        result.setApprovedAt(java.time.LocalDateTime.now());
        resultRepository.save(result);

        auditLogService.log("RESULT_APPROVE", "Result", result.getId(),
                "Approved result", null, null, null);

        return mapToDto(result, null, null, null, null, null, null);
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

        result.setStatus("REJECTED");
        result.setRemarks(comment);
        resultRepository.save(result);

        auditLogService.log("RESULT_REJECT", "Result", result.getId(),
                "Rejected result", null, null, null);

        return mapToDto(result, null, null, null, null, null, null);
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

        result.setStatus("PUBLISHED");
        result.setPublishedAt(java.time.LocalDateTime.now());
        resultRepository.save(result);

        auditLogService.log("RESULT_PUBLISH", "Result", result.getId(),
                "Published result", null, null, null);

        return mapToDto(result, null, null, null, null, null, null);
    }

    private ResultDto mapToDto(Result entity, String studentName, String admissionNumber,
                               String examinationName, String subjectName, String className, String teacherName) {
        ResultDto dto = new ResultDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setStudentId(entity.getStudentId());
        dto.setStudentName(studentName);
        dto.setAdmissionNumber(admissionNumber);
        dto.setExaminationSubjectId(entity.getExaminationSubjectId());
        dto.setExaminationId(entity.getExaminationId());
        dto.setExaminationName(examinationName);
        dto.setSubjectId(null);
        dto.setSubjectName(subjectName);
        dto.setClassId(null);
        dto.setClassName(className);
        dto.setTeacherId(entity.getTeacherId());
        dto.setTeacherName(teacherName);
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
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
