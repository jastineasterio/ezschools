package com.edumanage.examinations.dto;

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
public class ExaminationDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long academicYearId;
    private String academicYearName;
    private String name;
    private Long typeId;
    private String typeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
