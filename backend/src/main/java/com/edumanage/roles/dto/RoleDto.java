package com.edumanage.roles.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {
    private Long id;
    private String name;
    private String displayName;
    private String description;
    private boolean isSystem;
    private boolean isActive;
    private List<String> permissions;
}
