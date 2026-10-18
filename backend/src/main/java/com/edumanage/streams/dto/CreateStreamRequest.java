package com.edumanage.streams.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateStreamRequest {

    @NotNull(message = "Class ID is required")
    private Long classId;

    @NotBlank(message = "Name is required")
    private String name;

    private String code;

    private Integer capacity;

    private String status;
}
