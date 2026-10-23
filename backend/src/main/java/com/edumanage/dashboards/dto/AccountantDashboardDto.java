package com.edumanage.dashboards.dto;

import com.edumanage.fees.dto.PaymentDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Landing payload for the accountant dashboard. All figures are derived from
 * the school of the authenticated accountant and never from another tenant.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountantDashboardDto {

    private Long schoolId;
    private String schoolName;
    private Long academicYearId;
    private String academicYearName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalFeesBilled = BigDecimal.ZERO;
    private BigDecimal totalFeesCollected = BigDecimal.ZERO;
    private BigDecimal totalOutstanding = BigDecimal.ZERO;
    private long paymentCount;
    private long studentsWithOutstandingBalance;
    private long pendingVerificationCount;
    private BigDecimal pendingVerificationAmount = BigDecimal.ZERO;
    private List<PaymentSummary> paymentsByDate = new ArrayList<>();
    private List<PaymentSummary> paymentsByMethod = new ArrayList<>();
    private List<PaymentDto> recentPayments = new ArrayList<>();
    private List<PaymentDto> pendingPayments = new ArrayList<>();
    private List<FeeStructureSummary> feeStructures = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSummary {
        private String label;
        private long total;
        private BigDecimal amount = BigDecimal.ZERO;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeeStructureSummary {
        private Long feeStructureId;
        private String name;
        private String feeType;
        private BigDecimal amount = BigDecimal.ZERO;
        private String status;
        private String className;
        private String academicYearName;
        private long assignments;
        private BigDecimal billed = BigDecimal.ZERO;
        private BigDecimal collected = BigDecimal.ZERO;
        private BigDecimal outstanding = BigDecimal.ZERO;
    }
}
