package com.edumanage.results.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultActionRequest {

    @NotBlank(message = "Action is required")
    private String action;

    private String comment;
}
