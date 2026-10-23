package com.edumanage.certificates.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CertificateRequest(
        @NotNull Long studentId,
        @NotNull Long academicYearId,
        @NotNull Long classId,
        Long streamId,
        Long subjectId,
        @NotBlank @Size(max = 40) String awardType,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 500)
        String description
) {}
