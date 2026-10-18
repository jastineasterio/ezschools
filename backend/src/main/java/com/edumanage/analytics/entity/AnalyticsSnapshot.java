package com.edumanage.analytics.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "analytics_snapshots")
public class AnalyticsSnapshot extends SoftDeletableEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "metric_type", nullable = false, length = 100)
    private String metricType;

    @Column(name = "metric_value", nullable = false, columnDefinition = "JSONB")
    private String metricValue;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    public Long getSchoolId() {
        return this.schoolId;
    }

    public LocalDate getSnapshotDate() {
        return this.snapshotDate;
    }

    public String getMetricType() {
        return this.metricType;
    }

    public String getMetricValue() {
        return this.metricValue;
    }

    public Long getCreatedBy() {
        return this.createdBy;
    }

    public Long getUpdatedBy() {
        return this.updatedBy;
    }

    public UUID getUuid() {
        return super.getUuid();
    }

    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
    }

    public void setSnapshotDate(LocalDate snapshotDate) {
        this.snapshotDate = snapshotDate;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public void setMetricValue(String metricValue) {
        this.metricValue = metricValue;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
