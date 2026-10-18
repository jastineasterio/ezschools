package com.edumanage.subjects.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private String name;
    private String code;
    private String description;
    private Long departmentId;
    private String departmentName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
