package com.edumanage.examinationreports.service;

import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.examinationreports.dto.ExaminationReportDto;
import com.edumanage.examinationreports.entity.ExaminationReport;
import com.edumanage.examinationreports.repository.ExaminationReportRepository;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.examinations.repository.ExaminationRepository;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.security.AccessGuard;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ExaminationReportService {

    private final ExaminationReportRepository examinationReportRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ExaminationRepository examinationRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final AccessGuard accessGuard;

    public ExaminationReportService(ExaminationReportRepository examinationReportRepository,
                                    StudentProfileRepository studentProfileRepository,
                                    ExaminationRepository examinationRepository,
                                    UserRepository userRepository,
                                    AuditLogService auditLogService,
                                    AccessGuard accessGuard) {
        this.examinationReportRepository = examinationReportRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.examinationRepository = examinationRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.accessGuard = accessGuard;
    }

    public PageResponse<ExaminationReportDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<ExaminationReport> page;
        if (tenantId != null) {
            page = examinationReportRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = examinationReportRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(report -> mapToDto(report, null, null, null, null)));
    }

    public PageResponse<ExaminationReportDto> listByStudent(Long studentId, Pageable pageable) {
        accessGuard.assertCanAccessStudent(studentId);
        var student = studentProfileRepository.findById(studentId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        Page<ExaminationReport> page = examinationReportRepository.findByStudentIdAndSchoolId(studentId, tenantId != null ? tenantId : null, pageable);
        return PageResponse.from(page.map(report -> mapToDto(report, student.getFirstName() + " " + student.getLastName(), student.getAdmissionNumber(), null, null)));
    }

    public ExaminationReportDto getById(Long id) {
        var report = examinationReportRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Examination report not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(report.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(report, null, null, null, null);
    }

    @Transactional
    public ExaminationReportDto generate(Long studentId, Long examinationId) {
        var student = studentProfileRepository.findById(studentId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        var examination = examinationRepository.findById(examinationId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found"));

        if (!tenantId.equals(examination.getSchoolId())) {
            throw new ForbiddenException("Examination does not belong to this school");
        }

        var existingReport = examinationReportRepository.findByStudentIdAndExaminationId(studentId, examinationId);
        if (existingReport.isPresent()) {
            throw new com.edumanage.exception.ConflictException("Report already generated for this student and examination");
        }

        var currentUser = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long generatedBy = null;
        if (currentUser != null && currentUser.isAuthenticated()) {
            var principal = currentUser.getPrincipal();
            if (principal instanceof com.edumanage.security.CustomUserDetails) {
                generatedBy = ((com.edumanage.security.CustomUserDetails) principal).id();
            }
        }

        if (generatedBy == null) {
            throw new ForbiddenException("User not authenticated");
        }

        ExaminationReport report = new ExaminationReport();
        report.setSchoolId(student.getSchoolId());
        report.setStudentId(studentId);
        report.setExaminationId(examinationId);
        report.setFileKey("reports/" + studentId + "/" + examinationId + "/" + System.currentTimeMillis() + ".pdf");
        report.setFilename("report_" + student.getAdmissionNumber() + "_" + examination.getName() + ".pdf");
        report.setMimeType("application/pdf");
        report.setSize(0L);
        report.setGeneratedBy(generatedBy);
        report.setGeneratedAt(LocalDateTime.now());

        examinationReportRepository.save(report);

        auditLogService.log("EXAMINATION_REPORT_GENERATE", "ExaminationReport", report.getId(),
                "Generated examination report for student", null, null, null);

        String generatedByUserName = userRepository.findById(generatedBy)
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse(null);

        return mapToDto(report, student.getFirstName() + " " + student.getLastName(), student.getAdmissionNumber(), examination.getName(), generatedByUserName);
    }

    private ExaminationReportDto mapToDto(ExaminationReport entity, String studentName,
                                          String admissionNumber, String examinationName, String generatedByUserName) {
        ExaminationReportDto dto = new ExaminationReportDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setStudentId(entity.getStudentId());
        dto.setStudentName(studentName);
        dto.setAdmissionNumber(admissionNumber);
        dto.setExaminationId(entity.getExaminationId());
        dto.setExaminationName(examinationName);
        dto.setFileKey(entity.getFileKey());
        dto.setFilename(entity.getFilename());
        dto.setMimeType(entity.getMimeType());
        dto.setSize(entity.getSize());
        dto.setGeneratedBy(entity.getGeneratedBy());
        dto.setGeneratedByUserName(generatedByUserName);
        dto.setGeneratedAt(entity.getGeneratedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
