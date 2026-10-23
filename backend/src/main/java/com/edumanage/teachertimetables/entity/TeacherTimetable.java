package com.edumanage.teachertimetables.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import com.edumanage.teacherassignments.entity.TeacherAssignment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "teacher_timetables")
@Getter
@Setter
public class TeacherTimetable extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "teacher_assignment_id", nullable = false)
    private Long teacherAssignmentId;

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "room", length = 120)
    private String room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_assignment_id", insertable = false, updatable = false)
    private TeacherAssignment teacherAssignment;
}
