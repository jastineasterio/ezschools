package com.edumanage.boarding.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardingAttendanceDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private Long dormitoryId;
    private String dormitoryName;
    private LocalDate attendanceDate;
    private String status;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
