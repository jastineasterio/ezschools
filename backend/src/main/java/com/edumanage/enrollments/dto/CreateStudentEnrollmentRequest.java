package com.edumanage.enrollments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateStudentEnrollmentRequest {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Academic year ID is required")
    private Long academicYearId;

    @NotNull(message = "Class ID is required")
    private Long classId;

    private Long streamId;

    private String status;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;
}
