package com.edumanage.boarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBoardingAttendanceRequest {
    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Dormitory ID is required")
    private Long dormitoryId;

    @NotNull(message = "Date is required")
    private LocalDate attendanceDate;

    @NotBlank(message = "Status is required")
    private String status;

    private String remarks;
}
