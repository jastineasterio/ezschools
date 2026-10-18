package com.edumanage.boarding.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "rooms")
@Getter
@Setter
public class Room extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "dormitory_id", nullable = false)
    private Long dormitoryId;

    @Column(name = "room_number", nullable = false, length = 50)
    private String roomNumber;

    @Column(name = "capacity", nullable = false)
    private Integer capacity = 0;

    @Column(name = "current_occupancy", nullable = false)
    private Integer currentOccupancy = 0;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dormitory_id", insertable = false, updatable = false)
    private Dormitory dormitory;
}
