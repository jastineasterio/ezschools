CREATE TABLE IF NOT EXISTS notification_global_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(150) NOT NULL UNIQUE,
    setting_value TEXT,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notification_events (
    id BIGSERIAL PRIMARY KEY,
    event_code VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,
    default_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    target_role VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notification_event_channels (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES notification_events(id) ON DELETE CASCADE,
    channel VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(event_id, channel)
);

CREATE TABLE IF NOT EXISTS notification_templates (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    subject VARCHAR(500),
    content TEXT NOT NULL,
    variables JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS notification_history (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    event_code VARCHAR(100),
    channel VARCHAR(50),
    recipient_type VARCHAR(50),
    recipient_id BIGINT,
    recipient_identifier VARCHAR(255),
    subject VARCHAR(500),
    message TEXT,
    status VARCHAR(50),
    provider VARCHAR(100),
    provider_message_id VARCHAR(255),
    error_message TEXT,
    metadata JSONB,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_notification_templates_channel ON notification_templates(channel);
CREATE INDEX IF NOT EXISTS idx_notification_templates_active ON notification_templates(is_active);
CREATE INDEX IF NOT EXISTS idx_notification_history_channel ON notification_history(channel);
CREATE INDEX IF NOT EXISTS idx_notification_history_status ON notification_history(status);
CREATE INDEX IF NOT EXISTS idx_notification_history_event ON notification_history(event_code);
CREATE INDEX IF NOT EXISTS idx_notification_history_created ON notification_history(created_at);
