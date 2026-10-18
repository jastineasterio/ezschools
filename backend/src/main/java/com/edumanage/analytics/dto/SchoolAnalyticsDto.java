package com.edumanage.analytics.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class SchoolAnalyticsDto {
    private long totalStudents;
    private long totalTeachers;
    private long totalParents;
    private long totalClasses;
    private long totalSubjects;
    private AttendanceAnalytics attendance;
    private ExaminationAnalytics examinations;
    private FinanceAnalytics finance;
    private List<Map<String, Object>> studentGrowth;
    private List<Map<String, Object>> classPerformance;

    public SchoolAnalyticsDto() {
    }

    public SchoolAnalyticsDto(long totalStudents, long totalTeachers, long totalParents, long totalClasses, long totalSubjects, AttendanceAnalytics attendance, ExaminationAnalytics examinations, FinanceAnalytics finance, List<Map<String, Object>> studentGrowth, List<Map<String, Object>> classPerformance) {
        this.totalStudents = totalStudents;
        this.totalTeachers = totalTeachers;
        this.totalParents = totalParents;
        this.totalClasses = totalClasses;
        this.totalSubjects = totalSubjects;
        this.attendance = attendance;
        this.examinations = examinations;
        this.finance = finance;
        this.studentGrowth = studentGrowth;
        this.classPerformance = classPerformance;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalTeachers() {
        return totalTeachers;
    }

    public void setTotalTeachers(long totalTeachers) {
        this.totalTeachers = totalTeachers;
    }

    public long getTotalParents() {
        return totalParents;
    }

    public void setTotalParents(long totalParents) {
        this.totalParents = totalParents;
    }

    public long getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(long totalClasses) {
        this.totalClasses = totalClasses;
    }

    public long getTotalSubjects() {
        return totalSubjects;
    }

    public void setTotalSubjects(long totalSubjects) {
        this.totalSubjects = totalSubjects;
    }

    public AttendanceAnalytics getAttendance() {
        return attendance;
    }

    public void setAttendance(AttendanceAnalytics attendance) {
        this.attendance = attendance;
    }

    public ExaminationAnalytics getExaminations() {
        return examinations;
    }

    public void setExaminations(ExaminationAnalytics examinations) {
        this.examinations = examinations;
    }

    public FinanceAnalytics getFinance() {
        return finance;
    }

    public void setFinance(FinanceAnalytics finance) {
        this.finance = finance;
    }

    public List<Map<String, Object>> getStudentGrowth() {
        return studentGrowth;
    }

    public void setStudentGrowth(List<Map<String, Object>> studentGrowth) {
        this.studentGrowth = studentGrowth;
    }

    public List<Map<String, Object>> getClassPerformance() {
        return classPerformance;
    }

    public void setClassPerformance(List<Map<String, Object>> classPerformance) {
        this.classPerformance = classPerformance;
    }

    public static class AttendanceAnalytics {
        private double attendancePercentage;
        private long presentCount;
        private long absentCount;
        private long lateCount;

        public AttendanceAnalytics() {
        }

        public AttendanceAnalytics(double attendancePercentage, long presentCount, long absentCount, long lateCount) {
            this.attendancePercentage = attendancePercentage;
            this.presentCount = presentCount;
            this.absentCount = absentCount;
            this.lateCount = lateCount;
        }

        public double getAttendancePercentage() {
            return attendancePercentage;
        }

        public void setAttendancePercentage(double attendancePercentage) {
            this.attendancePercentage = attendancePercentage;
        }

        public long getPresentCount() {
            return presentCount;
        }

        public void setPresentCount(long presentCount) {
            this.presentCount = presentCount;
        }

        public long getAbsentCount() {
            return absentCount;
        }

        public void setAbsentCount(long absentCount) {
            this.absentCount = absentCount;
        }

        public long getLateCount() {
            return lateCount;
        }

        public void setLateCount(long lateCount) {
            this.lateCount = lateCount;
        }
    }

    public static class ExaminationAnalytics {
        private long totalExaminations;
        private long publishedResults;
        private double averageScore;

        public ExaminationAnalytics() {
        }

        public ExaminationAnalytics(long totalExaminations, long publishedResults, double averageScore) {
            this.totalExaminations = totalExaminations;
            this.publishedResults = publishedResults;
            this.averageScore = averageScore;
        }

        public long getTotalExaminations() {
            return totalExaminations;
        }

        public void setTotalExaminations(long totalExaminations) {
            this.totalExaminations = totalExaminations;
        }

        public long getPublishedResults() {
            return publishedResults;
        }

        public void setPublishedResults(long publishedResults) {
            this.publishedResults = publishedResults;
        }

        public double getAverageScore() {
            return averageScore;
        }

        public void setAverageScore(double averageScore) {
            this.averageScore = averageScore;
        }
    }

    public static class FinanceAnalytics {
        private BigDecimal totalFeeCollected;
        private BigDecimal totalOutstanding;
        private BigDecimal totalPayments;

        public FinanceAnalytics() {
        }

        public FinanceAnalytics(BigDecimal totalFeeCollected, BigDecimal totalOutstanding, BigDecimal totalPayments) {
            this.totalFeeCollected = totalFeeCollected;
            this.totalOutstanding = totalOutstanding;
            this.totalPayments = totalPayments;
        }

        public BigDecimal getTotalFeeCollected() {
            return totalFeeCollected;
        }

        public void setTotalFeeCollected(BigDecimal totalFeeCollected) {
            this.totalFeeCollected = totalFeeCollected;
        }

        public BigDecimal getTotalOutstanding() {
            return totalOutstanding;
        }

        public void setTotalOutstanding(BigDecimal totalOutstanding) {
            this.totalOutstanding = totalOutstanding;
        }

        public BigDecimal getTotalPayments() {
            return totalPayments;
        }

        public void setTotalPayments(BigDecimal totalPayments) {
            this.totalPayments = totalPayments;
        }
    }
}
