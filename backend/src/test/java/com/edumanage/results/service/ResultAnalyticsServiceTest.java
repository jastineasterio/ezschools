package com.edumanage.results.service;

import com.edumanage.departments.repository.DepartmentRepository;
import com.edumanage.results.dto.ResultAnalyticsDto;
import com.edumanage.results.dto.ResultAnalyticsRow;
import com.edumanage.results.repository.ResultRepository;
import com.edumanage.security.CurrentUserService;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.tenant.TenantContext;
import com.edumanage.users.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultAnalyticsServiceTest {

    @Mock private ResultRepository resultRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private CurrentUserService currentUserService;
    @InjectMocks private ResultAnalyticsService service;

    private final Long schoolId = 7L;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(schoolId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void schoolAnalyticsComputesAggregates() {
        ResultAnalyticsRow row1 = row(1L, 10L, "ADM1", "Alice", 1L, "Class 1", null, null,
                1L, "Math", 100L, "Exam 1", 1L, new BigDecimal("80"), new BigDecimal("80"), "A", "SUBMITTED", new BigDecimal("50"));
        ResultAnalyticsRow row2 = row(2L, 11L, "ADM2", "Bob", 1L, "Class 1", null, null,
                1L, "Math", 100L, "Exam 1", 1L, new BigDecimal("60"), new BigDecimal("60"), "C", "SUBMITTED", new BigDecimal("50"));
        when(resultRepository.findResultsForAnalytics(eq(schoolId), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(row1, row2));

        ResultAnalyticsDto dto = service.getSchoolAnalytics(null, null, null, null, null, null, null);

        assertEquals(2, dto.getTotalStudentsAssessed());
        assertEquals(70.0, dto.getOverallAverage());
        assertEquals(80.0, dto.getHighestScore());
        assertEquals(60.0, dto.getLowestScore());
        assertEquals(100.0, dto.getPassRate());
        assertEquals(0.0, dto.getFailRate());
        assertTrue(dto.getGradeDistribution().stream().anyMatch(g -> "A".equals(g.getGrade())));
    }

    @Test
    void departmentAnalyticsReturnsEmptyWhenUserHeadsNoDepartment() {
        User user = new User();
        user.setId(99L);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(departmentRepository.findBySchoolIdAndHeadOfDepartmentId(schoolId, 99L))
                .thenReturn(List.of());

        ResultAnalyticsDto dto = service.getDepartmentAnalytics(null, null, null, null, null, null);
        assertEquals(0, dto.getTotalStudentsAssessed());
    }

    private static ResultAnalyticsRow row(Long resultId, Long studentId, String admissionNumber,
                                          String studentName, Long classId, String className,
                                          Long streamId, String streamName, Long subjectId,
                                          String subjectName, Long examinationId, String examinationName,
                                          Long academicYearId, BigDecimal marksObtained,
                                          BigDecimal percentage, String grade, String status,
                                          BigDecimal passMark) {
        return new ResultAnalyticsRow() {
            public Long getResultId() { return resultId; }
            public Long getStudentId() { return studentId; }
            public String getAdmissionNumber() { return admissionNumber; }
            public String getStudentName() { return studentName; }
            public Long getClassId() { return classId; }
            public String getClassName() { return className; }
            public Long getStreamId() { return streamId; }
            public String getStreamName() { return streamName; }
            public Long getSubjectId() { return subjectId; }
            public String getSubjectName() { return subjectName; }
            public Long getExaminationId() { return examinationId; }
            public String getExaminationName() { return examinationName; }
            public Long getAcademicYearId() { return academicYearId; }
            public BigDecimal getMarksObtained() { return marksObtained; }
            public BigDecimal getPercentage() { return percentage; }
            public String getGrade() { return grade; }
            public String getStatus() { return status; }
            public LocalDateTime getSubmittedAt() { return null; }
            public BigDecimal getPassMark() { return passMark; }
        };
    }
}
