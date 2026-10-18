package com.edumanage.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSettingsDto {
    private Map<String, Object> settings;
}
