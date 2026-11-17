package com.edumanage.reports.pdf;

import java.util.List;

import lombok.Data;

@Data
public class ReportRenderData {
    private String schoolName;
    private String schoolAddress;
    private String schoolPhone;
    private String schoolEmail;
    private String schoolWebsite;
    private String schoolRegistrationNumber;

    private String primaryColor;
    private String secondaryColor;
    private String logoUrl;

    private String reportTypeLabel;
    private String reportName;

    private String academicYear;
    private String term;
    private String className;
    private String streamName;
    private String subjectName;
    private String examinationName;
    private String reportingPeriod;
    private String generatedAt;

    private List<String[]> summaryStats;
    private List<String> columns;
    private List<List<String>> rows;

    private boolean landscape;
}
