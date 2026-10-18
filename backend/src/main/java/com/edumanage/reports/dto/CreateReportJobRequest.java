package com.edumanage.reports.dto;

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
}
