package com.edumanage.departments.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDepartmentRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String code;

    private String description;

    private Long headOfDepartmentId;

    private String status;
}
