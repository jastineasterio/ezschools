package com.edumanage.teacherworkspace.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TeacherResultSubmissionDto {
    @NotNull
    private Long studentId;

    @PositiveOrZero
    private Integer marksObtained;

    @Size(max = 2000)
    private String remarks;
}
