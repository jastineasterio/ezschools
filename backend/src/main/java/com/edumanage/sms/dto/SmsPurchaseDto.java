package com.edumanage.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsPurchaseDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private String purchaseId;
    private String invoiceNumber;
    private String transactionRef;
    private Integer quantitySms;
    private Integer unitPrice;
    private Long subtotalAmount;
    private Long vatAmount;
    private Long totalAmount;
    private String currency;
    private boolean efdReceipt;
    private String status;
    private String paymentStatus;
    private String channel;
    private boolean canRetry;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
