package com.edumanage.boarding.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import com.edumanage.studentprofiles.entity.StudentProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "beds")
@Getter
@Setter
public class Bed extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "bed_number", nullable = false, length = 50)
    private String bedNumber;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "AVAILABLE";

    @Column(name = "allocated_date")
    private LocalDate allocatedDate;

    @Column(name = "released_date")
    private LocalDate releasedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", insertable = false, updatable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private StudentProfile student;
}
