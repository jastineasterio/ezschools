package com.edumanage.fees.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeStructureDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private String name;
    private String description;
    private BigDecimal amount;
    private LocalDate dueDate;
    private Long academicYearId;
    private Long classId;
    private String feeType;
    private Boolean isMandatory;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
