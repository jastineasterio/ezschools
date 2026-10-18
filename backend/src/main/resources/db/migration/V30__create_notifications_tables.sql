CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    action_url VARCHAR(500),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(school_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_notifications_school ON notifications(school_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read ON notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_notification_preferences_user ON notification_preferences(user_id);
