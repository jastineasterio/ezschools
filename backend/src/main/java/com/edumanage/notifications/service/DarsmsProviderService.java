package com.edumanage.notifications.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.net.ssl.SSLException;

@Service
public class DarsmsProviderService {

    private static final Logger log = LoggerFactory.getLogger(DarsmsProviderService.class);
    private static final Pattern TANZANIAN_MOBILE = Pattern.compile("^255[67][0-9]{8}$");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private static final String CODE_CONNECTION_FAILED = "SMS_PROVIDER_CONNECTION_FAILED";
    private static final String CODE_CONNECT_TIMEOUT = "SMS_PROVIDER_CONNECT_TIMEOUT";
    private static final String CODE_READ_TIMEOUT = "SMS_PROVIDER_READ_TIMEOUT";
    private static final String CODE_TLS_FAILURE = "SMS_PROVIDER_TLS_FAILURE";
    private static final String CODE_SERVER_ERROR = "SMS_PROVIDER_SERVER_ERROR";
    private static final String CODE_CLIENT_ERROR = "SMS_PROVIDER_CLIENT_ERROR";

    private final RestClient restClient;
    private final String apiKey;
    private final String senderId;
    private final String baseUrl;

    public DarsmsProviderService(
            RestClient.Builder restClientBuilder,
            @Value("${app.notifications.sms.api-key:}") String apiKey,
            @Value("${app.notifications.sms.sender-id:}") String senderId,
            @Value("${app.notifications.sms.base-url:https://dev.darsms.co.tz/api/v1}") String baseUrl) {
        this.restClient = restClientBuilder
                .requestFactory(timeoutRequestFactory())
                .build();
        this.apiKey = apiKey;
        this.senderId = senderId;
        this.baseUrl = baseUrl;
    }

    private SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    public boolean isConfigured() {
        return !apiKey.isBlank() && !senderId.isBlank();
    }

    public String send(String destination, String message) {
        return send(destination, message, apiKey, senderId).messageId();
    }

    /**
     * Sends an SMS using explicitly supplied credentials. Used by the per-school SMS
     * worker so each school sends through its own configured DARSMS credentials.
     * Never logs the apiKey or senderId.
     */
    public SendResult send(String destination, String message, String apiKey, String senderId) {
        if (apiKey == null || apiKey.isBlank() || senderId == null || senderId.isBlank()) {
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
                logSendResult(destination, null, null, "SMS_PROVIDER_REJECTED", false);
                throw new NotificationProviderException("SMS_PROVIDER_REJECTED");
            }
            String batchId = null;
            String messageId = null;
            Object data = response.get("data");
            if (data instanceof Map<?, ?> result) {
                Object b = result.get("batchId");
                if (b != null) {
                    batchId = String.valueOf(b);
                }
                messageId = extractMessageId(result);
            }
            logSendResult(destination, batchId, messageId, null, true);
            return new SendResult(batchId, messageId);
        } catch (RestClientResponseException e) {
            String code = providerFailureCode(e.getStatusCode().value(), e);
            logSendResult(destination, null, null, code, false, e.getStatusCode().value());
            throw new NotificationProviderException(code, e);
        } catch (RestClientException e) {
            String code = providerFailureCode(null, e);
            logSendResult(destination, null, null, code, false, null, e);
            throw new NotificationProviderException(code, e);
        }
    }

    /**
     * Extracts a provider message id from the send response leniently. The DARSMS
     * send response may carry the id under {@code data.messageIds[0]} or under a
     * single {@code data.messageId} field. A missing or unparseable id is not a
     * sending failure: the provider already accepted the message (success == true),
     * so we return {@code null} rather than rejecting an accepted SMS.
     */
    private static String extractMessageId(Map<?, ?> result) {
        if (result == null) {
            return null;
        }
        Object ids = result.get("messageIds");
        if (ids instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof String id) {
            return id;
        }
        Object single = result.get("messageId");
        if (single instanceof String sid && !sid.isBlank()) {
            return sid;
        }
        return null;
    }

    /**
     * Fetches the approved sender IDs for the given API key from DARSMS.
     * Only sender IDs with status APPROVED are returned.
     * Never logs the apiKey.
     */
    public List<String> getApprovedSenderIds(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new NotificationProviderException("SMS_NOT_CONFIGURED");
        }
        try {
            Map<?, ?> response = restClient.get()
                    .uri(baseUrl + "/integrations/sender-ids")
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                throw new NotificationProviderException("SMS_PROVIDER_REJECTED");
            }
            Object data = response.get("data");
            List<String> approved = new ArrayList<>();
            if (data instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> sender) {
                        Object statusObj = sender.get("status");
                        String status = statusObj == null ? null : String.valueOf(statusObj);
                        if ("APPROVED".equalsIgnoreCase(status)) {
                            Object id = sender.get("sender_id");
                            if (id instanceof String sid && !sid.isBlank()) {
                                approved.add(sid);
                            }
                        }
                    }
                }
            }
            return approved;
        } catch (RestClientResponseException e) {
            throw new NotificationProviderException(providerFailureCode(e.getStatusCode().value()), e);
        } catch (RestClientException e) {
            throw new NotificationProviderException("SMS_PROVIDER_UNAVAILABLE", e);
        }
    }

    /**
     * Fetches the current SMS balance for the given API key from DARSMS.
     * Never logs the apiKey.
     */
    public Map<String, Object> getBalance(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new NotificationProviderException("SMS_NOT_CONFIGURED");
        }
        try {
            Map<?, ?> response = restClient.get()
                    .uri(baseUrl + "/integrations/balance")
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                throw new NotificationProviderException("SMS_PROVIDER_REJECTED");
            }
            Object data = response.get("data");
            Map<String, Object> balance = new java.util.LinkedHashMap<>();
            if (data instanceof Map<?, ?> b) {
                b.forEach((k, v) -> balance.put(String.valueOf(k), v));
            }
            return balance;
        } catch (RestClientResponseException e) {
            throw new NotificationProviderException(providerFailureCode(e.getStatusCode().value()), e);
        } catch (RestClientException e) {
            throw new NotificationProviderException("SMS_PROVIDER_UNAVAILABLE", e);
        }
    }

    /**
     * Looks up the delivery state of a single message from DARSMS.
     * Never logs the apiKey or messageId.
     */
    public MessageStatus getMessageStatus(String apiKey, String messageId) {
        if (apiKey == null || apiKey.isBlank() || messageId == null || messageId.isBlank()) {
            throw new NotificationProviderException("SMS_NOT_CONFIGURED");
        }
        try {
            Map<?, ?> response = restClient.get()
                    .uri(baseUrl + "/integrations/sms/status/{messageId}", messageId)
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                throw new NotificationProviderException("SMS_PROVIDER_REJECTED");
            }
            Object data = response.get("data");
            String status = null;
            String deliveredAt = null;
            String errorMessage = null;
            if (data instanceof Map<?, ?> msg) {
                Object s = msg.get("status");
                if (s != null) status = String.valueOf(s);
                Object d = msg.get("deliveredAt");
                if (d != null) deliveredAt = String.valueOf(d);
                Object e = msg.get("errorMessage");
                if (e != null) errorMessage = String.valueOf(e);
            }
            return new MessageStatus(status, deliveredAt, errorMessage);
        } catch (RestClientResponseException ex) {
            throw new NotificationProviderException(providerFailureCode(ex.getStatusCode().value()), ex);
        } catch (RestClientException ex) {
            throw new NotificationProviderException("SMS_PROVIDER_UNAVAILABLE", ex);
        }
    }

    /**
     * Initiates an SMS credit purchase with DARSMS.
     * Never logs the apiKey or payerPhone.
     */
    public PurchaseResult initiatePurchase(String apiKey, Integer quantitySms, String payerPhone) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new NotificationProviderException("SMS_NOT_CONFIGURED");
        }
        if (quantitySms == null || quantitySms <= 0) {
            throw new NotificationProviderException("INVALID_QUANTITY");
        }
        if (payerPhone == null || payerPhone.isBlank()) {
            throw new NotificationProviderException("INVALID_PAYER_PHONE");
        }
        try {
            Map<?, ?> response = restClient.post()
                    .uri(baseUrl + "/integrations/purchases")
                    .header("X-API-Key", apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(Map.of("quantitySms", quantitySms, "payerPhone", payerPhone))
                    .retrieve()
                    .body(Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                throw new NotificationProviderException("SMS_PROVIDER_REJECTED");
            }
            Object data = response.get("data");
            String purchaseId = null, transactionRef = null, providerStatus = null, expiresAt = null;
            Long amount = null; Integer unitPrice = null, quantity = null; Boolean efdReceipt = null;
            if (data instanceof Map<?, ?> d) {
                Object id = d.get("purchaseId");
                if (id != null) purchaseId = String.valueOf(id);
                Object ref = d.get("transactionRef");
                if (ref != null) transactionRef = String.valueOf(ref);
                Object st = d.get("providerStatus");
                if (st != null) providerStatus = String.valueOf(st);
                Object exp = d.get("expiresAt");
                if (exp != null) expiresAt = String.valueOf(exp);
                Object a = d.get("amount");
                if (a != null) amount = toLong(a);
                Object up = d.get("unitPrice");
                if (up != null) unitPrice = toInteger(up);
                Object q = d.get("quantitySms");
                if (q != null) quantity = toInteger(q);
                Object e = d.get("efdReceipt");
                if (e != null) efdReceipt = Boolean.valueOf(String.valueOf(e));
            }
            return new PurchaseResult(purchaseId, transactionRef, amount, unitPrice, quantity,
                    providerStatus, expiresAt, efdReceipt);
        } catch (RestClientResponseException e) {
            throw new NotificationProviderException(providerFailureCode(e.getStatusCode().value()), e);
        } catch (RestClientException e) {
            throw new NotificationProviderException("SMS_PROVIDER_UNAVAILABLE", e);
        }
    }

    /**
     * Lists SMS credit purchases from DARSMS.
     * Never logs the apiKey.
     */
    public PurchasesResult listPurchases(String apiKey, String status, Integer page, Integer limit) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new NotificationProviderException("SMS_NOT_CONFIGURED");
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/integrations/purchases");
            if (status != null && !status.isBlank()) {
                builder.queryParam("status", status);
            }
            if (page != null) {
                builder.queryParam("page", String.valueOf(page));
            }
            if (limit != null) {
                builder.queryParam("limit", String.valueOf(limit));
            }
            Map<?, ?> response = restClient.get()
                    .uri(builder.toUriString())
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                throw new NotificationProviderException("SMS_PROVIDER_REJECTED");
            }
            List<Map<String, Object>> items = new ArrayList<>();
            Object data = response.get("data");
            if (data instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        Map<String, Object> entry = new java.util.LinkedHashMap<>();
                        m.forEach((k, v) -> entry.put(String.valueOf(k), v));
                        items.add(entry);
                    }
                }
            }
            Map<String, Object> meta = new java.util.LinkedHashMap<>();
            Object m = response.get("meta");
            if (m instanceof Map<?, ?> mm) {
                mm.forEach((k, v) -> meta.put(String.valueOf(k), v));
            }
            return new PurchasesResult(items, meta);
        } catch (RestClientResponseException e) {
            throw new NotificationProviderException(providerFailureCode(e.getStatusCode().value()), e);
        } catch (RestClientException e) {
            throw new NotificationProviderException("SMS_PROVIDER_UNAVAILABLE", e);
        }
    }

    /**
     * Re-triggers the payment push for a pending, failed or cancelled purchase.
     * Never logs the apiKey or phone.
     */
    public RetryResult retryPurchase(String apiKey, String purchaseId, String phone) {
        if (apiKey == null || apiKey.isBlank() || purchaseId == null || purchaseId.isBlank()) {
            throw new NotificationProviderException("SMS_NOT_CONFIGURED");
        }
        try {
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            if (phone != null && !phone.isBlank()) {
                body.put("phone", phone);
            }
            Map<?, ?> response = restClient.post()
                    .uri(baseUrl + "/integrations/purchases/{purchaseId}/pay", purchaseId)
                    .header("X-API-Key", apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                throw new NotificationProviderException("SMS_PROVIDER_REJECTED");
            }
            Object data = response.get("data");
            Boolean pushed = null, reinitiated = null; String transactionRef = null;
            if (data instanceof Map<?, ?> d) {
                Object p = d.get("pushed");
                if (p != null) pushed = Boolean.valueOf(String.valueOf(p));
                Object r = d.get("reinitiated");
                if (r != null) reinitiated = Boolean.valueOf(String.valueOf(r));
                Object ref = d.get("transactionRef");
                if (ref != null) transactionRef = String.valueOf(ref);
            }
            return new RetryResult(pushed, reinitiated, transactionRef);
        } catch (RestClientResponseException e) {
            throw new NotificationProviderException(providerFailureCode(e.getStatusCode().value()), e);
        } catch (RestClientException e) {
            throw new NotificationProviderException("SMS_PROVIDER_UNAVAILABLE", e);
        }
    }

    private static Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        try { return Long.valueOf(String.valueOf(value)); } catch (Exception e) { return null; }
    }

    private static Integer toInteger(Object value) {
        if (value instanceof Number n) return n.intValue();
        try { return Integer.valueOf(String.valueOf(value)); } catch (Exception e) { return null; }
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

    /**
     * Classifies a provider error into a granular failure code without exposing
     * credentials, authorization headers, message bodies, or full recipient numbers.
     *
     * @param status HTTP status code when available, null for transport errors.
     * @param cause  the underlying exception for transport errors; may be null for
     *               purely HTTP-status-based classification.
     */
    private String providerFailureCode(Integer status, Throwable cause) {
        if (status != null) {
            return switch (status) {
                case 401 -> "SMS_PROVIDER_AUTH_FAILED";
                case 403 -> "SMS_PROVIDER_ACCOUNT_DISABLED";
                case 422 -> "SMS_PROVIDER_REJECTED_RECIPIENT";
                case 500, 502, 503, 504 -> CODE_SERVER_ERROR;
                case 400, 404, 405, 406, 408, 409, 410, 413, 415, 429 -> CODE_CLIENT_ERROR;
                default -> status >= 500 ? CODE_SERVER_ERROR
                        : status >= 400 ? CODE_CLIENT_ERROR
                        : "SMS_PROVIDER_UNAVAILABLE";
            };
        }
        if (cause != null) {
            Throwable root = rootCause(cause);
            if (root instanceof SSLException || containsType(cause, SSLException.class)
                    || containsType(cause, javax.net.ssl.SSLHandshakeException.class)) {
                return CODE_TLS_FAILURE;
            }
            if (root instanceof SocketTimeoutException || containsType(cause, SocketTimeoutException.class)) {
                return CODE_READ_TIMEOUT;
            }
            if (root instanceof ConnectException || containsType(cause, ConnectException.class)) {
                return CODE_CONNECTION_FAILED;
            }
            if (cause instanceof ResourceAccessException) {
                String msg = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase(Locale.ROOT);
                if (msg.contains("timed out") || msg.contains("read timed out")) {
                    return CODE_READ_TIMEOUT;
                }
                if (msg.contains("connect timed out") || msg.contains("connection refused")
                        || msg.contains("connection reset")) {
                    return CODE_CONNECTION_FAILED;
                }
            }
        }
        return "SMS_PROVIDER_UNAVAILABLE";
    }

    private static Throwable rootCause(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }

    private static boolean containsType(Throwable t, Class<?> type) {
        Throwable cursor = t;
        while (cursor != null) {
            if (type.isInstance(cursor)) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() <= 4) {
            return "****";
        }
        return "****" + digits.substring(digits.length() - 4);
    }

    /**
     * Records a safe, non-secret diagnostic for an outbound SMS send attempt.
     * Never logs the apiKey, authorization headers, message body, or full phone number.
     */
    private void logSendResult(String destination, String batchId, String messageId,
                               String failureCode, boolean accepted) {
        logSendResult(destination, batchId, messageId, failureCode, accepted, null, null);
    }

    private void logSendResult(String destination, String batchId, String messageId,
                               String failureCode, boolean accepted, Integer httpStatus) {
        logSendResult(destination, batchId, messageId, failureCode, accepted, httpStatus, null);
    }

    private void logSendResult(String destination, String batchId, String messageId,
                               String failureCode, boolean accepted, Integer httpStatus, Throwable cause) {
        if (!log.isDebugEnabled() && !accepted && failureCode != null
                && !"SMS_PROVIDER_UNAVAILABLE".equals(failureCode)) {
            log.info("SMS send destination={} batchId={} messageId={} accepted={} code={} httpStatus={}",
                    maskPhone(destination), batchId, messageId, accepted, failureCode, httpStatus);
        } else if (log.isDebugEnabled()) {
            String detail = cause == null ? null : cause.getClass().getSimpleName();
            log.debug("SMS send destination={} batchId={} messageId={} accepted={} code={} httpStatus={} cause={}",
                    maskPhone(destination), batchId, messageId, accepted, failureCode, httpStatus, detail);
        }
    }

    public record MessageStatus(String status, String deliveredAt, String errorMessage) {
    }

    public record SendResult(String batchId, String messageId) {
    }

    public record PurchaseResult(String purchaseId, String transactionRef, Long amount, Integer unitPrice,
                                 Integer quantitySms, String providerStatus, String expiresAt, Boolean efdReceipt) {
    }

    public record PurchasesResult(List<Map<String, Object>> items, Map<String, Object> meta) {
    }

    public record RetryResult(Boolean pushed, Boolean reinitiated, String transactionRef) {
    }
}
