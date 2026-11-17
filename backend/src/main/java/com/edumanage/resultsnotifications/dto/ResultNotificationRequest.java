package com.edumanage.resultsnotifications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultNotificationRequest {

    @NotNull(message = "Examination ID is required")
    private Long examinationId;

    @NotNull(message = "Class ID is required")
    private Long classId;

    @NotNull(message = "Stream ID is required")
    private Long streamId;

    @NotBlank(message = "Recipient type is required")
    @Size(max = 20)
    private String recipientType;

    @Size(max = 100)
    private String idempotencyKey;
}
