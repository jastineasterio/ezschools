CREATE TABLE IF NOT EXISTS report_jobs (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT,
    requested_by BIGINT NOT NULL,
    report_type VARCHAR(100) NOT NULL,
    report_name VARCHAR(255) NOT NULL,
    parameters JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    file_url VARCHAR(500),
    file_name VARCHAR(500),
    file_size BIGINT,
    error_message TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    expires_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_report_jobs_school ON report_jobs(school_id);
CREATE INDEX IF NOT EXISTS idx_report_jobs_user ON report_jobs(requested_by);
CREATE INDEX IF NOT EXISTS idx_report_jobs_status ON report_jobs(status);
