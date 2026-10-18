package com.edumanage.examinationsubjects.entity;

import com.edumanage.classes.entity.Class;
import com.edumanage.common.entity.SoftDeletableEntity;
import com.edumanage.examinations.entity.Examination;
import com.edumanage.streams.entity.Stream;
import com.edumanage.subjects.entity.Subject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "examination_subjects", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"examination_id", "subject_id", "class_id", "stream_id"})
})
@Getter
@Setter
public class ExaminationSubject extends SoftDeletableEntity {

    @Column(name = "examination_id", nullable = false)
    private Long examinationId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "stream_id")
    private Long streamId;

    @Column(name = "maximum_marks", nullable = false, precision = 10, scale = 2)
    private BigDecimal maximumMarks;

    @Column(name = "pass_mark", nullable = false, precision = 10, scale = 2)
    private BigDecimal passMark;

    @Column(name = "grading_scheme_id")
    private Long gradingSchemeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "examination_id", insertable = false, updatable = false)
    private Examination examination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", insertable = false, updatable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", insertable = false, updatable = false)
    private Class clazz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stream_id", insertable = false, updatable = false)
    private Stream stream;
}
