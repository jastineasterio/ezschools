package com.edumanage.teachertimetables.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateSchoolTimeZoneRequest {
    @NotBlank
    @Size(max = 64)
    private String timeZone;
}
