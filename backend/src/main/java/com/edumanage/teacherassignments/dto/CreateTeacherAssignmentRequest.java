package com.edumanage.teacherassignments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTeacherAssignmentRequest {

    @NotNull(message = "Teacher ID is required")
    private Long teacherId;

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotNull(message = "Class ID is required")
    private Long classId;

    @NotNull(message = "Academic year ID is required")
    private Long academicYearId;

    private Long streamId;

    private boolean isClassTeacher;
}
