package com.edumanage.transport.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "route_stops")
public class RouteStop extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "route_id", nullable = false, insertable = false, updatable = false)
    private Long routeId;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @Column(name = "latitude", precision = 10, scale = 8)
    private java.math.BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private java.math.BigDecimal longitude;

    @Column(name = "arrival_time")
    private java.time.LocalTime arrivalTime;

    @Column(name = "departure_time")
    private java.time.LocalTime departureTime;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
