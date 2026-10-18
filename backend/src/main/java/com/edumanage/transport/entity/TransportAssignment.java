package com.edumanage.transport.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "transport_assignments")
public class TransportAssignment extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "route_id", nullable = false, insertable = false, updatable = false)
    private Long routeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id", nullable = false)
    private RouteStop stop;

    @Column(name = "stop_id", nullable = false, insertable = false, updatable = false)
    private Long stopId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "vehicle_id", nullable = false, insertable = false, updatable = false)
    private Long vehicleId;

    @Column(name = "pickup_point", length = 255)
    private String pickupPoint;

    @Column(name = "drop_point", length = 255)
    private String dropPoint;

    @Column(name = "academic_year_id")
    private Long academicYearId;

    @Column(name = "start_date", nullable = false)
    private java.time.LocalDate startDate;

    @Column(name = "end_date")
    private java.time.LocalDate endDate;

    @Column(name = "fee_applicable", nullable = false)
    private Boolean feeApplicable = false;

    @Column(name = "monthly_fee", precision = 10, scale = 2)
    private java.math.BigDecimal monthlyFee;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
