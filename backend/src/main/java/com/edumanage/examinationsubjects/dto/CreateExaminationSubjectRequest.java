package com.edumanage.examinationsubjects.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateExaminationSubjectRequest {

    // examinationId is supplied via the @PathVariable and is not read from the
    // request body, so it must not be a required body field.
    private Long examinationId;

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotNull(message = "Class ID is required")
    private Long classId;

    private Long streamId;

    @NotNull(message = "Maximum marks is required")
    private Integer maximumMarks;

    @NotNull(message = "Pass mark is required")
    private Integer passMark;

    private Long gradingSchemeId;
}
