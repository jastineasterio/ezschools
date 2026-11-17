package com.edumanage.teacherworkspace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherDashboardChartDto {
    private String name;
    private double averageMarks;
    private double passRate;

}
