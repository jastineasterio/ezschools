package com.edumanage.fees.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request for a bulk fee assignment operation. The accountant selects a scope
 * (whole class or a specific stream), optional academic year, one or more fee
 * structures and an optional due date. All records are created within the
 * authenticated accountant's school only.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkFeeAssignmentRequest {

    @NotNull(message = "Assignment scope is required")
    private String scope; // "CLASS" or "STREAM"

    @NotNull(message = "Class is required")
    private Long classId;

    private Long streamId;

    private Long academicYearId;

    @NotEmpty(message = "At least one fee structure is required")
    private List<Long> feeStructureIds;

    private LocalDate dueDate;
}
