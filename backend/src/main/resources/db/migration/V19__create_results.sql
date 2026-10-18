CREATE TABLE IF NOT EXISTS results (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    examination_subject_id BIGINT NOT NULL REFERENCES examination_subjects(id) ON DELETE CASCADE,
    examination_id BIGINT NOT NULL REFERENCES examinations(id) ON DELETE CASCADE,
    teacher_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    marks_obtained NUMERIC(10,2),
    percentage NUMERIC(10,2),
    grade VARCHAR(10),
    grade_point NUMERIC(10,2),
    remarks TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at TIMESTAMP,
    reviewed_at TIMESTAMP,
    approved_at TIMESTAMP,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(student_id, examination_subject_id)
);

CREATE INDEX idx_results_school ON results(school_id);
CREATE INDEX idx_results_student ON results(student_id);
CREATE INDEX idx_results_examination ON results(examination_id);
CREATE INDEX idx_results_status ON results(status);
CREATE INDEX idx_results_teacher ON results(teacher_id);
