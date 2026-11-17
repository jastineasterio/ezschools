package com.edumanage.teacherworkspace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherClassPerformanceDto {
    private String className;
    private String streamName;
    private double averageMarks;
    private long studentCount;

}
