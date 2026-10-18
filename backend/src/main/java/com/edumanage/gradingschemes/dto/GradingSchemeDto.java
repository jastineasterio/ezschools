package com.edumanage.gradingschemes.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradingSchemeDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private String name;
    private String description;
    private boolean isDefault;
    private String status;
    private List<GradingSchemeRangeDto> ranges;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
