package com.edumanage.examinationreports.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExaminationReportDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private Long examinationId;
    private String examinationName;
    private String fileKey;
    private String filename;
    private String mimeType;
    private Long size;
    private Long generatedBy;
    private String generatedByUserName;
    private LocalDateTime generatedAt;
    private LocalDateTime createdAt;
}
