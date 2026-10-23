package com.edumanage.teacherassignments.entity;

import com.edumanage.academicyears.entity.AcademicYear;
import com.edumanage.classes.entity.Class;
import com.edumanage.common.entity.SoftDeletableEntity;
import com.edumanage.streams.entity.Stream;
import com.edumanage.subjects.entity.Subject;
import com.edumanage.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "teacher_assignments")
@Getter
@Setter
public class TeacherAssignment extends SoftDeletableEntity {

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "stream_id")
    private Long streamId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "is_class_teacher", nullable = false)
    private boolean isClassTeacher = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", insertable = false, updatable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", insertable = false, updatable = false)
    private Class clazz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stream_id", insertable = false, updatable = false)
    private Stream stream;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", insertable = false, updatable = false)
    private AcademicYear academicYear;
}
