package com.edumanage.teacherassignments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAssignmentDto {
    private Long id;
    private String uuid;
    private Long teacherId;
    private String teacherName;
    private Long schoolId;
    private Long subjectId;
    private String subjectName;
    private Long classId;
    private String className;
    private Long streamId;
    private String streamName;
    private Long academicYearId;
    private String academicYearName;
    private Long studentCount;
    private boolean isClassTeacher;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
