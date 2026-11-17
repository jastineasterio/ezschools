package com.edumanage.resultsnotifications.dto;

import java.math.BigDecimal;

public record StudentResultPreview(
        Long resultId,
        Long studentId,
        String studentName,
        String admissionNumber,
        Long classId,
        String className,
        Long streamId,
        String streamName,
        Long subjectId,
        String subjectName,
        Long examinationSubjectId,
        Integer marksObtained,
        BigDecimal maximumMarks,
        String grade,
        String status,
        String phone,
        boolean eligible,
        String exclusionReason
) {
}
