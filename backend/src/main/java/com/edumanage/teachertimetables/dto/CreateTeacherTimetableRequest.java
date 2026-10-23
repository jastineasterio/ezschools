package com.edumanage.teachertimetables.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

@Data
public class CreateTeacherTimetableRequest {
    @NotNull
    private Long teacherAssignmentId;

    @Min(1)
    @Max(7)
    private int dayOfWeek;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @Size(max = 120)
    private String room;
}
