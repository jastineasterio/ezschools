package com.edumanage.transport.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "routes")
public class Route extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "start_point", nullable = false, length = 255)
    private String startPoint;

    @Column(name = "end_point", nullable = false, length = 255)
    private String endPoint;

    @Column(name = "distance_km", precision = 8, scale = 2)
    private java.math.BigDecimal distanceKm;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "morning_departure")
    private java.time.LocalTime morningDeparture;

    @Column(name = "afternoon_departure")
    private java.time.LocalTime afternoonDeparture;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
