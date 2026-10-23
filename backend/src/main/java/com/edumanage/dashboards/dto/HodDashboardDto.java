package com.edumanage.dashboards.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Landing payload for the head of department dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HodDashboardDto {

    private Long schoolId;
    private String schoolName;
    private Long academicYearId;
    private String academicYearName;
    private long totalDepartments;
    private long headedDepartments;
    private long subjects;
    private long teachers;
    private long students;
    private long pendingResultApprovals;
    private long publishedResults;
    private List<DepartmentSummary> departments = new ArrayList<>();
    private List<ExaminationSummary> upcomingExaminations = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentSummary {
        private Long id;
        private String name;
        private String code;
        private String status;
        private long subjects;
        private long teachers;
        private long students;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExaminationSummary {
        private Long id;
        private String name;
        private String status;
        private String startDate;
        private String endDate;
    }
}
