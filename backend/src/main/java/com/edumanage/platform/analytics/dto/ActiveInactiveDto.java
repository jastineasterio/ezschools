package com.edumanage.platform.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActiveInactiveDto {
    private String status;
    private long count;
    private double percent;
}
