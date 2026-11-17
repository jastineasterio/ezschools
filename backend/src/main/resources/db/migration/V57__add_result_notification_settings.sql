-- V57: Add school-specific Results Notifications SMS template setting.
-- Each school can customize the SMS template used for Results Notifications.
-- A missing row means the school uses the built-in default template.

CREATE TABLE IF NOT EXISTS result_notification_settings (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL UNIQUE,
    sms_template TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_result_notification_settings_school_id ON result_notification_settings(school_id);
