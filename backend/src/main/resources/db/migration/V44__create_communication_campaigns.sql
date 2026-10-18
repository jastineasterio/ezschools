CREATE TABLE communication_campaigns (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    sender_user_id BIGINT NOT NULL REFERENCES users(id),
    idempotency_key VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    category VARCHAR(40) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    recipient_group VARCHAR(30) NOT NULL,
    intended_count INTEGER NOT NULL DEFAULT 0,
    excluded_count INTEGER NOT NULL DEFAULT 0,
    accepted_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (school_id, sender_user_id, idempotency_key)
);

CREATE TABLE communication_deliveries (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL REFERENCES communication_campaigns(id) ON DELETE CASCADE,
    recipient_type VARCHAR(30) NOT NULL,
    recipient_id BIGINT NOT NULL,
    channel VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider_message_id VARCHAR(255),
    failure_code VARCHAR(60),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (campaign_id, recipient_type, recipient_id, channel)
);

CREATE INDEX idx_communication_campaign_school_created
    ON communication_campaigns (school_id, created_at DESC);
CREATE INDEX idx_communication_campaign_sender
    ON communication_campaigns (sender_user_id, created_at DESC);
CREATE INDEX idx_communication_delivery_campaign_status
    ON communication_deliveries (campaign_id, status);

CREATE TABLE notification_manager_channel_permissions (
    manager_user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    sms_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO notification_global_settings (setting_key, setting_value, description)
VALUES
    ('notifications.global.enabled', 'true', 'Enable outbound notification campaigns'),
    ('sms.enabled', 'true', 'Enable SMS notification campaigns'),
    ('email.enabled', 'true', 'Enable email notification campaigns')
ON CONFLICT (setting_key) DO NOTHING;
