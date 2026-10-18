package com.edumanage.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long studentId;
    private String studentName;
    private Long classId;
    private String className;
    private Long streamId;
    private String streamName;
    private Long academicYearId;
    private String academicYearName;
    private Long subjectId;
    private String subjectName;
    private Long teacherId;
    private String teacherName;
    private LocalDate attendanceDate;
    private String status;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
