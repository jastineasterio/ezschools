package com.edumanage.examinations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateExaminationRequest {

    @NotNull(message = "Academic year ID is required")
    private Long academicYearId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Type ID is required")
    private Long typeId;

    private LocalDate startDate;

    private LocalDate endDate;

    private String status;

    private String description;
}
