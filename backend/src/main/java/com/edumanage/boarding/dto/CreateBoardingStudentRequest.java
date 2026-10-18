package com.edumanage.boarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBoardingStudentRequest {
    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Dormitory ID is required")
    private Long dormitoryId;

    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotNull(message = "Bed ID is required")
    private Long bedId;

    @NotBlank(message = "Status is required")
    private String status;
}
