package com.edumanage.resultsnotifications.service;

import com.edumanage.sms.service.SmsMessageStatusEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResultNotificationSmsStatusListener {

    private static final Logger log = LoggerFactory.getLogger(ResultNotificationSmsStatusListener.class);

    private final ResultNotificationService resultNotificationService;

    /**
     * Keeps ResultNotificationDelivery in sync with SmsMessage status changes produced by
     * the SMS worker or by delivery-receipt polling.
     *
     * The update is idempotent and school-scoped, so duplicate callbacks or queue
     * messages cannot corrupt delivery counts or touch another school's records. No SMS
     * is sent during this sync.
     */
    @EventListener
    public void onSmsMessageStatus(SmsMessageStatusEvent event) {
        try {
            resultNotificationService.syncDeliveryFromSmsMessage(
                    event.getSchoolId(), event.getSmsMessageId());
        } catch (Exception e) {
            log.warn("Failed to sync result notification delivery for smsMessageId={} schoolId={} status={}: {}",
                    event.getSmsMessageId(), event.getSchoolId(), event.getStatus(), e.getMessage());
        }
    }
}
