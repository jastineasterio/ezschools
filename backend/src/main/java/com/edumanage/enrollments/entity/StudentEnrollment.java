package com.edumanage.enrollments.entity;

import com.edumanage.academicyears.entity.AcademicYear;
import com.edumanage.classes.entity.Class;
import com.edumanage.common.entity.SoftDeletableEntity;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.streams.entity.Stream;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "student_enrollments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "academic_year_id"})
})
@Getter
@Setter
public class StudentEnrollment extends SoftDeletableEntity {

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "stream_id")
    private Long streamId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ENROLLED";

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", insertable = false, updatable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", insertable = false, updatable = false)
    private Class clazz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stream_id", insertable = false, updatable = false)
    private Stream stream;
}
