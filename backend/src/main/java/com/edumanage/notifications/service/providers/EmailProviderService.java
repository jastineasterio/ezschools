package com.edumanage.notifications.service.providers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class EmailProviderService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailProviderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public Map<String, Object> sendEmail(String to, String subject, String body) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (to == null || to.isBlank()) {
                result.put("success", false);
                result.put("error", "Invalid recipient");
                return result;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail.isBlank() ? "noreply@edumanage.local" : fromEmail);
            message.setTo(to);
            message.setSubject(subject != null && !subject.isBlank() ? subject : (body != null && body.length() > 60 ? body.substring(0,60) : body));
            message.setText(body);
            mailSender.send(message);
            result.put("success", true);
            result.put("status", "SENT");
            result.put("providerMessageId", "email-" + System.currentTimeMillis());
            return result;
        } catch (Exception e) {
            log.error("Email send failed", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }
}
