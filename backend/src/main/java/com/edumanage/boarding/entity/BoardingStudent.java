package com.edumanage.boarding.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import com.edumanage.studentprofiles.entity.StudentProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "boarding_students", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"school_id", "student_id", "status"})
})
@Getter
@Setter
public class BoardingStudent extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "dormitory_id", nullable = false)
    private Long dormitoryId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "bed_id", nullable = false)
    private Long bedId;

    @Column(name = "allocated_date", nullable = false)
    private LocalDate allocatedDate = LocalDate.now();

    @Column(name = "released_date")
    private LocalDate releasedDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dormitory_id", insertable = false, updatable = false)
    private Dormitory dormitory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", insertable = false, updatable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bed_id", insertable = false, updatable = false)
    private Bed bed;
}
