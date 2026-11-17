ALTER TABLE sms_messages ADD COLUMN IF NOT EXISTS batch_id VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_sms_messages_batch_id ON sms_messages(batch_id);
