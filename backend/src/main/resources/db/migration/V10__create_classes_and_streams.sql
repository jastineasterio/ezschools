CREATE TABLE IF NOT EXISTS classes (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50),
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(school_id, name)
);

CREATE TABLE IF NOT EXISTS streams (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50),
    capacity INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(class_id, name),
    UNIQUE(school_id, class_id, code)
);

CREATE INDEX idx_classes_school ON classes(school_id);
CREATE INDEX idx_streams_class ON streams(class_id);
CREATE INDEX idx_streams_school ON streams(school_id);
