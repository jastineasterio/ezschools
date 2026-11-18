package com.edumanage.audit.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Redacts sensitive values so they are never persisted in audit log metadata or
 * descriptions. Covers passwords, hashes, tokens, OTPs, secrets, API keys and
 * payment credentials.
 */
public final class AuditRedactor {

    private static final String REDACTED = "[REDACTED]";

    private static final Pattern SENSITIVE_KEY = Pattern.compile(
            "(?i)(password|passwd|pwd|secret|token|refresh.?token|access.?token|" +
                    "api.?key|apikey|credential|otp|authorization|private.?key|" +
                    "payment.?credential|cvv|pin)");

    private AuditRedactor() {
    }

    public static String redact(String value) {
        if (value == null) {
            return null;
        }
        if (SENSITIVE_KEY.matcher(value.trim()).matches()) {
            return REDACTED;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> redactMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        Map<String, Object> redacted = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (SENSITIVE_KEY.matcher(key).matches()) {
                redacted.put(key, REDACTED);
            } else if (value instanceof Map) {
                redacted.put(key, redactMap((Map<String, Object>) value));
            } else {
                redacted.put(key, value);
            }
        }
        return redacted;
    }
}
