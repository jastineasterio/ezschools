package com.edumanage.reports.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportJobRequest {
    @NotBlank(message = "Report type is required")
    private String reportType;

    @NotBlank(message = "Report name is required")
    private String reportName;

    private String parameters;

    private Long academicYearId;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private Long classId;

    private Long streamId;

    private Long departmentId;

    private Long subjectId;

    private Long examinationId;

    private Long studentId;

    private String status;

    private String gender;

    private List<String> includeFields;
}
