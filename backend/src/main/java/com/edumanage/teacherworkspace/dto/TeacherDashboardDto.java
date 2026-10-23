package com.edumanage.teacherworkspace.dto;

import com.edumanage.teacherassignments.dto.TeacherAssignmentDto;
import com.edumanage.teachertimetables.dto.TeacherTimetableDto;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class TeacherDashboardDto {
    private LocalDate today;
    private String timeZone;
    private List<TeacherAssignmentDto> assignments;
    private List<TeacherTimetableDto> timetable;
    private List<TeacherUpcomingExaminationDto> upcomingExaminations;
}
