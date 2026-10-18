package com.edumanage.examinationsubjects.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExaminationSubjectDto {
    private Long id;
    private String uuid;
    private Long examinationId;
    private String examinationName;
    private Long subjectId;
    private String subjectName;
    private Long classId;
    private String className;
    private Long streamId;
    private String streamName;
    private Integer maximumMarks;
    private Integer passMark;
    private Long gradingSchemeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
