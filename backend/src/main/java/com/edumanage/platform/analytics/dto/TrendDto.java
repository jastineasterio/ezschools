package com.edumanage.platform.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendDto {
    private String period;
    private long userCount;
    private long newUsers;
}
