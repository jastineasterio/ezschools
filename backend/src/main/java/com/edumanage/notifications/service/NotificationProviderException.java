package com.edumanage.notifications.service;

public class NotificationProviderException extends RuntimeException {
    private final String failureCode;

    public NotificationProviderException(String failureCode) {
        super(failureCode);
        this.failureCode = failureCode;
    }

    public NotificationProviderException(String failureCode, Throwable cause) {
        super(failureCode, cause);
        this.failureCode = failureCode;
    }

    public String getFailureCode() {
        return failureCode;
    }
}
