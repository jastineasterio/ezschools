CREATE TABLE IF NOT EXISTS subjects (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50),
    description TEXT,
    department_id BIGINT REFERENCES departments(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(school_id, code),
    UNIQUE(school_id, name)
);

CREATE INDEX idx_subjects_school ON subjects(school_id);
CREATE INDEX idx_subjects_department ON subjects(department_id);
CREATE INDEX idx_subjects_status ON subjects(status);
