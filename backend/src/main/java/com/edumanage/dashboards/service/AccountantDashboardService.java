package com.edumanage.dashboards.service;

import com.edumanage.academicyears.entity.AcademicYear;
import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.dashboards.dto.AccountantDashboardDto;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.fees.dto.PaymentDto;
import com.edumanage.fees.entity.Payment;
import com.edumanage.fees.repository.FeeAssignmentRepository;
import com.edumanage.fees.repository.FeeStructureRepository;
import com.edumanage.fees.repository.PaymentRepository;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.security.AccessGuard;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the accountant dashboard.
 *
 * <p>The service only ever reports figures for the school of the authenticated
 * accountant. Financial endpoints are protected by {@link AccessGuard} so a
 * non financial role can not read the collected or outstanding amounts.</p>
 */
@Service
public class AccountantDashboardService {

    private static final int RECENT_LIMIT = 10;
    private static final int STRUCTURE_LIMIT = 25;
    private static final LocalDate EPOCH = LocalDate.of(1970, 1, 1);

    private final AccessGuard accessGuard;
    private final PaymentRepository paymentRepository;
    private final FeeAssignmentRepository feeAssignmentRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SchoolRepository schoolRepository;

    public AccountantDashboardService(AccessGuard accessGuard,
                                      PaymentRepository paymentRepository,
                                      FeeAssignmentRepository feeAssignmentRepository,
                                      FeeStructureRepository feeStructureRepository,
                                      AcademicYearRepository academicYearRepository,
                                      ClassRepository classRepository,
                                      StudentProfileRepository studentProfileRepository,
                                      SchoolRepository schoolRepository) {
        this.accessGuard = accessGuard;
        this.paymentRepository = paymentRepository;
        this.feeAssignmentRepository = feeAssignmentRepository;
        this.feeStructureRepository = feeStructureRepository;
        this.academicYearRepository = academicYearRepository;
        this.classRepository = classRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.schoolRepository = schoolRepository;
    }

    @Transactional(readOnly = true)
    public AccountantDashboardDto getDashboard() {
        accessGuard.assertFinancialAccess();

        Long schoolId = com.edumanage.tenant.TenantContext.requireTenantId();

        AccountantDashboardDto dto = new AccountantDashboardDto();
        dto.setSchoolId(schoolId);
        dto.setSchoolName(schoolRepository.findById(schoolId).map(school -> school.getName()).orElse(null));

        AcademicYear academicYear = academicYearRepository.findBySchoolIdAndIsCurrentTrueAndDeletedAtIsNull(schoolId).orElse(null);
        LocalDate fromDate = EPOCH;
        if (academicYear != null) {
            dto.setAcademicYearId(academicYear.getId());
            dto.setAcademicYearName(academicYear.getName());
            dto.setPeriodStart(academicYear.getStartDate());
            dto.setPeriodEnd(academicYear.getEndDate());
            fromDate = academicYear.getStartDate() == null ? EPOCH : academicYear.getStartDate();
        }

        dto.setTotalFeesBilled(orZero(feeAssignmentRepository.sumAmountDueSince(schoolId, fromDate)));
        dto.setTotalFeesCollected(orZero(paymentRepository.sumCompletedAmountSince(schoolId, fromDate)));
        dto.setTotalOutstanding(orZero(feeAssignmentRepository.sumBalanceSince(schoolId, fromDate)));
        dto.setPaymentCount(paymentRepository.countSince(schoolId, fromDate));
        dto.setStudentsWithOutstandingBalance(
                feeAssignmentRepository.countDistinctStudentsWithBalanceSince(schoolId, fromDate));
        dto.setPendingVerificationCount(paymentRepository.countUnverifiedBySchoolId(schoolId));
        dto.setPendingVerificationAmount(orZero(paymentRepository.sumUnverifiedAmountBySchoolId(schoolId)));

        dto.setPaymentsByDate(paymentRepository.summariseByPaymentDate(schoolId, fromDate).stream()
                .map(summary -> new AccountantDashboardDto.PaymentSummary(
                        summary.getPaymentDate() == null ? null : summary.getPaymentDate().toString(),
                        summary.getTotal() == null ? 0L : summary.getTotal(),
                        orZero(summary.getAmount())))
                .toList());

        dto.setPaymentsByMethod(paymentRepository.summariseByPaymentMethod(schoolId).stream()
                .map(summary -> new AccountantDashboardDto.PaymentSummary(
                        summary.getPaymentMethod() == null ? "UNKNOWN" : summary.getPaymentMethod(),
                        summary.getTotal() == null ? 0L : summary.getTotal(),
                        orZero(summary.getAmount())))
                .toList());

        dto.setRecentPayments(paymentRepository.findRecentBySchoolId(schoolId, PageRequest.of(0, RECENT_LIMIT)).stream()
                .map(this::mapPayment)
                .toList());

        dto.setPendingPayments(paymentRepository.findUnverifiedBySchoolId(schoolId, PageRequest.of(0, RECENT_LIMIT)).stream()
                .map(this::mapPayment)
                .toList());

        dto.setFeeStructures(buildFeeStructures(schoolId));
        return dto;
    }

    private List<AccountantDashboardDto.FeeStructureSummary> buildFeeStructures(Long schoolId) {
        Map<Long, FeeAssignmentRepository.FeeStructureTotals> totals = new HashMap<>();
        feeAssignmentRepository.summariseByFeeStructure(schoolId)
                .forEach(summary -> totals.put(summary.getFeeStructureId(), summary));

        List<AccountantDashboardDto.FeeStructureSummary> summaries = new ArrayList<>();
        feeStructureRepository.findBySchoolId(schoolId, PageRequest.of(0, STRUCTURE_LIMIT)).forEach(structure -> {
            AccountantDashboardDto.FeeStructureSummary summary = new AccountantDashboardDto.FeeStructureSummary();
            summary.setFeeStructureId(structure.getId());
            summary.setName(structure.getName());
            summary.setFeeType(structure.getFeeType());
            summary.setAmount(orZero(structure.getAmount()));
            summary.setStatus(structure.getStatus());
            summary.setClassName(structure.getClassId() == null ? null
                    : classRepository.findById(structure.getClassId()).map(clazz -> clazz.getName()).orElse(null));
            summary.setAcademicYearName(structure.getAcademicYearId() == null ? null
                    : academicYearRepository.findById(structure.getAcademicYearId()).map(year -> year.getName()).orElse(null));

            FeeAssignmentRepository.FeeStructureTotals structureTotals = totals.get(structure.getId());
            if (structureTotals != null) {
                summary.setAssignments(structureTotals.getAssignments() == null ? 0L : structureTotals.getAssignments());
                summary.setBilled(orZero(structureTotals.getBilled()));
                summary.setCollected(orZero(structureTotals.getCollected()));
                summary.setOutstanding(orZero(structureTotals.getOutstanding()));
            }
            summaries.add(summary);
        });
        return summaries;
    }

    private PaymentDto mapPayment(Payment payment) {
        PaymentDto dto = new PaymentDto();
        dto.setId(payment.getId());
        dto.setUuid(payment.getUuid() != null ? payment.getUuid().toString() : null);
        dto.setSchoolId(payment.getSchoolId());
        dto.setStudentId(payment.getStudentId());
        dto.setFeeAssignmentId(payment.getFeeAssignmentId());
        dto.setAmount(payment.getAmount());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setReferenceNumber(payment.getReferenceNumber());
        dto.setReceiptNumber(payment.getReceiptNumber());
        dto.setRemarks(payment.getRemarks());
        dto.setStatus(payment.getStatus());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setUpdatedAt(payment.getUpdatedAt());

        if (payment.getStudentId() != null) {
            studentProfileRepository.findById(payment.getStudentId()).ifPresent(student -> {
                dto.setStudentName(student.getFirstName() + " " + student.getLastName());
                dto.setAdmissionNumber(student.getAdmissionNumber());
            });
        }
        return dto;
    }

    private BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
