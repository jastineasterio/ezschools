package com.edumanage.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAnalyticsSnapshotRequest {
    @NotBlank(message = "Metric type is required")
    private String metricType;

    @NotBlank(message = "Metric value is required")
    private String metricValue;
}
