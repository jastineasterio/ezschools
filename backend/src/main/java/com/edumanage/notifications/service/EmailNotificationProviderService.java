package com.edumanage.notifications.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationProviderService {

    private final JavaMailSender mailSender;
    private final String username;
    private final String password;
    private final String from;
    private final String host;

    public EmailNotificationProviderService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${app.notifications.email.from:}") String from,
            @Value("${spring.mail.host:}") String host) {
        this.mailSender = mailSender;
        this.username = username;
        this.password = password;
        this.from = from;
        this.host = host;
    }

    public boolean isConfigured() {
        return !host.isBlank() && !username.isBlank() && !password.isBlank() && !from.isBlank();
    }

    public void send(String destination, String subject, String body) {
        if (!isConfigured()) {
            throw new NotificationProviderException("EMAIL_NOT_CONFIGURED");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(destination);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new NotificationProviderException("EMAIL_PROVIDER_UNAVAILABLE", e);
        }
    }
}
