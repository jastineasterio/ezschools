package com.edumanage.packages.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageFeatureDto {
    private Long id;
    private String featureKey;
    private String featureName;
    private boolean isEnabled;
    private LocalDateTime createdAt;
}
