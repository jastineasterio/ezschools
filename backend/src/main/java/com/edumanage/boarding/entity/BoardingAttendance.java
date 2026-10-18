package com.edumanage.boarding.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import com.edumanage.studentprofiles.entity.StudentProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "boarding_attendance", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"school_id", "student_id", "dormitory_id", "attendance_date"})
})
@Getter
@Setter
public class BoardingAttendance extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "dormitory_id", nullable = false)
    private Long dormitoryId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dormitory_id", insertable = false, updatable = false)
    private Dormitory dormitory;
}
