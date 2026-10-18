package com.edumanage.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookCategoryDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private String name;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
