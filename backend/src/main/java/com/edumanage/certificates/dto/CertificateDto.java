package com.edumanage.certificates.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CertificateDto(
        Long id,
        Long studentId,
        String studentName,
        String admissionNumber,
        Long academicYearId,
        String academicYear,
        Long classId,
        String className,
        Long streamId,
        String streamName,
        Long subjectId,
        String subjectName,
        Long resultId,
        BigDecimal percentage,
        String awardType,
        String title,
        String description,
        String certificateNumber,
        String verificationCode,
        String status,
        Integer version,
        LocalDateTime issuedAt,
        LocalDateTime revokedAt,
        String revocationReason
) {}
