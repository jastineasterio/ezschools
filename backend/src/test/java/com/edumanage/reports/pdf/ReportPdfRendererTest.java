package com.edumanage.reports.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ReportPdfRendererTest {

    @Test
    void renderProducesValidPdf() throws Exception {
        ReportPdfRenderer renderer = new ReportPdfRenderer();

        ReportRenderData data = new ReportRenderData();
        data.setSchoolName("Test School");
        data.setSchoolAddress("123 Education Ave");
        data.setSchoolPhone("+1 555-0100");
        data.setSchoolEmail("info@testschool.edu");
        data.setSchoolRegistrationNumber("REG-001");
        data.setPrimaryColor("#3b82f6");
        data.setSecondaryColor("#64748b");
        data.setLogoUrl(null);
        data.setReportTypeLabel("Student Directory");
        data.setReportName("Student Directory Report");
        data.setAcademicYear("2024/2025");
        data.setTerm(null);
        data.setClassName("Grade 10");
        data.setStreamName("Science");
        data.setSubjectName(null);
        data.setExaminationName(null);
        data.setReportingPeriod("Term 1");
        data.setGeneratedAt("03 Nov 2026 10:00");
        data.setLandscape(false);
        data.setSummaryStats(List.of(
                new String[] { "Total Students", "42" },
                new String[] { "Active", "40" }));
        data.setColumns(List.of("Admission No", "Full Name", "Class", "Stream", "Status"));
        data.setRows(List.of(
                List.of("A001", "Alice Smith", "Grade 10", "Science", "ACTIVE"),
                List.of("A002", "Bob Jones", "Grade 10", "Science", "ACTIVE")));

        byte[] pdf = renderer.render(data);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(1000);
        // PDF magic bytes
        assertThat(new String(pdf, 0, 5)).startsWith("%PDF");
    }

    @Test
    void renderLandscapeProducesValidPdf() throws Exception {
        ReportPdfRenderer renderer = new ReportPdfRenderer();

        ReportRenderData data = new ReportRenderData();
        data.setSchoolName("Test School");
        data.setPrimaryColor("#3b82f6");
        data.setSecondaryColor("#64748b");
        data.setLogoUrl(null);
        data.setReportTypeLabel("Financial Transactions Report");
        data.setReportName("Finance Report");
        data.setGeneratedAt("03 Nov 2026 10:00");
        data.setLandscape(true);
        data.setColumns(List.of("Date", "Amount", "Method", "Status"));
        data.setRows(List.of(
                List.of("01 Jan 2026", "100.00", "Cash", "COMPLETED")));

        byte[] pdf = renderer.render(data);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(1000);
        assertThat(new String(pdf, 0, 5)).startsWith("%PDF");
    }

    @Test
    void renderNoDataProducesValidPdf() throws Exception {
        ReportPdfRenderer renderer = new ReportPdfRenderer();

        ReportRenderData data = new ReportRenderData();
        data.setSchoolName("Test School");
        data.setPrimaryColor("#3b82f6");
        data.setSecondaryColor("#64748b");
        data.setLogoUrl(null);
        data.setReportTypeLabel("Student Directory");
        data.setReportName("Empty Report");
        data.setGeneratedAt("03 Nov 2026 10:00");
        data.setLandscape(false);
        data.setColumns(List.of());
        data.setRows(List.of());

        byte[] pdf = renderer.render(data);

        assertThat(pdf).isNotNull();
        assertThat(new String(pdf, 0, 5)).startsWith("%PDF");
    }
}
