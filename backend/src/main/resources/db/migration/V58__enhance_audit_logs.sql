ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS actor_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS actor_role VARCHAR(100),
    ADD COLUMN IF NOT EXISTS result VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    ADD COLUMN IF NOT EXISTS before_changes JSONB,
    ADD COLUMN IF NOT EXISTS after_changes JSONB;

CREATE INDEX IF NOT EXISTS idx_audit_logs_result ON audit_logs(result);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_school ON audit_logs(created_at, school_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_school_created ON audit_logs(school_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity_type ON audit_logs(entity_type);

-- Audit log retention: records older than 90 days are purged automatically by
-- the scheduled AuditRetentionJob. These indexes support the timestamp-based
-- cleanup and the Super Admin filter queries.
