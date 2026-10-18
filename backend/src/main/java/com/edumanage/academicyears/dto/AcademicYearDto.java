package com.edumanage.academicyears.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYearDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private boolean isCurrent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
