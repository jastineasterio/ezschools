package com.edumanage.analytics.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AnalyticsSnapshotDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private LocalDate snapshotDate;
    private String metricType;
    private String metricValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AnalyticsSnapshotDto() {
    }

    public AnalyticsSnapshotDto(Long id, String uuid, Long schoolId, LocalDate snapshotDate, String metricType, String metricValue, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.uuid = uuid;
        this.schoolId = schoolId;
        this.snapshotDate = snapshotDate;
        this.metricType = metricType;
        this.metricValue = metricValue;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(LocalDate snapshotDate) {
        this.snapshotDate = snapshotDate;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public String getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(String metricValue) {
        this.metricValue = metricValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
