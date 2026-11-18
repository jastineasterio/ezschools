package com.edumanage.exception;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.edumanage.notifications.service.NotificationProviderException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex, HttpServletRequest request) {
        var body = new HashMap<String, Object>();
        body.put("success", false);
        body.put("message", ex.getMessage());
        body.put("timestamp", java.time.Instant.now().toString());
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var errors = new HashMap<String, String>();
        for (var error : ex.getBindingResult().getAllErrors()) {
            if (error instanceof FieldError fieldError) {
                errors.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                errors.put(error.getObjectName(), error.getDefaultMessage());
            }
        }
        var body = new HashMap<String, Object>();
        body.put("success", false);
        body.put("message", "Validation failed");
        body.put("errors", errors);
        body.put("timestamp", java.time.Instant.now().toString());
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableMessage(HttpMessageNotReadableException ex,
                                                                       HttpServletRequest request) {
        Throwable cause = ex.getMostSpecificCause();
        String property = extractUnknownProperty(cause);
        String target = extractTargetType(cause);
        logger.warn("Malformed request body at {} (cause type: {}){}",
                request.getRequestURI(), cause == null ? "unknown" : cause.getClass().getSimpleName(),
                property == null ? "" : " - unrecognized field '" + property + "'"
                        + (target == null ? "" : " in " + target));

        var body = new HashMap<String, Object>();
        body.put("success", false);
        body.put("message", property == null
                ? "Malformed request body"
                : "Invalid request: unrecognized field '" + property + "'" + (target == null ? "" : " for " + target));
        body.put("timestamp", java.time.Instant.now().toString());
        body.put("path", request.getRequestURI());
        if (property != null) {
            body.put("errors", Map.of(property, "Unrecognized field"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private String extractUnknownProperty(Throwable cause) {
        if (cause == null) {
            return null;
        }
        if (cause instanceof UnrecognizedPropertyException upe) {
            return upe.getPropertyName();
        }
        if (cause instanceof JsonMappingException jme) {
            var ref = jme.getPath();
            if (ref != null && !ref.isEmpty()) {
                var last = ref.get(ref.size() - 1);
                if (last.getFieldName() != null) {
                    return last.getFieldName();
                }
            }
        }
        return extractUnknownProperty(cause.getCause());
    }

    private String extractTargetType(Throwable cause) {
        if (cause == null) {
            return null;
        }
        if (cause instanceof UnrecognizedPropertyException upe && upe.getReferringClass() != null) {
            return upe.getReferringClass().getName();
        }
        return extractTargetType(cause.getCause());
    }

    @ExceptionHandler(NotificationProviderException.class)
    public ResponseEntity<Map<String, Object>> handleNotificationProvider(
            NotificationProviderException ex, HttpServletRequest request) {
        var body = new HashMap<String, Object>();
        body.put("success", false);
        body.put("message", ex.getFailureCode());
        body.put("timestamp", java.time.Instant.now().toString());
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(httpStatusFor(ex.getFailureCode())).body(body);
    }

    private HttpStatus httpStatusFor(String failureCode) {
        return switch (failureCode) {
            case "SMS_PROVIDER_AUTH_FAILED" -> HttpStatus.UNAUTHORIZED;
            case "SMS_PROVIDER_ACCOUNT_DISABLED" -> HttpStatus.FORBIDDEN;
            case "SMS_PROVIDER_RATE_LIMITED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "SMS_PROVIDER_UNAVAILABLE", "SMS_PROVIDER_REJECTED", "SMS_PROVIDER_INVALID_RESPONSE" ->
                    HttpStatus.BAD_GATEWAY;
            case "SMS_NOT_CONFIGURED", "EMAIL_NOT_CONFIGURED", "INVALID_PHONE", "INVALID_QUANTITY", "INVALID_PAYER_PHONE" ->
                    HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                            HttpServletRequest request) {
        boolean uniqueViolation = isUniqueViolation(ex);
        var body = new HashMap<String, Object>();
        body.put("success", false);
        body.put("message", uniqueViolation
                ? "This record already exists. A duplicate was not created."
                : "Data integrity violation: " + ex.getMessage());
        body.put("timestamp", java.time.Instant.now().toString());
        body.put("path", request.getRequestURI());
        HttpStatus status = uniqueViolation ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(body);
    }

    private boolean isUniqueViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getRootCause();
        while (cause != null) {
            if (cause instanceof SQLException sqlException) {
                if ("23505".equals(sqlException.getSQLState())) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, HttpServletRequest request) {
        logger.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        var body = new HashMap<String, Object>();
        body.put("success", false);
        body.put("message", "Internal server error");
        body.put("timestamp", java.time.Instant.now().toString());
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
