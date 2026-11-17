CREATE TABLE IF NOT EXISTS sms_purchases (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL,
    purchase_id VARCHAR(100) NOT NULL UNIQUE,
    invoice_number VARCHAR(100),
    transaction_ref VARCHAR(100),
    quantity_sms INTEGER,
    unit_price INTEGER,
    subtotal_amount BIGINT,
    vat_amount BIGINT,
    total_amount BIGINT,
    currency VARCHAR(10) NOT NULL DEFAULT 'TZS',
    efd_receipt BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(30),
    channel VARCHAR(30),
    can_retry BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sms_purchases_school ON sms_purchases(school_id);
CREATE INDEX IF NOT EXISTS idx_sms_purchases_purchase_id ON sms_purchases(purchase_id);
CREATE INDEX IF NOT EXISTS idx_sms_purchases_status ON sms_purchases(status);
