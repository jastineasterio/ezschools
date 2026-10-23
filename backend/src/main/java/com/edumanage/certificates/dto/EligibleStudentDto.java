package com.edumanage.certificates.dto;

import java.math.BigDecimal;

public record EligibleStudentDto(
        Long studentId,
        String studentName,
        String admissionNumber,
        Long resultId,
        BigDecimal marks,
        BigDecimal percentage,
        String grade,
        Long gradingSchemeId
) {}
