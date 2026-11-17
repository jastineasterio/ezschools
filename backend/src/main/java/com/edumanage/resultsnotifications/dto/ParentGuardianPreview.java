package com.edumanage.resultsnotifications.dto;

public record ParentGuardianPreview(
        Long parentId,
        String parentName,
        String phone,
        Long studentId,
        String studentName,
        String admissionNumber,
        String relationshipType,
        boolean eligible,
        String exclusionReason
) {
}
