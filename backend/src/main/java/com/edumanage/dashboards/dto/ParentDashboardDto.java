package com.edumanage.dashboards.dto;

import com.edumanage.notifications.dto.NotificationDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Landing payload for the parent dashboard. The payload is always scoped to
 * the children linked to the authenticated parent account.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentDashboardDto {

    private Long parentId;
    private String parentName;
    private String email;
    private String phone;
    private Long schoolId;
    private String schoolName;
    private Long academicYearId;
    private String academicYearName;
    private boolean profileLinked;
    private int childCount;
    private BigDecimal totalOutstandingBalance = BigDecimal.ZERO;
    private long unreadNotificationCount;
    private List<Child> children = new ArrayList<>();
    private List<NotificationDto> notifications = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Child {
        private Long studentId;
        private String studentName;
        private String admissionNumber;
        private String className;
        private String streamName;
        private String academicYearName;
        private String enrollmentStatus;
        private String studentType;
        private String status;
        private AttendanceSummary attendance;
        private List<AttendanceRecord> recentAttendance = new ArrayList<>();
        private List<Result> recentResults = new ArrayList<>();
        private BigDecimal totalFeesDue = BigDecimal.ZERO;
        private BigDecimal totalFeesPaid = BigDecimal.ZERO;
        private BigDecimal balance = BigDecimal.ZERO;
        private List<FeeItem> feeItems = new ArrayList<>();
        private List<Payment> recentPayments = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceSummary {
        private long total;
        private long present;
        private long absent;
        private long late;
        private long excused;
        private Double attendanceRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceRecord {
        private LocalDate attendanceDate;
        private String status;
        private String className;
        private String subjectName;
        private String remarks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {
        private Long id;
        private String examinationName;
        private String subjectName;
        private Integer marksObtained;
        private Double percentage;
        private String grade;
        private String status;
        private String remarks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeeItem {
        private Long id;
        private String feeStructureName;
        private BigDecimal amountDue;
        private BigDecimal amountPaid;
        private BigDecimal balance;
        private LocalDate dueDate;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payment {
        private Long id;
        private BigDecimal amount;
        private String paymentMethod;
        private LocalDate paymentDate;
        private String receiptNumber;
        private String referenceNumber;
        private String status;
    }
}
