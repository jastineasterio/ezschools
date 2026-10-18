package com.edumanage.boarding.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BedDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long roomId;
    private String roomNumber;
    private String bedNumber;
    private Long studentId;
    private String studentName;
    private String status;
    private LocalDate allocatedDate;
    private LocalDate releasedDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
