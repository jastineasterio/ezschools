package com.edumanage.results.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ResultAnalyticsRow {
    Long getResultId();

    Long getStudentId();

    String getAdmissionNumber();

    String getStudentName();

    Long getClassId();

    String getClassName();

    Long getStreamId();

    String getStreamName();

    Long getSubjectId();

    String getSubjectName();

    Long getExaminationId();

    String getExaminationName();

    Long getAcademicYearId();

    BigDecimal getMarksObtained();

    BigDecimal getPercentage();

    String getGrade();

    String getStatus();

    LocalDateTime getSubmittedAt();

    BigDecimal getPassMark();
}
