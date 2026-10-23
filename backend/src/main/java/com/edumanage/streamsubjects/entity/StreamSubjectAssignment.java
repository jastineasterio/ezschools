package com.edumanage.streamsubjects.entity;

import com.edumanage.subjects.entity.Subject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "stream_subject_assignments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"stream_id", "academic_year_id", "subject_id"})
})
@Getter
@Setter
public class StreamSubjectAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "stream_id", nullable = false)
    private Long streamId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", insertable = false, updatable = false)
    private Subject subject;
}
