package com.edumanage.results.service;

import com.edumanage.departments.repository.DepartmentRepository;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.results.dto.ResultAnalyticsDto;
import com.edumanage.results.dto.ResultAnalyticsRow;
import com.edumanage.results.repository.ResultRepository;
import com.edumanage.security.CurrentUserService;
import com.edumanage.subjects.entity.Subject;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;

@Service
public class ResultAnalyticsService {

    private final ResultRepository resultRepository;
    private final DepartmentRepository departmentRepository;
    private final SubjectRepository subjectRepository;
    private final CurrentUserService currentUserService;

    public ResultAnalyticsService(ResultRepository resultRepository,
                                  DepartmentRepository departmentRepository,
                                  SubjectRepository subjectRepository,
                                  CurrentUserService currentUserService) {
        this.resultRepository = resultRepository;
        this.departmentRepository = departmentRepository;
        this.subjectRepository = subjectRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public ResultAnalyticsDto getSchoolAnalytics(Long academicYearId, Long examinationId, Long classId,
                                                 Long streamId, Long subjectId, LocalDate startDate, LocalDate endDate) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("A school assignment is required");
        }
        List<ResultAnalyticsRow> rows = resultRepository.findResultsForAnalytics(
                tenantId, academicYearId, examinationId, classId, streamId, subjectId, startDate, endDate);
        return compute(rows, false);
    }

    @Transactional(readOnly = true)
    public ResultAnalyticsDto getDepartmentAnalytics(Long academicYearId, Long examinationId, Long classId,
                                                     Long streamId, LocalDate startDate, LocalDate endDate) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("A school assignment is required");
        }
        var user = currentUserService.requireCurrentUser();
        Set<Long> deptIds = departmentRepository.findBySchoolIdAndHeadOfDepartmentId(tenantId, user.getId()).stream()
                .map(department -> department.getId())
                .collect(Collectors.toSet());
        if (deptIds.isEmpty()) {
            return emptyAnalytics();
        }
        Set<Long> authorizedSubjectIds = subjectRepository.findBySchoolId(tenantId, Pageable.unpaged()).getContent().stream()
                .filter(subject -> subject.getDepartmentId() != null && deptIds.contains(subject.getDepartmentId()))
                .map(Subject::getId)
                .collect(Collectors.toSet());
        if (authorizedSubjectIds.isEmpty()) {
            return emptyAnalytics();
        }
        List<ResultAnalyticsRow> rows = resultRepository.findResultsForAnalytics(
                tenantId, academicYearId, examinationId, classId, streamId, null, startDate, endDate);
        List<ResultAnalyticsRow> filtered = rows.stream()
                .filter(row -> authorizedSubjectIds.contains(row.getSubjectId()))
                .collect(Collectors.toList());
        return compute(filtered, true);
    }

    private ResultAnalyticsDto compute(List<ResultAnalyticsRow> rows, boolean departmentMode) {
        if (rows.isEmpty()) {
            return emptyAnalytics();
        }

        List<ResultAnalyticsRow> assessed = rows.stream()
                .filter(row -> row.getMarksObtained() != null)
                .collect(Collectors.toList());

        double totalMarks = assessed.stream()
                .mapToDouble(row -> row.getMarksObtained().doubleValue())
                .sum();
        double overallAverage = assessed.isEmpty() ? 0 : round(totalMarks / assessed.size());

        long totalStudentsAssessed = assessed.stream()
                .map(ResultAnalyticsRow::getStudentId)
                .distinct()
                .count();

        long passed = assessed.stream()
                .filter(row -> row.getPassMark() != null
                        && row.getMarksObtained() != null
                        && row.getMarksObtained().compareTo(row.getPassMark()) >= 0)
                .count();
        double passRate = totalStudentsAssessed > 0 ? round((double) passed / totalStudentsAssessed * 100) : 0;
        double failRate = 100 - passRate;

        Optional<BigDecimal> highest = assessed.stream()
                .map(ResultAnalyticsRow::getMarksObtained)
                .max(Comparator.naturalOrder());
        Optional<BigDecimal> lowest = assessed.stream()
                .map(ResultAnalyticsRow::getMarksObtained)
                .min(Comparator.naturalOrder());

        Map<String, Long> gradeCounts = assessed.stream()
                .filter(row -> row.getGrade() != null)
                .collect(Collectors.groupingBy(ResultAnalyticsRow::getGrade, Collectors.counting()));
        List<ResultAnalyticsDto.GradeDistribution> gradeDistribution = gradeCounts.entrySet().stream()
                .map(entry -> new ResultAnalyticsDto.GradeDistribution(
                        entry.getKey(), entry.getValue(),
                        round((double) entry.getValue() / assessed.size() * 100)))
                .sorted(Comparator.comparing(ResultAnalyticsDto.GradeDistribution::getGrade))
                .collect(Collectors.toList());

        Map<Long, AggData> classAgg = new java.util.LinkedHashMap<>();
        Map<Long, AggData> streamAgg = new java.util.LinkedHashMap<>();
        Map<Long, AggData> subjectAgg = new java.util.LinkedHashMap<>();
        Map<Long, ClassStreamKey> streamMeta = new java.util.LinkedHashMap<>();
        for (ResultAnalyticsRow row : assessed) {
            if (row.getClassId() != null) {
                AggData data = classAgg.computeIfAbsent(row.getClassId(), id -> new AggData(row.getClassName()));
                add(data, row);
            }
            if (row.getStreamId() != null) {
                AggData data = streamAgg.computeIfAbsent(row.getStreamId(), id -> new AggData(row.getStreamName()));
                add(data, row);
                streamMeta.put(row.getStreamId(), new ClassStreamKey(row.getClassId(), row.getClassName()));
            }
            if (row.getSubjectId() != null) {
                AggData data = subjectAgg.computeIfAbsent(row.getSubjectId(), id -> new AggData(row.getSubjectName()));
                add(data, row);
            }
        }

        List<ResultAnalyticsDto.ClassPerformance> classPerformance = classAgg.entrySet().stream()
                .map(entry -> toClassPerformance(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        List<ResultAnalyticsDto.StreamPerformance> streamPerformance = streamAgg.entrySet().stream()
                .map(entry -> {
                    ClassStreamKey key = streamMeta.get(entry.getKey());
                    return toStreamPerformance(entry.getKey(), entry.getValue(),
                            key == null ? null : key.classId,
                            key == null ? null : key.className);
                })
                .collect(Collectors.toList());
        List<ResultAnalyticsDto.SubjectPerformance> subjectPerformance = subjectAgg.entrySet().stream()
                .map(entry -> toSubjectPerformance(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        Map<Long, List<ResultAnalyticsRow>> studentRows = assessed.stream()
                .collect(Collectors.groupingBy(ResultAnalyticsRow::getStudentId));
        List<ResultAnalyticsDto.StudentPerformance> studentPerformances = studentRows.entrySet().stream()
                .map(entry -> {
                    List<ResultAnalyticsRow> rws = entry.getValue();
                    double avg = rws.stream()
                            .mapToDouble(row -> row.getMarksObtained().doubleValue())
                            .average()
                            .orElse(0);
                    boolean needsSupport = avg < 50;
                    return new ResultAnalyticsDto.StudentPerformance(
                            entry.getKey(),
                            rws.get(0).getStudentName(),
                            rws.get(0).getAdmissionNumber(),
                            round(avg),
                            gradeFor(avg),
                            rws.size(),
                            needsSupport);
                })
                .collect(Collectors.toList());

        List<ResultAnalyticsDto.StudentPerformance> topStudents = studentPerformances.stream()
                .sorted(Comparator.comparingDouble((ResultAnalyticsDto.StudentPerformance s) -> s.getAverage() == null ? 0 : s.getAverage()).reversed())
                .limit(10)
                .collect(Collectors.toList());

        List<ResultAnalyticsDto.StudentPerformance> needingSupport = studentPerformances.stream()
                .filter(ResultAnalyticsDto.StudentPerformance::isNeedsSupport)
                .sorted(Comparator.comparingDouble((ResultAnalyticsDto.StudentPerformance s) -> s.getAverage() == null ? 0 : s.getAverage()))
                .limit(10)
                .collect(Collectors.toList());

        Map<Long, List<ResultAnalyticsRow>> examRows = rows.stream()
                .filter(row -> row.getMarksObtained() != null)
                .collect(Collectors.groupingBy(ResultAnalyticsRow::getExaminationId));
        List<ResultAnalyticsDto.TrendPoint> trends = examRows.entrySet().stream()
                .map(entry -> {
                    List<ResultAnalyticsRow> rws = entry.getValue();
                    double avg = rws.stream().mapToDouble(row -> row.getMarksObtained().doubleValue()).average().orElse(0);
                    long passedTrend = rws.stream()
                            .filter(row -> row.getPassMark() != null && row.getMarksObtained().compareTo(row.getPassMark()) >= 0)
                            .count();
                    return new ResultAnalyticsDto.TrendPoint(
                            entry.getKey(), rws.get(0).getExaminationName(), round(avg), rws.size(),
                            round((double) passedTrend / rws.size() * 100));
                })
                .sorted(Comparator.comparing(ResultAnalyticsDto.TrendPoint::getExaminationName))
                .collect(Collectors.toList());

        long submitted = rows.stream().filter(row -> "SUBMITTED".equalsIgnoreCase(row.getStatus())).count();
        long approved = rows.stream().filter(row -> "APPROVED".equalsIgnoreCase(row.getStatus())).count();
        long published = rows.stream().filter(row -> "PUBLISHED".equalsIgnoreCase(row.getStatus())).count();

        long totalResults = rows.size();
        long passedResults = passed;
        long failedResults = assessed.size() - passed;
        long draftResults = rows.stream().filter(row -> "DRAFT".equalsIgnoreCase(row.getStatus())).count();

        ResultAnalyticsDto dto = new ResultAnalyticsDto();
        dto.setOverallAverage(overallAverage);
        dto.setTotalStudentsAssessed(totalStudentsAssessed);
        dto.setPassRate(passRate);
        dto.setFailRate(failRate);
        dto.setHighestScore(highest.map(BigDecimal::doubleValue).orElse(null));
        dto.setLowestScore(lowest.map(BigDecimal::doubleValue).orElse(null));
        dto.setGradeDistribution(gradeDistribution);
        dto.setClassPerformance(classPerformance);
        dto.setStreamPerformance(streamPerformance);
        dto.setSubjectPerformance(subjectPerformance);
        dto.setTopStudents(topStudents);
        dto.setStudentsNeedingSupport(needingSupport);
        dto.setPerformanceTrends(trends);
        dto.setTotalExaminations(rows.stream().map(ResultAnalyticsRow::getExaminationId).distinct().count());
        dto.setSubmittedResults(submitted);
        dto.setApprovedResults(approved);
        dto.setPublishedResults(published);
        dto.setTotalResults(totalResults);
        dto.setPassedResults(passedResults);
        dto.setFailedResults(failedResults);
        dto.setDraftResults(draftResults);
        return dto;
    }

    private static void add(AggData data, ResultAnalyticsRow row) {
        data.studentCount++;
        data.sum += row.getMarksObtained().doubleValue();
        data.highest = data.highest == null ? row.getMarksObtained().doubleValue()
                : Math.max(data.highest, row.getMarksObtained().doubleValue());
        data.lowest = data.lowest == null ? row.getMarksObtained().doubleValue()
                : Math.min(data.lowest, row.getMarksObtained().doubleValue());
        if (row.getPassMark() != null && row.getMarksObtained().compareTo(row.getPassMark()) >= 0) {
            data.passed++;
        }
    }

    private ResultAnalyticsDto.ClassPerformance toClassPerformance(Long id, AggData data) {
        ResultAnalyticsDto.ClassPerformance cp = new ResultAnalyticsDto.ClassPerformance();
        cp.setClassId(id);
        cp.setClassName(data.name);
        cp.setStudentCount(data.studentCount);
        cp.setAverage(data.studentCount > 0 ? round(data.sum / data.studentCount) : null);
        cp.setPassRate(data.studentCount > 0 ? round((double) data.passed / data.studentCount * 100) : 0);
        cp.setHighestScore(data.highest);
        cp.setLowestScore(data.lowest);
        return cp;
    }

    private ResultAnalyticsDto.StreamPerformance toStreamPerformance(Long id, AggData data,
                                                                     Long classId, String className) {
        ResultAnalyticsDto.StreamPerformance sp = new ResultAnalyticsDto.StreamPerformance();
        sp.setStreamId(id);
        sp.setStreamName(data.name);
        sp.setClassName(className);
        sp.setStudentCount(data.studentCount);
        sp.setAverage(data.studentCount > 0 ? round(data.sum / data.studentCount) : null);
        sp.setPassRate(data.studentCount > 0 ? round((double) data.passed / data.studentCount * 100) : 0);
        return sp;
    }

    private ResultAnalyticsDto.SubjectPerformance toSubjectPerformance(Long id, AggData data) {
        ResultAnalyticsDto.SubjectPerformance sp = new ResultAnalyticsDto.SubjectPerformance();
        sp.setSubjectId(id);
        sp.setSubjectName(data.name);
        sp.setStudentCount(data.studentCount);
        sp.setAverage(data.studentCount > 0 ? round(data.sum / data.studentCount) : null);
        sp.setPassRate(data.studentCount > 0 ? round((double) data.passed / data.studentCount * 100) : 0);
        sp.setHighestScore(data.highest);
        sp.setLowestScore(data.lowest);
        return sp;
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String gradeFor(double average) {
        if (average >= 75) return "A";
        if (average >= 65) return "B";
        if (average >= 45) return "C";
        if (average >= 30) return "D";
        return "F";
    }

    private ResultAnalyticsDto emptyAnalytics() {
        ResultAnalyticsDto dto = new ResultAnalyticsDto();
        dto.setOverallAverage(0.0);
        dto.setTotalStudentsAssessed(0);
        dto.setPassRate(0.0);
        dto.setFailRate(0.0);
        dto.setHighestScore(null);
        dto.setLowestScore(null);
        dto.setGradeDistribution(List.of());
        dto.setClassPerformance(List.of());
        dto.setStreamPerformance(List.of());
        dto.setSubjectPerformance(List.of());
        dto.setTopStudents(List.of());
        dto.setStudentsNeedingSupport(List.of());
        dto.setPerformanceTrends(List.of());
        dto.setTotalExaminations(0);
        dto.setSubmittedResults(0);
        dto.setApprovedResults(0);
        dto.setPublishedResults(0);
        return dto;
    }

    private static class AggData {
        String name;
        int studentCount;
        double sum;
        int passed;
        Double highest;
        Double lowest;

        AggData(String name) {
            this.name = name;
        }
    }

    private static class ClassStreamKey {
        Long classId;
        String className;

        ClassStreamKey(Long classId, String className) {
            this.classId = classId;
            this.className = className;
        }
    }
}
