package com.edumanage.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookCopyDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long bookId;
    private String bookTitle;
    private String barcode;
    private String status;
    private String conditionStatus;
    private String remarks;
    private LocalDate acquiredDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
