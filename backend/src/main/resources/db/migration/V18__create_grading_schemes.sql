CREATE TABLE IF NOT EXISTS grading_schemes (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS grading_scheme_ranges (
    id BIGSERIAL PRIMARY KEY,
    scheme_id BIGINT NOT NULL REFERENCES grading_schemes(id) ON DELETE CASCADE,
    grade VARCHAR(10) NOT NULL,
    min_mark NUMERIC(10,2) NOT NULL,
    max_mark NUMERIC(10,2) NOT NULL,
    grade_point NUMERIC(10,2),
    remark VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_grading_schemes_school ON grading_schemes(school_id);
CREATE INDEX idx_grading_ranges_scheme ON grading_scheme_ranges(scheme_id);
