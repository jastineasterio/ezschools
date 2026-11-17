package com.edumanage.fees.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of a submitted bulk fee assignment: counts of assigned, skipped and
 * failed records plus a per-student, per-fee detail list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkFeeAssignmentResultDto {

    private int assigned;
    private int skipped;
    private int failed;
    private List<BulkAssignmentDetail> details;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkAssignmentDetail {
        private Long studentId;
        private String studentName;
        private String admissionNumber;
        private Long feeStructureId;
        private String feeStructureName;
        private String outcome; // ASSIGNED, SKIPPED, FAILED
        private String reason;
    }
}
