package com.edumanage.reports.dto;

import java.util.List;

import lombok.Data;

@Data
public class ReportFilterFieldDto {
    private String name;
    private String label;
    private String type;
    private boolean required;
    private String placeholder;
    private String helperText;
    private String optionsEndpoint;
    private String dependsOn;
    private List<ReportOptionDto> options;
}
