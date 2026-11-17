package com.edumanage.branding.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    @NotNull
    @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "Primary color must be a valid HEX color (e.g. #3b82f6)")
    private String primaryColor;

    @NotNull
    @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "Secondary color must be a valid HEX color (e.g. #64748b)")
    private String secondaryColor;

    @NotNull
    @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "Accent color must be a valid HEX color (e.g. #f59e0b)")
    private String accentColor;

    @NotNull
    @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "Background color must be a valid HEX color (e.g. #ffffff)")
    private String backgroundColor;

    @NotNull
    @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "Text color must be a valid HEX color (e.g. #0f172a)")
    private String textColor;

    private String logoUrl;
    private String faviconUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
