CREATE TABLE IF NOT EXISTS result_grade_snapshots (
    id BIGSERIAL PRIMARY KEY,
    result_id BIGINT NOT NULL REFERENCES results(id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    grading_scheme_id BIGINT,
    grading_scheme_name VARCHAR(255),
    min_mark NUMERIC(10,2),
    max_mark NUMERIC(10,2),
    grade_ranges JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_result_grade_snapshots_result ON result_grade_snapshots(result_id);
CREATE INDEX IF NOT EXISTS idx_result_grade_snapshots_school ON result_grade_snapshots(school_id);

ALTER TABLE results
    ADD COLUMN IF NOT EXISTS grading_scheme_id BIGINT,
    ADD COLUMN IF NOT EXISTS grading_scheme_name VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_results_grading_scheme ON results(grading_scheme_id);
