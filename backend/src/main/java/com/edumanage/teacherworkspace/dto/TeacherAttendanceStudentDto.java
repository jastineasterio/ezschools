package com.edumanage.teacherworkspace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TeacherAttendanceStudentDto {
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private Long streamId;
    private String streamName;
    private String status;
    private String remarks;
}
