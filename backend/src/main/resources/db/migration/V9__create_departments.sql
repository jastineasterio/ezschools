CREATE TABLE IF NOT EXISTS departments (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50),
    description TEXT,
    head_of_department_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(school_id, name),
    UNIQUE(school_id, code)
);

CREATE INDEX idx_departments_school ON departments(school_id);
CREATE INDEX idx_departments_status ON departments(status);
