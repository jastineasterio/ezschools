package com.edumanage.teacherworkspace.dto;

import com.edumanage.teacherassignments.dto.TeacherAssignmentDto;
import com.edumanage.teachertimetables.dto.TeacherTimetableDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherDashboardDto {
    private LocalDate today;
    private String timeZone;
    private List<TeacherAssignmentDto> assignments;
    private List<TeacherTimetableDto> timetable;
    private List<TeacherUpcomingExaminationDto> upcomingExaminations;
    private long assignedClassesCount;
    private long assignedSubjectsCount;
    private long totalUniqueStudents;
    private long upcomingExaminationsCount;
    private long attendanceTasksCount;
    private long unreadNotificationsCount;
    private long announcementsCount;
    private List<TeacherDashboardChartDto> performanceChart;
    private List<TeacherAttendanceChartDto> attendanceChart;
    private List<TeacherClassPerformanceDto> classPerformance;
    private List<TeacherSubjectPerformanceDto> subjectPerformance;
    private List<TeacherStudentDistributionDto> studentDistribution;
}
