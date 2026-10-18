package com.edumanage.results.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private Long examinationSubjectId;
    private Long examinationId;
    private String examinationName;
    private Long subjectId;
    private String subjectName;
    private Long classId;
    private String className;
    private Long teacherId;
    private String teacherName;
    private Integer marksObtained;
    private Double percentage;
    private String grade;
    private Double gradePoint;
    private String remarks;
    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
