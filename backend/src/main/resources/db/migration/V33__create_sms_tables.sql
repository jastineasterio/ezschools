CREATE TABLE IF NOT EXISTS sms_configurations (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL DEFAULT 'TWILIO',
    encrypted_api_key TEXT NOT NULL,
    encrypted_api_secret TEXT NOT NULL,
    sender_id VARCHAR(30),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    monthly_limit INTEGER,
    daily_limit INTEGER,
    remarks TEXT,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS sms_messages (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL,
    template_id BIGINT,
    recipient VARCHAR(30) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider_response JSONB,
    retry_count INTEGER NOT NULL DEFAULT 0,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    failed_at TIMESTAMP,
    error_message TEXT,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sms_templates (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    variables JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_sms_configurations_school_provider ON sms_configurations(school_id, provider) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_sms_configurations_school ON sms_configurations(school_id);
CREATE INDEX IF NOT EXISTS idx_sms_configurations_active ON sms_configurations(is_active);

CREATE INDEX IF NOT EXISTS idx_sms_messages_school ON sms_messages(school_id);
CREATE INDEX IF NOT EXISTS idx_sms_messages_status ON sms_messages(status);
CREATE INDEX IF NOT EXISTS idx_sms_messages_sent_at ON sms_messages(sent_at);
CREATE INDEX IF NOT EXISTS idx_sms_messages_template ON sms_messages(template_id);

CREATE INDEX IF NOT EXISTS idx_sms_templates_school ON sms_templates(school_id);
CREATE INDEX IF NOT EXISTS idx_sms_templates_active ON sms_templates(is_active);
