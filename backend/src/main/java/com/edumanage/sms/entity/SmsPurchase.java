package com.edumanage.sms.entity;

import com.edumanage.common.entity.UuidBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sms_purchases", uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(name = "uk_sms_purchases_purchase_id", columnNames = "purchase_id")
})
@Getter
@Setter
public class SmsPurchase extends UuidBaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "purchase_id", nullable = false, length = 100)
    private String purchaseId;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @Column(name = "quantity_sms")
    private Integer quantitySms;

    @Column(name = "unit_price")
    private Integer unitPrice;

    @Column(name = "subtotal_amount")
    private Long subtotalAmount;

    @Column(name = "vat_amount")
    private Long vatAmount;

    @Column(name = "total_amount")
    private Long totalAmount;

    @Column(name = "currency", length = 10, nullable = false)
    private String currency = "TZS";

    @Column(name = "efd_receipt", nullable = false)
    private boolean efdReceipt = false;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "PENDING";

    @Column(name = "payment_status", length = 30)
    private String paymentStatus;

    @Column(name = "channel", length = 30)
    private String channel;

    @Column(name = "can_retry", nullable = false)
    private boolean canRetry = false;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
