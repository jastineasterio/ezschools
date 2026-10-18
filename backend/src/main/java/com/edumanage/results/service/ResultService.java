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
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ResultService {

    private final ResultRepository resultRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ExaminationSubjectRepository examinationSubjectRepository;
    private final ExaminationRepository examinationRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ResultService(ResultRepository resultRepository,
                         StudentProfileRepository studentProfileRepository,
                         ExaminationSubjectRepository examinationSubjectRepository,
                         ExaminationRepository examinationRepository,
                         UserRepository userRepository,
                         AuditLogService auditLogService) {
        this.resultRepository = resultRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.examinationSubjectRepository = examinationSubjectRepository;
        this.examinationRepository = examinationRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
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
        var student = studentProfileRepository.findById(studentId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        Page<Result> page = resultRepository.findByStudentIdAndSchoolId(studentId, tenantId != null ? tenantId : null, pageable);
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

        var existingResult = resultRepository.findByStudentIdAndExaminationSubjectId(request.getStudentId(), request.getExaminationSubjectId());
        if (existingResult.isPresent()) {
            throw new com.edumanage.exception.ConflictException("Result already exists for this student and examination subject");
        }

        var currentUser = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long teacherId = null;
        if (currentUser != null && currentUser.isAuthenticated()) {
            var principal = currentUser.getPrincipal();
            if (principal instanceof com.edumanage.security.CustomUserDetails) {
                teacherId = ((com.edumanage.security.CustomUserDetails) principal).id();
            }
        }

        if (teacherId == null) {
            throw new ForbiddenException("Teacher not authenticated");
        }

        var teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));

        Result result = new Result();
        result.setSchoolId(tenantId);
        result.setStudentId(request.getStudentId());
        result.setExaminationSubjectId(request.getExaminationSubjectId());
        result.setExaminationId(examSubject.getExaminationId());
        result.setTeacherId(teacherId);
        result.setMarksObtained(request.getMarksObtained() != null ? new BigDecimal(request.getMarksObtained()) : null);
        result.setPercentage(request.getMarksObtained() != null ? new BigDecimal(request.getMarksObtained()) : null);
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

        var student = studentProfileRepository.findById(request.getStudentId())
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        result.setMarksObtained(request.getMarksObtained() != null ? new BigDecimal(request.getMarksObtained()) : null);
        result.setPercentage(request.getMarksObtained() != null ? new BigDecimal(request.getMarksObtained()) : null);
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

        result.setStatus("SUBMITTED");
        result.setSubmittedAt(java.time.LocalDateTime.now());
        resultRepository.save(result);

        auditLogService.log("RESULT_SUBMIT", "Result", result.getId(),
                "Submitted result", null, null, null);

        return mapToDto(result, null, null, null, null, null, null);
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
