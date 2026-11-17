package com.edumanage.fees.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Preview of a bulk fee assignment: how many eligible students will receive
 * which fee structures, and the resulting totals. No records are written.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkFeeAssignmentPreviewDto {

    private int eligibleStudentCount;
    private List<BulkAssignmentStudentPreview> students;
    private List<BulkAssignmentFeeStructurePreview> feeStructures;
    private long totalAssignments;
    private BigDecimal totalAmount;
    private LocalDate dueDate;
}
