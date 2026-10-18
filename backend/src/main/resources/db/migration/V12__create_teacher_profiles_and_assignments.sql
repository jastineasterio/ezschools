CREATE TABLE IF NOT EXISTS teacher_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    employee_number VARCHAR(100),
    gender VARCHAR(20),
    date_of_birth DATE,
    employment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    qualification TEXT,
    experience_years INTEGER,
    photo_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS teacher_assignments (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    class_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    stream_id BIGINT REFERENCES streams(id) ON DELETE CASCADE,
    academic_year_id BIGINT NOT NULL REFERENCES academic_years(id) ON DELETE CASCADE,
    is_class_teacher BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(teacher_id, subject_id, class_id, stream_id, academic_year_id)
);

CREATE INDEX idx_teacher_profiles_school ON teacher_profiles(school_id);
CREATE INDEX idx_teacher_profiles_user ON teacher_profiles(user_id);
CREATE INDEX idx_teacher_assignments_teacher ON teacher_assignments(teacher_id);
CREATE INDEX idx_teacher_assignments_school ON teacher_assignments(school_id);
CREATE INDEX idx_teacher_assignments_subject ON teacher_assignments(subject_id);
CREATE INDEX idx_teacher_assignments_class ON teacher_assignments(class_id);
CREATE INDEX idx_teacher_assignments_year ON teacher_assignments(academic_year_id);
