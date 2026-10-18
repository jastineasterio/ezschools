package com.edumanage.branding.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchoolBrandingDto {
    private Long id;
    private Long schoolId;
    private String primaryColor;
    private String secondaryColor;
    private String accentColor;
    private String backgroundColor;
    private String textColor;
    private String logoUrl;
    private String faviconUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
