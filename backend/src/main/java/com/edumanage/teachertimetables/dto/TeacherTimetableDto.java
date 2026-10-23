package com.edumanage.teachertimetables.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;

@Data
@AllArgsConstructor
public class TeacherTimetableDto {
    private Long id;
    private Long teacherAssignmentId;
    private int dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private String className;
    private String streamName;
    private String subjectName;
}
