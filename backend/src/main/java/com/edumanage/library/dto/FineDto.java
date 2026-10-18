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
public class FineDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long borrowingId;
    private Long memberId;
    private String memberName;
    private String membershipNumber;
    private BigDecimal fineAmount;
    private BigDecimal paidAmount;
    private BigDecimal balance;
    private String fineType;
    private String status;
    private LocalDate paidDate;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
