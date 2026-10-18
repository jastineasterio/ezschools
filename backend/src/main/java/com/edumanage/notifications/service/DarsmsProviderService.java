package com.edumanage.notifications.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class DarsmsProviderService {

    private static final Pattern TANZANIAN_MOBILE = Pattern.compile("^255[67][0-9]{8}$");
    private final RestClient restClient;
    private final String apiKey;
    private final String senderId;
    private final String baseUrl;

    public DarsmsProviderService(
            RestClient.Builder restClientBuilder,
            @Value("${app.notifications.sms.api-key:}") String apiKey,
            @Value("${app.notifications.sms.sender-id:}") String senderId,
            @Value("${app.notifications.sms.base-url:https://dev.darsms.co.tz/api/v1}") String baseUrl) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
        this.senderId = senderId;
        this.baseUrl = baseUrl;
    }

    public boolean isConfigured() {
        return !apiKey.isBlank() && !senderId.isBlank();
    }

    public String send(String destination, String message) {
        if (!isConfigured()) {
            throw new NotificationProviderException("SMS_NOT_CONFIGURED");
        }

        String normalized = normalizeTanzanianPhone(destination);
        try {
            Map<?, ?> response = restClient.post()
                    .uri(baseUrl + "/integrations/sms/send")
                    .header("X-API-Key", apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(Map.of("senderId", senderId, "to", List.of(normalized), "message", message))
                    .retrieve()
                    .body(Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                throw new NotificationProviderException("SMS_PROVIDER_REJECTED");
            }
            Object data = response.get("data");
            if (!(data instanceof Map<?, ?> result)) {
                throw new NotificationProviderException("SMS_PROVIDER_INVALID_RESPONSE");
            }
            Object messageIds = result.get("messageIds");
            if (!(messageIds instanceof List<?> ids) || ids.isEmpty() || !(ids.get(0) instanceof String id)) {
                throw new NotificationProviderException("SMS_PROVIDER_INVALID_RESPONSE");
            }
            return id;
        } catch (RestClientResponseException e) {
            throw new NotificationProviderException(providerFailureCode(e.getStatusCode().value()), e);
        } catch (RestClientException e) {
            throw new NotificationProviderException("SMS_PROVIDER_UNAVAILABLE", e);
        }
    }

    public static String normalizeTanzanianPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new NotificationProviderException("INVALID_PHONE");
        }

        String normalized = phone.trim().replaceAll("[\\s()-]", "");
        if (normalized.startsWith("+")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("0")) {
            normalized = "255" + normalized.substring(1);
        }
        if (!TANZANIAN_MOBILE.matcher(normalized).matches()) {
            throw new NotificationProviderException("INVALID_PHONE");
        }
        return normalized;
    }

    private String providerFailureCode(int status) {
        return switch (status) {
            case 401 -> "SMS_PROVIDER_AUTH_FAILED";
            case 403 -> "SMS_PROVIDER_ACCOUNT_DISABLED";
            case 422 -> "SMS_PROVIDER_REJECTED_RECIPIENT";
            case 429 -> "SMS_PROVIDER_RATE_LIMITED";
            default -> "SMS_PROVIDER_UNAVAILABLE";
        };
    }
}
