CREATE TABLE IF NOT EXISTS parents (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(30) NOT NULL,
    gender VARCHAR(20),
    occupation VARCHAR(100),
    address TEXT,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS student_parents (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    relationship_type VARCHAR(50) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    receives_sms BOOLEAN NOT NULL DEFAULT TRUE,
    receives_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(student_id, parent_id, relationship_type)
);

CREATE INDEX idx_parents_school ON parents(school_id);
CREATE INDEX idx_student_parents_student ON student_parents(student_id);
CREATE INDEX idx_student_parents_parent ON student_parents(parent_id);
