ALTER TABLE notification_templates ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
