package com.edumanage.results.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultAnalyticsDto {
    private Double overallAverage;
    private long totalStudentsAssessed;
    private double passRate;
    private double failRate;
    private Double highestScore;
    private Double lowestScore;
    private List<GradeDistribution> gradeDistribution;
    private List<ClassPerformance> classPerformance;
    private List<StreamPerformance> streamPerformance;
    private List<SubjectPerformance> subjectPerformance;
    private List<StudentPerformance> topStudents;
    private List<StudentPerformance> studentsNeedingSupport;
    private List<TrendPoint> performanceTrends;
    private long totalExaminations;
    private long submittedResults;
    private long approvedResults;
    private long publishedResults;
    private long totalResults;
    private long passedResults;
    private long failedResults;
    private long draftResults;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradeDistribution {
        private String grade;
        private long count;
        private double percentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassPerformance {
        private Long classId;
        private String className;
        private Double average;
        private long studentCount;
        private double passRate;
        private Double highestScore;
        private Double lowestScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreamPerformance {
        private Long streamId;
        private String streamName;
        private String className;
        private Double average;
        private long studentCount;
        private double passRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectPerformance {
        private Long subjectId;
        private String subjectName;
        private Double average;
        private long studentCount;
        private double passRate;
        private Double highestScore;
        private Double lowestScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentPerformance {
        private Long studentId;
        private String studentName;
        private String admissionNumber;
        private Double average;
        private String grade;
        private long assessmentCount;
        private boolean needsSupport;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private Long examinationId;
        private String examinationName;
        private Double average;
        private long studentCount;
        private double passRate;
    }
}
