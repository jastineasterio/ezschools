package com.edumanage.packages.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageDto {
    private Long id;
    private String uuid;
    private String name;
    private String displayName;
    private String description;
    private BigDecimal price;
    private String billingPeriod;
    @JsonProperty("active")
    private boolean isActive;

    @JsonProperty("default")
    private boolean isDefault;
    private int sortOrder;
    private LocalDateTime createdAt;
    private List<PackageRoleDto> roles;
    private List<PackageFeatureDto> features;
    private List<PackageLimitDto> limits;
}
