-- V51: Extend communication delivery tracking with SENT/DELIVERED lifecycle,
-- provider reference information, timestamps and recipient detail for the
-- Communication Center History and Delivery Details.
ALTER TABLE communication_deliveries
    ADD COLUMN IF NOT EXISTS sent_at TIMESTAMP;

ALTER TABLE communication_deliveries
    ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMP;

ALTER TABLE communication_deliveries
    ADD COLUMN IF NOT EXISTS batch_id VARCHAR(255);

ALTER TABLE communication_deliveries
    ADD COLUMN IF NOT EXISTS error_message TEXT;

ALTER TABLE communication_deliveries
    ADD COLUMN IF NOT EXISTS delivery_status VARCHAR(20);

ALTER TABLE communication_deliveries
    ADD COLUMN IF NOT EXISTS recipient_name VARCHAR(255);

ALTER TABLE communication_deliveries
    ADD COLUMN IF NOT EXISTS recipient_phone VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_communication_delivery_sent_at
    ON communication_deliveries(sent_at);
CREATE INDEX IF NOT EXISTS idx_communication_delivery_delivery_status
    ON communication_deliveries(delivery_status);
CREATE INDEX IF NOT EXISTS idx_communication_delivery_batch_id
    ON communication_deliveries(batch_id);

ALTER TABLE communication_campaigns
    ADD COLUMN IF NOT EXISTS delivered_count INTEGER NOT NULL DEFAULT 0;
