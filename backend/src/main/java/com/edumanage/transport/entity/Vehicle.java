package com.edumanage.transport.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "vehicles", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"school_id", "registration_number"})
})
public class Vehicle extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "registration_number", nullable = false, length = 100)
    private String registrationNumber;

    @Column(name = "vehicle_type", nullable = false, length = 50)
    private String vehicleType = "BUS";

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "driver_name", length = 255)
    private String driverName;

    @Column(name = "driver_phone", length = 20)
    private String driverPhone;

    @Column(name = "insurance_number", length = 100)
    private String insuranceNumber;

    @Column(name = "insurance_expiry")
    private java.time.LocalDate insuranceExpiry;

    @Column(name = "last_service_date")
    private java.time.LocalDate lastServiceDate;

    @Column(name = "next_service_date")
    private java.time.LocalDate nextServiceDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
