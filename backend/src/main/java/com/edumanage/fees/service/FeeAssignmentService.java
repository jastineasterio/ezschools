package com.edumanage.fees.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.fees.dto.CreateFeeAssignmentRequest;
import com.edumanage.fees.dto.FeeAssignmentDto;
import com.edumanage.fees.entity.FeeAssignment;
import com.edumanage.fees.entity.FeeStructure;
import com.edumanage.fees.repository.FeeAssignmentRepository;
import com.edumanage.fees.repository.FeeStructureRepository;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.security.AccessGuard;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class FeeAssignmentService {

    private final FeeAssignmentRepository feeAssignmentRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AuditLogService auditLogService;
    private final AccessGuard accessGuard;

    public FeeAssignmentService(FeeAssignmentRepository feeAssignmentRepository,
                                FeeStructureRepository feeStructureRepository,
                                StudentProfileRepository studentProfileRepository,
                                AuditLogService auditLogService,
                                AccessGuard accessGuard) {
        this.feeAssignmentRepository = feeAssignmentRepository;
        this.feeStructureRepository = feeStructureRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.auditLogService = auditLogService;
        this.accessGuard = accessGuard;
    }

    public PageResponse<FeeAssignmentDto> list(Pageable pageable) {
        accessGuard.assertFinancialAccess();
        Long tenantId = TenantContext.getTenantId();
        Page<FeeAssignment> page;
        if (tenantId != null) {
            page = feeAssignmentRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = feeAssignmentRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(this::mapToDto));
    }

    public List<FeeAssignmentDto> listByStudent(Long studentId) {
        accessGuard.assertFinancialAccess();
        Long tenantId = TenantContext.getTenantId();
        var student = studentProfileRepository.findById(studentId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (tenantId != null && !tenantId.equals(student.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        Long schoolId = tenantId != null ? tenantId : student.getSchoolId();
        return feeAssignmentRepository.findBySchoolIdAndStudentId(schoolId, studentId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public FeeAssignmentDto assign(CreateFeeAssignmentRequest request) {
        accessGuard.assertFinancialAccess();
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

        var feeStructure = feeStructureRepository.findById(request.getFeeStructureId())
                .filter(f -> !f.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Fee structure not found"));

        if (!tenantId.equals(feeStructure.getSchoolId())) {
            throw new ForbiddenException("Fee structure does not belong to this school");
        }

        if (feeAssignmentRepository.existsBySchoolIdAndStudentIdAndFeeStructureId(
                tenantId, request.getStudentId(), request.getFeeStructureId())) {
            throw new com.edumanage.exception.ConflictException("Fee already assigned to this student");
        }

        BigDecimal amountDue = feeStructure.getAmount();
        BigDecimal balance = amountDue;

        FeeAssignment assignment = new FeeAssignment();
        assignment.setSchoolId(tenantId);
        assignment.setStudentId(request.getStudentId());
        assignment.setFeeStructureId(request.getFeeStructureId());
        assignment.setAmountDue(amountDue);
        assignment.setAmountPaid(BigDecimal.ZERO);
        assignment.setBalance(balance);
        assignment.setDueDate(request.getDueDate() != null ? request.getDueDate() : feeStructure.getDueDate());
        assignment.setStatus("PENDING");
        assignment.setAssignedAt(LocalDate.now());

        feeAssignmentRepository.save(assignment);

        auditLogService.log("FEE_ASSIGN", "FeeAssignment", assignment.getId(),
                "Assigned fee " + feeStructure.getName() + " to student", null, null, null);

        return mapToDto(assignment);
    }

    @Transactional
    public void bulkAssign(Long feeStructureId, List<Long> studentIds) {
        accessGuard.assertFinancialAccess();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        var feeStructure = feeStructureRepository.findById(feeStructureId)
                .filter(f -> !f.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Fee structure not found"));

        if (!tenantId.equals(feeStructure.getSchoolId())) {
            throw new ForbiddenException("Fee structure does not belong to this school");
        }

        BigDecimal amountDue = feeStructure.getAmount();
        List<FeeAssignment> assignments = new ArrayList<>();

        for (Long studentId : studentIds) {
            var student = studentProfileRepository.findById(studentId)
                    .filter(s -> !s.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

            if (!tenantId.equals(student.getSchoolId())) {
                throw new ForbiddenException("Student does not belong to this school: " + studentId);
            }

            if (feeAssignmentRepository.existsBySchoolIdAndStudentIdAndFeeStructureId(tenantId, studentId, feeStructureId)) {
                continue;
            }

            FeeAssignment assignment = new FeeAssignment();
            assignment.setSchoolId(tenantId);
            assignment.setStudentId(studentId);
            assignment.setFeeStructureId(feeStructureId);
            assignment.setAmountDue(amountDue);
            assignment.setAmountPaid(BigDecimal.ZERO);
            assignment.setBalance(amountDue);
            assignment.setDueDate(feeStructure.getDueDate());
            assignment.setStatus("PENDING");
            assignment.setAssignedAt(LocalDate.now());
            assignments.add(assignment);
        }

        feeAssignmentRepository.saveAll(assignments);

        auditLogService.log("FEE_BULK_ASSIGN", "FeeAssignment", feeStructureId,
                "Bulk assigned fee to " + assignments.size() + " students", null, null, null);
    }

    private FeeAssignmentDto mapToDto(FeeAssignment entity) {
        FeeAssignmentDto dto = new FeeAssignmentDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setStudentId(entity.getStudentId());
        dto.setFeeStructureId(entity.getFeeStructureId());
        dto.setAmountDue(entity.getAmountDue());
        dto.setAmountPaid(entity.getAmountPaid());
        dto.setBalance(entity.getBalance());
        dto.setDueDate(entity.getDueDate());
        dto.setStatus(entity.getStatus());
        dto.setAssignedAt(entity.getAssignedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        if (entity.getStudentId() != null) {
            studentProfileRepository.findById(entity.getStudentId())
                    .filter(student -> !student.isDeleted())
                    .ifPresent(student -> {
                        dto.setStudentName(student.getFirstName() + " " + student.getLastName());
                        dto.setAdmissionNumber(student.getAdmissionNumber());
                    });
        }
        if (entity.getFeeStructureId() != null) {
            feeStructureRepository.findById(entity.getFeeStructureId())
                    .ifPresent(structure -> dto.setFeeStructureName(structure.getName()));
        }
        return dto;
    }
}
