package com.edumanage.subjects.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubjectRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String code;

    private String description;

    private Long departmentId;

    private String status;
}
