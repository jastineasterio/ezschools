package com.edumanage.permissions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDto {
    private Long id;
    private String name;
    private String displayName;
    private String description;
    private String category;
}
