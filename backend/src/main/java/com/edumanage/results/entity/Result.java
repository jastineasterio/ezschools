package com.edumanage.results.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import com.edumanage.examinations.entity.Examination;
import com.edumanage.examinationsubjects.entity.ExaminationSubject;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "results", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "examination_subject_id"})
})
@Getter
@Setter
public class Result extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "examination_subject_id", nullable = false)
    private Long examinationSubjectId;

    @Column(name = "examination_id", nullable = false)
    private Long examinationId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "marks_obtained", precision = 10, scale = 2)
    private BigDecimal marksObtained;

    @Column(name = "percentage", precision = 10, scale = 2)
    private BigDecimal percentage;

    @Column(name = "grade", length = 10)
    private String grade;

    @Column(name = "grade_point", precision = 10, scale = 2)
    private BigDecimal gradePoint;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "examination_subject_id", insertable = false, updatable = false)
    private ExaminationSubject examinationSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "examination_id", insertable = false, updatable = false)
    private Examination examination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
    private User teacher;
}
