package com.edumanage.examinationreports.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import com.edumanage.examinations.entity.Examination;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "examination_reports")
@Getter
@Setter
public class ExaminationReport extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "examination_id", nullable = false)
    private Long examinationId;

    @Column(name = "file_key", nullable = false, length = 500)
    private String fileKey;

    @Column(name = "filename", nullable = false, length = 500)
    private String filename;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "generated_by", nullable = false)
    private Long generatedBy;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "examination_id", insertable = false, updatable = false)
    private Examination examination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by", insertable = false, updatable = false)
    private User generatedByUser;
}
