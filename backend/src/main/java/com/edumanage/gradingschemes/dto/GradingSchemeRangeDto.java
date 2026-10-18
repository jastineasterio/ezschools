package com.edumanage.gradingschemes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradingSchemeRangeDto {
    private Long id;
    private String uuid;
    private Long schemeId;
    private String grade;
    private Integer minMark;
    private Integer maxMark;
    private Double gradePoint;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
