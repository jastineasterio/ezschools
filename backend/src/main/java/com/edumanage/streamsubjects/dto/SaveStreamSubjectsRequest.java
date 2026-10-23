package com.edumanage.streamsubjects.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SaveStreamSubjectsRequest {
    @NotNull
    private Long academicYearId;

    @NotNull
    private Long classId;

    @NotNull
    private Long streamId;

    @NotNull
    private List<@NotNull Long> subjectIds;
}
