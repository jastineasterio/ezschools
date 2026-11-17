-- V56: Create result notification campaign and delivery tracking tables
-- for the Manager Results Notifications bulk SMS feature.

CREATE TABLE IF NOT EXISTS result_notifications (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    school_id BIGINT NOT NULL,
    examination_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    stream_id BIGINT,
    recipient_type VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    total_recipients INTEGER NOT NULL DEFAULT 0,
    excluded_count INTEGER NOT NULL DEFAULT 0,
    successful_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    pending_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    initiated_by BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_result_notification_school_id ON result_notifications(school_id);
CREATE INDEX IF NOT EXISTS idx_result_notification_idempotency_key ON result_notifications(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_result_notification_status ON result_notifications(status);
CREATE INDEX IF NOT EXISTS idx_result_notification_created_at ON result_notifications(created_at);

CREATE TABLE IF NOT EXISTS result_notification_deliveries (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    notification_id BIGINT NOT NULL,
    sms_message_id BIGINT,
    recipient_type VARCHAR(20) NOT NULL,
    recipient_id BIGINT NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    recipient_phone VARCHAR(50),
    student_id BIGINT,
    student_name VARCHAR(255),
    admission_number VARCHAR(100),
    channel VARCHAR(10) NOT NULL DEFAULT 'SMS',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider_message_id VARCHAR(255),
    batch_id VARCHAR(255),
    failure_code VARCHAR(60),
    error_message TEXT,
    delivery_status VARCHAR(20),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    failed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_result_notification_delivery_notification
        FOREIGN KEY (notification_id) REFERENCES result_notifications(id),
    CONSTRAINT fk_result_notification_delivery_sms_message
        FOREIGN KEY (sms_message_id) REFERENCES sms_messages(id)
);

CREATE INDEX IF NOT EXISTS idx_result_notification_delivery_notification_id ON result_notification_deliveries(notification_id);
CREATE INDEX IF NOT EXISTS idx_result_notification_delivery_status ON result_notification_deliveries(status);
CREATE INDEX IF NOT EXISTS idx_result_notification_delivery_recipient ON result_notification_deliveries(recipient_type, recipient_id);
CREATE INDEX IF NOT EXISTS idx_result_notification_delivery_sms_message_id ON result_notification_deliveries(sms_message_id);
