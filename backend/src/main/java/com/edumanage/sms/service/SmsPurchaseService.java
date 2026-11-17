package com.edumanage.sms.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.notifications.service.DarsmsProviderService;
import com.edumanage.notifications.service.NotificationProviderException;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.schools.entity.School;
import com.edumanage.sms.dto.CreateSmsPurchaseRequest;
import com.edumanage.sms.dto.SmsPurchaseDto;
import com.edumanage.sms.entity.SmsPurchase;
import com.edumanage.sms.repository.SmsPurchaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SmsPurchaseService {

    private final SmsPurchaseRepository smsPurchaseRepository;
    private final SmsConfigurationService smsConfigurationService;
    private final DarsmsProviderService darsmsProviderService;
    private final SchoolRepository schoolRepository;

    public SmsPurchaseService(SmsPurchaseRepository smsPurchaseRepository,
                              SmsConfigurationService smsConfigurationService,
                              DarsmsProviderService darsmsProviderService,
                              SchoolRepository schoolRepository) {
        this.smsPurchaseRepository = smsPurchaseRepository;
        this.smsConfigurationService = smsConfigurationService;
        this.darsmsProviderService = darsmsProviderService;
        this.schoolRepository = schoolRepository;
    }

    @Transactional
    public SmsPurchaseDto createPurchase(Long schoolId, CreateSmsPurchaseRequest request) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        var creds = smsConfigurationService.getCredentials(schoolId);
        if (creds.apiKey() == null || creds.apiKey().isBlank()) {
            throw new BadRequestException("SMS API key is not configured for this school");
        }

        DarsmsProviderService.PurchaseResult result = darsmsProviderService.initiatePurchase(
                creds.apiKey(), request.getQuantitySms(), request.getPayerPhone());

        if (result.purchaseId() == null) {
            throw new BadRequestException("DARSMS did not return a purchase reference");
        }

        if (smsPurchaseRepository.existsByPurchaseId(result.purchaseId())) {
            throw new BadRequestException("SMS purchase already recorded");
        }

        SmsPurchase purchase = new SmsPurchase();
        purchase.setSchoolId(schoolId);
        purchase.setPurchaseId(result.purchaseId());
        purchase.setTransactionRef(result.transactionRef());
        purchase.setQuantitySms(request.getQuantitySms());
        purchase.setUnitPrice(result.unitPrice());
        purchase.setSubtotalAmount(result.amount());
        purchase.setTotalAmount(result.amount());
        purchase.setCurrency("TZS");
        purchase.setStatus(result.providerStatus() != null ? result.providerStatus() : "PENDING");
        purchase.setPaymentStatus(result.providerStatus());
        purchase.setChannel("mobile_money");
        purchase.setCanRetry(true);
        purchase.setExpiresAt(parseExpiresAt(result.expiresAt()));

        var saved = smsPurchaseRepository.save(purchase);
        return mapToDto(saved);
    }

    public Page<SmsPurchaseDto> getPurchases(Long schoolId, String status, Pageable pageable) {
        if (!schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException("School not found");
        }
        Page<SmsPurchase> page;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            page = smsPurchaseRepository.findBySchoolIdAndStatus(schoolId, status, pageable);
        } else {
            page = smsPurchaseRepository.findBySchoolId(schoolId, pageable);
        }
        return page.map(this::mapToDto);
    }

    @Transactional
    public SmsPurchaseDto retryPurchase(Long schoolId, String purchaseId, String phone) {
        SmsPurchase purchase = smsPurchaseRepository.findByPurchaseId(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("SMS purchase not found"));
        if (!purchase.getSchoolId().equals(schoolId)) {
            throw new BadRequestException("Access denied");
        }

        var creds = smsConfigurationService.getCredentials(schoolId);
        DarsmsProviderService.RetryResult result = darsmsProviderService.retryPurchase(
                creds.apiKey(), purchaseId, phone);

        if (result.transactionRef() != null) {
            purchase.setTransactionRef(result.transactionRef());
        }
        if (result.reinitiated() != null) {
            purchase.setCanRetry(result.reinitiated());
        }
        purchase.setStatus("PENDING");
        purchase.setPaymentStatus("PENDING");

        var saved = smsPurchaseRepository.save(purchase);
        return mapToDto(saved);
    }

    private SmsPurchaseDto mapToDto(SmsPurchase purchase) {
        SmsPurchaseDto dto = new SmsPurchaseDto();
        dto.setId(purchase.getId());
        dto.setUuid(purchase.getUuid() != null ? purchase.getUuid().toString() : null);
        dto.setSchoolId(purchase.getSchoolId());
        dto.setPurchaseId(purchase.getPurchaseId());
        dto.setInvoiceNumber(purchase.getInvoiceNumber());
        dto.setTransactionRef(purchase.getTransactionRef());
        dto.setQuantitySms(purchase.getQuantitySms());
        dto.setUnitPrice(purchase.getUnitPrice());
        dto.setSubtotalAmount(purchase.getSubtotalAmount());
        dto.setVatAmount(purchase.getVatAmount());
        dto.setTotalAmount(purchase.getTotalAmount());
        dto.setCurrency(purchase.getCurrency());
        dto.setEfdReceipt(purchase.isEfdReceipt());
        dto.setStatus(purchase.getStatus());
        dto.setPaymentStatus(purchase.getPaymentStatus());
        dto.setChannel(purchase.getChannel());
        dto.setCanRetry(purchase.isCanRetry());
        dto.setExpiresAt(purchase.getExpiresAt());
        dto.setCreatedAt(purchase.getCreatedAt());
        dto.setUpdatedAt(purchase.getUpdatedAt());
        return dto;
    }

    private LocalDateTime parseExpiresAt(String expiresAt) {
        if (expiresAt == null || expiresAt.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(expiresAt);
        } catch (Exception e) {
            return null;
        }
    }
}
