package com.edumanage.boarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDormitoryRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 1000)
    private String description;

    private Integer capacity;

    @Size(max = 20)
    private String gender;

    private Long wardenId;

    private String status;
}
