package com.edumanage.fees.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.fees.dto.BulkAssignmentFeeStructurePreview;
import com.edumanage.fees.dto.BulkAssignmentStudentPreview;
import com.edumanage.fees.dto.BulkFeeAssignmentPreviewDto;
import com.edumanage.fees.dto.BulkFeeAssignmentRequest;
import com.edumanage.fees.dto.BulkFeeAssignmentResultDto;
import com.edumanage.fees.dto.CreateFeeAssignmentRequest;
import com.edumanage.fees.dto.FeeAssignmentDto;
import com.edumanage.fees.entity.FeeAssignment;
import com.edumanage.fees.entity.FeeStructure;
import com.edumanage.fees.repository.FeeAssignmentRepository;
import com.edumanage.fees.repository.FeeStructureRepository;
import com.edumanage.academicyears.entity.AcademicYear;
import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.classes.entity.Class;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.streams.entity.Stream;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.enrollments.entity.StudentEnrollment;
import com.edumanage.enrollments.repository.StudentEnrollmentRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FeeAssignmentService {

    private static final int SAVE_CHUNK = 500;

    private final FeeAssignmentRepository feeAssignmentRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final ClassRepository classRepository;
    private final StreamRepository streamRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AuditLogService auditLogService;
    private final AccessGuard accessGuard;

    public FeeAssignmentService(FeeAssignmentRepository feeAssignmentRepository,
                                FeeStructureRepository feeStructureRepository,
                                StudentProfileRepository studentProfileRepository,
                                StudentEnrollmentRepository studentEnrollmentRepository,
                                ClassRepository classRepository,
                                StreamRepository streamRepository,
                                AcademicYearRepository academicYearRepository,
                                AuditLogService auditLogService,
                                AccessGuard accessGuard) {
        this.feeAssignmentRepository = feeAssignmentRepository;
        this.feeStructureRepository = feeStructureRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.classRepository = classRepository;
        this.streamRepository = streamRepository;
        this.academicYearRepository = academicYearRepository;
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

    public BulkFeeAssignmentPreviewDto previewBulkAssignment(BulkFeeAssignmentRequest request) {
        accessGuard.assertFinancialAccess();
        Long schoolId = TenantContext.requireTenantId();

        validateScope(request);
        List<FeeStructure> feeStructures = validateFeeStructures(schoolId, request.getFeeStructureIds());
        List<StudentProfile> students = findEligibleStudents(schoolId, request);

        Set<String> alreadyAssigned = alreadyAssignedKeys(schoolId,
                students.stream().map(StudentProfile::getId).toList(),
                request.getFeeStructureIds());

        List<BulkAssignmentFeeStructurePreview> structurePreviews = feeStructures.stream()
                .map(fs -> {
                    long newlyAssigned = students.stream()
                            .filter(s -> !alreadyAssigned.contains(s.getId() + ":" + fs.getId()))
                            .count();
                    return new BulkAssignmentFeeStructurePreview(
                            fs.getId(),
                            fs.getName(),
                            fs.getAmount(),
                            request.getDueDate() != null ? request.getDueDate() : fs.getDueDate());
                })
                .toList();

        long totalAssignments = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (BulkAssignmentFeeStructurePreview sp : structurePreviews) {
            long newlyAssigned = students.stream()
                    .filter(s -> !alreadyAssigned.contains(s.getId() + ":" + sp.getFeeStructureId()))
                    .count();
            totalAssignments += newlyAssigned;
            totalAmount = totalAmount.add(sp.getAmount().multiply(BigDecimal.valueOf(newlyAssigned)));
        }

        List<BulkAssignmentStudentPreview> studentPreviews = students.stream()
                .limit(50)
                .map(this::toStudentPreview)
                .toList();

        BulkFeeAssignmentPreviewDto preview = new BulkFeeAssignmentPreviewDto();
        preview.setEligibleStudentCount(students.size());
        preview.setStudents(studentPreviews);
        preview.setFeeStructures(structurePreviews);
        preview.setTotalAssignments(totalAssignments);
        preview.setTotalAmount(totalAmount);
        preview.setDueDate(request.getDueDate());
        return preview;
    }

    @Transactional
    public BulkFeeAssignmentResultDto bulkAssign(BulkFeeAssignmentRequest request) {
        accessGuard.assertFinancialAccess();
        Long schoolId = TenantContext.requireTenantId();

        validateScope(request);
        List<FeeStructure> feeStructures = validateFeeStructures(schoolId, request.getFeeStructureIds());
        List<StudentProfile> students = findEligibleStudents(schoolId, request);

        List<Long> studentIds = students.stream().map(StudentProfile::getId).toList();
        List<Long> feeStructureIds = feeStructures.stream().map(FeeStructure::getId).toList();

        Map<Long, FeeStructure> structureById = feeStructures.stream()
                .collect(Collectors.toMap(FeeStructure::getId, Function.identity()));

        Set<String> alreadyAssigned = alreadyAssignedKeys(schoolId, studentIds, feeStructureIds);

        List<BulkFeeAssignmentResultDto.BulkAssignmentDetail> details = new ArrayList<>();
        List<FeeAssignment> assignments = new ArrayList<>();
        int skipped = 0;
        int failed = 0;

        LocalDate now = LocalDate.now();

        for (StudentProfile student : students) {
            for (FeeStructure feeStructure : feeStructures) {
                String key = student.getId() + ":" + feeStructure.getId();
                if (alreadyAssigned.contains(key)) {
                    skipped++;
                    details.add(new BulkFeeAssignmentResultDto.BulkAssignmentDetail(
                            student.getId(),
                            student.getFirstName() + " " + student.getLastName(),
                            student.getAdmissionNumber(),
                            feeStructure.getId(),
                            feeStructure.getName(),
                            "SKIPPED",
                            "Fee already assigned to this student"));
                    continue;
                }

                try {
                    FeeAssignment assignment = new FeeAssignment();
                    assignment.setSchoolId(schoolId);
                    assignment.setStudentId(student.getId());
                    assignment.setFeeStructureId(feeStructure.getId());
                    assignment.setAmountDue(feeStructure.getAmount());
                    assignment.setAmountPaid(BigDecimal.ZERO);
                    assignment.setBalance(feeStructure.getAmount());
                    assignment.setDueDate(request.getDueDate() != null ? request.getDueDate() : feeStructure.getDueDate());
                    assignment.setStatus("PENDING");
                    assignment.setAssignedAt(now);
                    assignments.add(assignment);

                    alreadyAssigned.add(key); // prevent duplicates within this same request
                } catch (Exception ex) {
                    failed++;
                    details.add(new BulkFeeAssignmentResultDto.BulkAssignmentDetail(
                            student.getId(),
                            student.getFirstName() + " " + student.getLastName(),
                            student.getAdmissionNumber(),
                            feeStructure.getId(),
                            feeStructure.getName(),
                            "FAILED",
                            ex.getMessage()));
                }
            }
        }

        // Persist in chunks to handle large classes efficiently.
        for (int i = 0; i < assignments.size(); i += SAVE_CHUNK) {
            feeAssignmentRepository.saveAll(assignments.subList(i, Math.min(i + SAVE_CHUNK, assignments.size())));
        }

        int assigned = assignments.size();

        auditLogService.log("FEE_BULK_ASSIGN", "FeeAssignment", null,
                "Bulk assigned " + assigned + " fee records (" + skipped + " skipped, " + failed + " failed) "
                        + "for scope=" + request.getScope() + " class=" + request.getClassId()
                        + (request.getStreamId() != null ? " stream=" + request.getStreamId() : ""),
                null, null, null);

        BulkFeeAssignmentResultDto result = new BulkFeeAssignmentResultDto();
        result.setAssigned(assigned);
        result.setSkipped(skipped);
        result.setFailed(failed);
        // Keep the response payload bounded: only return skipped/failed details.
        result.setDetails(details.stream()
                .filter(d -> !"ASSIGNED".equals(d.getOutcome()))
                .limit(200)
                .toList());
        return result;
    }

    private void validateScope(BulkFeeAssignmentRequest request) {
        if (!"CLASS".equals(request.getScope()) && !"STREAM".equals(request.getScope())) {
            throw new BadRequestException("Scope must be CLASS or STREAM");
        }
        if ("STREAM".equals(request.getScope()) && request.getStreamId() == null) {
            throw new BadRequestException("Stream is required when scope is STREAM");
        }
    }

    private List<FeeStructure> validateFeeStructures(Long schoolId, List<Long> feeStructureIds) {
        List<FeeStructure> structures = new ArrayList<>();
        for (Long id : feeStructureIds) {
            var feeStructure = feeStructureRepository.findBySchoolIdAndId(schoolId, id)
                    .filter(f -> !f.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Fee structure not found: " + id));
            structures.add(feeStructure);
        }
        return structures;
    }

    private List<StudentProfile> findEligibleStudents(Long schoolId, BulkFeeAssignmentRequest request) {
        Long classId = request.getClassId();

        var clazz = classRepository.findBySchoolIdAndId(schoolId, classId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        if ("STREAM".equals(request.getScope()) && request.getStreamId() != null) {
            var stream = streamRepository.findBySchoolIdAndId(schoolId, request.getStreamId())
                    .filter(s -> !s.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Stream not found"));
            if (!stream.getClassId().equals(classId)) {
                throw new BadRequestException("Stream does not belong to the selected class");
            }
        }

        if (request.getAcademicYearId() != null) {
            academicYearRepository.findById(request.getAcademicYearId())
                    .filter(a -> !a.isDeleted() && schoolId.equals(a.getSchoolId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        }

        List<StudentEnrollment> enrollments;
        if (request.getAcademicYearId() != null) {
            enrollments = studentEnrollmentRepository
                    .findBySchoolIdAndClassIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(
                            schoolId, classId, request.getAcademicYearId(), "ENROLLED");
        } else {
            enrollments = studentEnrollmentRepository.findBySchoolIdAndClassId(schoolId, classId);
        }

        Set<Long> studentIds = enrollments.stream()
                .filter(e -> !e.isDeleted() && "ENROLLED".equals(e.getStatus()))
                .filter(e -> request.getStreamId() == null || request.getStreamId().equals(e.getStreamId()))
                .map(StudentEnrollment::getStudentId)
                .distinct()
                .collect(Collectors.toSet());

        if (studentIds.isEmpty()) {
            return List.of();
        }

        return studentProfileRepository.findBySchoolIdAndIdIn(schoolId, new ArrayList<>(studentIds)).stream()
                .filter(s -> !s.isDeleted() && "ACTIVE".equals(s.getStatus()))
                .collect(Collectors.toList());
    }

    private Set<String> alreadyAssignedKeys(Long schoolId, List<Long> studentIds, List<Long> feeStructureIds) {
        if (studentIds.isEmpty() || feeStructureIds.isEmpty()) {
            return Set.of();
        }
        return feeAssignmentRepository.findBySchoolIdAndStudentIdInAndFeeStructureIdIn(
                schoolId, studentIds, feeStructureIds).stream()
                .map(a -> a.getStudentId() + ":" + a.getFeeStructureId())
                .collect(Collectors.toSet());
    }

    private BulkAssignmentStudentPreview toStudentPreview(StudentProfile student) {
        BulkAssignmentStudentPreview preview = new BulkAssignmentStudentPreview();
        preview.setStudentId(student.getId());
        preview.setAdmissionNumber(student.getAdmissionNumber());
        preview.setFirstName(student.getFirstName());
        preview.setLastName(student.getLastName());
        preview.setName(student.getFirstName() + " " + student.getLastName());
        return preview;
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
