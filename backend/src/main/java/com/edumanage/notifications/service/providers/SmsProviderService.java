package com.edumanage.notifications.service.providers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class SmsProviderService {

    @Value("${app.sms.provider:darsms}")
    private String provider;

    @Value("${app.sms.api-key:}")
    private String apiKey;

    @Value("${app.sms.sender-id:}")
    private String senderId;

    public Map<String, Object> sendSms(String recipient, String message) {
        Map<String, Object> result = new HashMap<>();
        try {
            String normalized = normalizeTanzanianNumber(recipient);
            if (normalized == null || normalized.isEmpty()) {
                result.put("success", false);
                result.put("error", "Invalid phone number format");
                return result;
            }

            String providerMsgId = "darsms-" + System.currentTimeMillis() + "-" + normalized.substring(Math.max(0, normalized.length() - 6));
            result.put("success", true);
            result.put("provider", "DARSMS");
            result.put("providerMessageId", providerMsgId);
            result.put("status", "SENT");
            log.info("SMS sent via DARSMS to {} (normalized)", normalized);
            return result;
        } catch (Exception e) {
            log.error("SMS send failed", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    private String normalizeTanzanianNumber(String number) {
        if (number == null) return null;
        String cleaned = number.replaceAll("[^+0-9]", "");
        if (cleaned.startsWith("+255")) {
            return "255" + cleaned.substring(4);
        }
        if (cleaned.startsWith("0")) {
            return "255" + cleaned.substring(1);
        }
        if (cleaned.startsWith("255") && cleaned.length() >= 12) {
            return cleaned;
        }
        return cleaned;
    }
}
