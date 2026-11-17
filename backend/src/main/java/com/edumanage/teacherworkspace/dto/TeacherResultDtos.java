package com.edumanage.teacherworkspace.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data transfer objects for the teacher examination results workflow.
 */
public class TeacherResultDtos {

    public record ClassOption(Long id, String name, Long academicYearId, String academicYearName) {}

    public record StreamOption(Long id, String name) {}

    public record StudentOption(Long id, String admissionNumber, String fullName) {}

    public record SubjectOption(Long id, String name, String code) {}

    public record ExaminationOption(
            Long id,
            Long examinationSubjectId,
            String examinationName,
            String typeName,
            Long academicYearId,
            String academicYearName,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            BigDecimal maximumMarks,
            BigDecimal passMark,
            Long subjectId,
            Long classId,
            Long streamId,
            Long gradingSchemeId
    ) {}

    public record GradingRangeOption(String grade, BigDecimal minMark, BigDecimal maxMark,
                                     BigDecimal gradePoint, String remark) {}

    public record ResultRecord(
            Long id,
            Long studentId,
            String studentName,
            String admissionNumber,
            Long examinationId,
            String examinationName,
            Long subjectId,
            String subjectName,
            Long classId,
            String className,
            Long streamId,
            String streamName,
            Long academicYearId,
            String academicYearName,
            Integer marksObtained,
            BigDecimal maximumMarks,
            String grade,
            BigDecimal gradePoint,
            Double percentage,
            String status,
            LocalDateTime createdAt
    ) {}
}
