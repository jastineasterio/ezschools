ALTER TABLE sms_messages ADD COLUMN IF NOT EXISTS provider_message_id VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_sms_messages_provider_message_id ON sms_messages(provider_message_id);
