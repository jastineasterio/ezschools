package com.edumanage.teacherworkspace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherAttendanceChartDto {
    private String date;
    private long present;
    private long absent;
    private long late;
    private long excused;

}
