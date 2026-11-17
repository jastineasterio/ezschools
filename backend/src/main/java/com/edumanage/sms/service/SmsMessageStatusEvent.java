package com.edumanage.sms.service;

/**
 * Published whenever an {@link com.edumanage.sms.entity.SmsMessage} reaches a new
 * delivery-relevant status (accepted by the provider, delivered, or failed).
 *
 * The results notifications module listens to this event to keep
 * {@link com.edumanage.resultsnotifications.entity.ResultNotificationDelivery} in
 * sync without coupling the SMS module to the results notifications module.
 */
public class SmsMessageStatusEvent {

    private final Long smsMessageId;
    private final Long schoolId;
    private final String status;

    public SmsMessageStatusEvent(Long smsMessageId, Long schoolId, String status) {
        this.smsMessageId = smsMessageId;
        this.schoolId = schoolId;
        this.status = status;
    }

    public Long getSmsMessageId() {
        return smsMessageId;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public String getStatus() {
        return status;
    }
}
