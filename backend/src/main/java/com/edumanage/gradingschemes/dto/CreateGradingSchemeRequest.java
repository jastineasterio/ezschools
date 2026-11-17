package com.edumanage.gradingschemes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGradingSchemeRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @JsonProperty("isDefault")
    private boolean isDefault;

    private String status;
}
