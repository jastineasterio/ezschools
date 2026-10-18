CREATE TABLE IF NOT EXISTS academic_years (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(school_id, name)
);

CREATE INDEX idx_academic_years_school ON academic_years(school_id);
CREATE INDEX idx_academic_years_status ON academic_years(status);
CREATE INDEX idx_academic_years_current ON academic_years(school_id, is_current) WHERE is_current = TRUE;
