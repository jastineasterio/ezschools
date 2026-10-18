package com.edumanage.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BorrowingDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long bookCopyId;
    private String bookTitle;
    private String barcode;
    private Long memberId;
    private String memberName;
    private String memberType;
    private String membershipNumber;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private String status;
    private String remarks;
    private BigDecimal fineAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
