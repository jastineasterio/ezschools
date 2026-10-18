package com.edumanage.gradingschemes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGradingSchemeRangeRequest {

    @NotNull(message = "Scheme ID is required")
    private Long schemeId;

    @NotBlank(message = "Grade is required")
    private String grade;

    @NotNull(message = "Minimum mark is required")
    private Integer minMark;

    @NotNull(message = "Maximum mark is required")
    private Integer maxMark;

    private Double gradePoint;

    private String remark;
}
