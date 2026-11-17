package com.edumanage.reports.dto;

import java.util.List;

import lombok.Data;

@Data
public class ReportFilterDefinitionsResponse {
    private List<ReportTypeInfoDto> reportTypes;
}
