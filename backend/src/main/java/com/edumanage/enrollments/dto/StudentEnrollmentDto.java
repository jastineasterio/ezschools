package com.edumanage.enrollments.dto;

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
public class StudentEnrollmentDto {
    private Long id;
    private String uuid;
    private Long studentId;
    private String studentName;
    private Long schoolId;
    private Long academicYearId;
    private String academicYearName;
    private Long classId;
    private String className;
    private Long streamId;
    private String streamName;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
