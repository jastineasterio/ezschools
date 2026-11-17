package com.edumanage.resultsnotifications.dto;

import java.util.List;

public record ResultNotificationPreview(
        Long examinationId,
        String examinationName,
        Long classId,
        String className,
        Long streamId,
        String streamName,
        String recipientType,
        int eligibleCount,
        int excludedCount,
        List<StudentResultPreview> students,
        List<ParentGuardianPreview> parents,
        String sampleMessage,
        boolean canSend,
        String reason
) {
}
