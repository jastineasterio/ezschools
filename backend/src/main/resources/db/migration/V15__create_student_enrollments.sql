CREATE TABLE IF NOT EXISTS student_enrollments (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    academic_year_id BIGINT NOT NULL REFERENCES academic_years(id) ON DELETE CASCADE,
    class_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    stream_id BIGINT REFERENCES streams(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(student_id, academic_year_id)
);

CREATE INDEX idx_enrollments_school ON student_enrollments(school_id);
CREATE INDEX idx_enrollments_student ON student_enrollments(student_id);
CREATE INDEX idx_enrollments_year ON student_enrollments(academic_year_id);
CREATE INDEX idx_enrollments_class ON student_enrollments(class_id);
CREATE INDEX idx_enrollments_status ON student_enrollments(status);
