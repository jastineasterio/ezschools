CREATE TABLE IF NOT EXISTS schools (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    registration_number VARCHAR(100) UNIQUE,
    email VARCHAR(255),
    phone VARCHAR(30),
    address TEXT,
    region VARCHAR(100),
    district VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_suspended BOOLEAN NOT NULL DEFAULT FALSE,
    suspended_at TIMESTAMP,
    suspended_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS school_users (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(school_id, user_id)
);

CREATE INDEX idx_schools_uuid ON schools(uuid);
CREATE INDEX idx_schools_active ON schools(is_active);
CREATE INDEX idx_schools_suspended ON schools(is_suspended);
CREATE INDEX idx_school_users_school ON school_users(school_id);
CREATE INDEX idx_school_users_user ON school_users(user_id);
