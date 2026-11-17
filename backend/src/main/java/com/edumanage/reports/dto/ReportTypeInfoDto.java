package com.edumanage.reports.dto;

import java.util.List;

import lombok.Data;

@Data
public class ReportTypeInfoDto {
    private String id;
    private String label;
    private String category;
    private List<ReportFilterFieldDto> fields;
}
