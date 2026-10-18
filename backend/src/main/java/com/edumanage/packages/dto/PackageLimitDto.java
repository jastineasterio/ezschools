package com.edumanage.packages.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageLimitDto {
    private Long id;
    private String limitKey;
    private String limitName;
    private long maxValue;
    private long currentValue;
    private String unit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
