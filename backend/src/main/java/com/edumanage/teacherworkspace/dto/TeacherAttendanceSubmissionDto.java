package com.edumanage.teacherworkspace.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TeacherAttendanceSubmissionDto {
    @NotNull
    private Long studentId;

    @NotNull
    @Pattern(regexp = "PRESENT|ABSENT|LATE|EXCUSED")
    private String status;

    @Size(max = 2000)
    private String remarks;
}
