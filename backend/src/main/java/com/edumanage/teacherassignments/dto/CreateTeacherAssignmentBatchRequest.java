package com.edumanage.teacherassignments.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateTeacherAssignmentBatchRequest {
    @NotNull
    private Long teacherId;

    @NotNull
    private Long academicYearId;

    @NotNull
    private Long classId;

    private Long streamId;

    @NotEmpty
    private List<@NotNull Long> subjectIds;

    private boolean classTeacher;
}
