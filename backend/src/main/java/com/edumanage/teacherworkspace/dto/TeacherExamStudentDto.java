package com.edumanage.teacherworkspace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TeacherExamStudentDto {
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private Long resultId;
    private BigDecimal marksObtained;
    private String resultStatus;
    private boolean editable;
}
