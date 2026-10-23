package com.edumanage.certificates.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_certificates", uniqueConstraints = {
        @UniqueConstraint(columnNames = "certificate_number"),
        @UniqueConstraint(columnNames = "verification_code")
})
@Getter
@Setter
public class StudentCertificate extends SoftDeletableEntity {
    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "stream_id")
    private Long streamId;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "result_id")
    private Long resultId;

    @Column(name = "award_type", nullable = false, length = 40)
    private String awardType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "certificate_number", nullable = false, unique = true, length = 80)
    private String certificateNumber;

    @Column(name = "verification_code", nullable = false, unique = true, length = 80)
    private String verificationCode;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "GENERATED";

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "issued_by")
    private Long issuedBy;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "revoked_by")
    private Long revokedBy;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revocation_reason", columnDefinition = "TEXT")
    private String revocationReason;
}
