package com.edumanage.boarding.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardingStudentDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private Long dormitoryId;
    private String dormitoryName;
    private Long roomId;
    private String roomNumber;
    private Long bedId;
    private String bedNumber;
    private LocalDate allocatedDate;
    private LocalDate releasedDate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
