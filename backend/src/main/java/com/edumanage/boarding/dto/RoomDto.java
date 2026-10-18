package com.edumanage.boarding.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long dormitoryId;
    private String dormitoryName;
    private String roomNumber;
    private Integer capacity;
    private Integer currentOccupancy;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
