package com.edumanage.analytics.service;

import com.edumanage.analytics.dto.SchoolAnalyticsDto;
import com.edumanage.attendance.repository.AttendanceRepository;
import com.edumanage.examinations.repository.ExaminationRepository;
import com.edumanage.fees.repository.FeeAssignmentRepository;
import com.edumanage.fees.repository.PaymentRepository;
import com.edumanage.results.repository.ResultRepository;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.teacherprofiles.repository.TeacherProfileRepository;
import com.edumanage.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchoolAnalyticsService {

    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final AttendanceRepository attendanceRepository;
    private final ExaminationRepository examinationRepository;
    private final ResultRepository resultRepository;
    private final PaymentRepository paymentRepository;
    private final FeeAssignmentRepository feeAssignmentRepository;

    public SchoolAnalyticsService(StudentProfileRepository studentProfileRepository,
                                  TeacherProfileRepository teacherProfileRepository,
                                  AttendanceRepository attendanceRepository,
                                  ExaminationRepository examinationRepository,
                                  ResultRepository resultRepository,
                                  PaymentRepository paymentRepository,
                                  FeeAssignmentRepository feeAssignmentRepository) {
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.attendanceRepository = attendanceRepository;
        this.examinationRepository = examinationRepository;
        this.resultRepository = resultRepository;
        this.paymentRepository = paymentRepository;
        this.feeAssignmentRepository = feeAssignmentRepository;
    }

    public SchoolAnalyticsDto getSchoolAnalytics() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }

        long totalStudents = studentProfileRepository.countBySchoolIdAndDeletedAtIsNull(tenantId);
        long totalTeachers = teacherProfileRepository.countBySchoolIdAndDeletedAtIsNull(tenantId);

        var attendanceStats = getAttendanceStats(tenantId);
        var examStats = getExaminationStats(tenantId);
        var financeStats = getFinanceStats(tenantId);

        return new SchoolAnalyticsDto(
                totalStudents,
                totalTeachers,
                0,
                0,
                0,
                attendanceStats,
                examStats,
                financeStats,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private SchoolAnalyticsDto.AttendanceAnalytics getAttendanceStats(Long schoolId) {
        var today = LocalDate.now();
        var startOfMonth = today.withDayOfMonth(1);

        long presentCount = attendanceRepository.countBySchoolIdAndStatusAndAttendanceDateBetween(schoolId, "PRESENT", startOfMonth, today);
        long absentCount = attendanceRepository.countBySchoolIdAndStatusAndAttendanceDateBetween(schoolId, "ABSENT", startOfMonth, today);
        long lateCount = attendanceRepository.countBySchoolIdAndStatusAndAttendanceDateBetween(schoolId, "LATE", startOfMonth, today);

        long total = presentCount + absentCount + lateCount;
        double percentage = total > 0 ? (double) presentCount / total * 100 : 0;

        return new SchoolAnalyticsDto.AttendanceAnalytics(percentage, presentCount, absentCount, lateCount);
    }

    private SchoolAnalyticsDto.ExaminationAnalytics getExaminationStats(Long schoolId) {
        long totalExaminations = examinationRepository.countBySchoolIdAndDeletedAtIsNull(schoolId);
        long publishedResults = resultRepository.countBySchoolIdAndStatusAndDeletedAtIsNull(schoolId, "PUBLISHED");

        Double avgScore = resultRepository.findAverageScoreBySchoolId(schoolId);
        double average = avgScore != null ? avgScore : 0;

        return new SchoolAnalyticsDto.ExaminationAnalytics(totalExaminations, publishedResults, average);
    }

    private SchoolAnalyticsDto.FinanceAnalytics getFinanceStats(Long schoolId) {
        BigDecimal totalCollected = paymentRepository.sumAmountBySchoolId(schoolId);
        BigDecimal totalOutstanding = feeAssignmentRepository.sumBalanceBySchoolId(schoolId);

        return new SchoolAnalyticsDto.FinanceAnalytics(
                totalCollected != null ? totalCollected : BigDecimal.ZERO,
                totalOutstanding != null ? totalOutstanding : BigDecimal.ZERO,
                totalCollected != null ? totalCollected : BigDecimal.ZERO
        );
    }
}
