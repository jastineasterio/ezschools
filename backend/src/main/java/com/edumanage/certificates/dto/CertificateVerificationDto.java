package com.edumanage.certificates.dto;

import java.time.LocalDateTime;

public record CertificateVerificationDto(
        boolean valid,
        String status,
        String certificateNumber,
        String schoolName,
        String studentName,
        String title,
        String academicYear,
        LocalDateTime issuedAt
) {}
