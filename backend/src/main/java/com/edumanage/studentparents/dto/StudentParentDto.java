package com.edumanage.studentparents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentParentDto {
    private Long id;
    private String uuid;
    private Long studentId;
    private String studentName;
    private Long parentId;
    private String parentName;
    private String relationshipType;
    private boolean isPrimary;
    private boolean receivesSms;
    private boolean receivesNotifications;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
