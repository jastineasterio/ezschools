CREATE TABLE IF NOT EXISTS analytics_snapshots (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT,
    snapshot_date DATE NOT NULL,
    metric_type VARCHAR(100) NOT NULL,
    metric_value JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_analytics_snapshots_school ON analytics_snapshots(school_id);
CREATE INDEX IF NOT EXISTS idx_analytics_snapshots_date ON analytics_snapshots(snapshot_date);
CREATE INDEX IF NOT EXISTS idx_analytics_snapshots_type ON analytics_snapshots(metric_type);
