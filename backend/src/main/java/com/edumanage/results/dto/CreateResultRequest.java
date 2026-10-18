package com.edumanage.results.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateResultRequest {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Examination subject ID is required")
    private Long examinationSubjectId;

    private Integer marksObtained;

    private String remarks;
}
