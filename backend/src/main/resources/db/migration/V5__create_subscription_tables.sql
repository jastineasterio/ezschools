CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    package_id BIGINT NOT NULL REFERENCES packages(id),
    status VARCHAR(20) NOT NULL DEFAULT 'TRIAL',
    start_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_date TIMESTAMP,
    billing_period VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    price NUMERIC(10,2) NOT NULL DEFAULT 0,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    trial_ends_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subscriptions_school ON subscriptions(school_id);
CREATE INDEX idx_subscriptions_package ON subscriptions(package_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
