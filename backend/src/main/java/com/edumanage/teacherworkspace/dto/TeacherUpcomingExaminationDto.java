package com.edumanage.teacherworkspace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TeacherUpcomingExaminationDto {
    private Long examinationSubjectId;
    private Long examinationId;
    private String examinationName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String subjectName;
    private String className;
    private String streamName;
    private Long academicYearId;
    private BigDecimal maximumMarks;
}
