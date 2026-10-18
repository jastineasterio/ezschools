CREATE TABLE IF NOT EXISTS student_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    admission_number VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    gender VARCHAR(20),
    date_of_birth DATE,
    registration_date DATE NOT NULL DEFAULT CURRENT_DATE,
    phone VARCHAR(30),
    email VARCHAR(255),
    address TEXT,
    photo_url VARCHAR(500),
    student_type VARCHAR(50) NOT NULL DEFAULT 'DAY',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    emergency_contact_name VARCHAR(255),
    emergency_contact_phone VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(school_id, admission_number)
);

CREATE INDEX idx_student_profiles_school ON student_profiles(school_id);
CREATE INDEX idx_student_profiles_admission ON student_profiles(school_id, admission_number);
CREATE INDEX idx_student_profiles_user ON student_profiles(user_id);
