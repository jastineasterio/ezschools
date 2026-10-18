CREATE TABLE IF NOT EXISTS attendance (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    class_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    stream_id BIGINT REFERENCES streams(id) ON DELETE SET NULL,
    academic_year_id BIGINT NOT NULL REFERENCES academic_years(id) ON DELETE CASCADE,
    subject_id BIGINT REFERENCES subjects(id) ON DELETE SET NULL,
    teacher_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    remarks TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(school_id, student_id, class_id, stream_id, attendance_date, subject_id)
);

CREATE INDEX idx_attendance_school ON attendance(school_id);
CREATE INDEX idx_attendance_student ON attendance(student_id);
CREATE INDEX idx_attendance_date ON attendance(attendance_date);
CREATE INDEX idx_attendance_class ON attendance(class_id);
CREATE INDEX idx_attendance_status ON attendance(status);
