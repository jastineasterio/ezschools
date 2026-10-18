CREATE TABLE IF NOT EXISTS examination_reports (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    examination_id BIGINT NOT NULL REFERENCES examinations(id) ON DELETE CASCADE,
    file_key VARCHAR(500) NOT NULL,
    filename VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    generated_by BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_examination_reports_school ON examination_reports(school_id);
CREATE INDEX idx_examination_reports_student ON examination_reports(student_id);
CREATE INDEX idx_examination_reports_examination ON examination_reports(examination_id);
