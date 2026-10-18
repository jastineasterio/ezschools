package com.edumanage.streams.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long classId;
    private String className;
    private String name;
    private String code;
    private Integer capacity;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
