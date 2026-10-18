package com.edumanage.boarding.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DormitoryDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private String name;
    private String description;
    private Integer capacity;
    private String gender;
    private Long wardenId;
    private String wardenName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
