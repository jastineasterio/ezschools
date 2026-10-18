package com.edumanage.sms.service;

import org.springframework.stereotype.Service;

@Service
public class MockSmsProviderService {

    public String sendSms(String recipient, String message, String apiKey, String apiSecret, String senderId) {
        return "{\"status\":\"success\",\"messageId\":\"mock-" + System.currentTimeMillis() + "\",\"recipient\":\"" + recipient + "\"}";
    }
}
