package com.edumanage.packages.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageRoleDto {
    private Long id;
    private String roleName;
    private LocalDateTime createdAt;
}
