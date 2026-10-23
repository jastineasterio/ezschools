package com.edumanage.dashboards.dto;

import com.edumanage.boarding.dto.BoardingAttendanceDto;
import com.edumanage.boarding.dto.BoardingStudentDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Landing payload for the boarding warden dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WardenDashboardDto {

    private Long schoolId;
    private String schoolName;
    private long totalDormitories;
    private long assignedDormitories;
    private long totalRooms;
    private long totalBeds;
    private long occupiedBeds;
    private long boardingStudents;
    private LocalDate attendanceDate;
    private long presentToday;
    private long absentToday;
    private List<BoardingStudentDto> students = new ArrayList<>();
    private List<BoardingAttendanceDto> recentAttendance = new ArrayList<>();
}
