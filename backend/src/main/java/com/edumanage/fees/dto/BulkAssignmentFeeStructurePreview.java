package com.edumanage.fees.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single fee structure returned in a bulk assignment preview.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkAssignmentFeeStructurePreview {

    private Long feeStructureId;
    private String name;
    private BigDecimal amount;
    private LocalDate dueDate;
}
