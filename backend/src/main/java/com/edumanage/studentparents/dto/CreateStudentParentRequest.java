package com.edumanage.studentparents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateStudentParentRequest {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Parent ID is required")
    private Long parentId;

    @NotBlank(message = "Relationship type is required")
    private String relationshipType;

    private boolean isPrimary;

    private boolean receivesSms;

    private boolean receivesNotifications;
}
