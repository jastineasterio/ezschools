package com.edumanage.streamsubjects.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamSubjectAssignmentDto {
    private Long id;
    private Long subjectId;
    private String subjectName;
    private String subjectCode;
    private Long academicYearId;
    private Long classId;
    private Long streamId;
}
