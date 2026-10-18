CREATE TABLE IF NOT EXISTS packages (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(150) NOT NULL,
    description TEXT,
    price NUMERIC(10,2) NOT NULL DEFAULT 0,
    billing_period VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS package_roles (
    id BIGSERIAL PRIMARY KEY,
    package_id BIGINT NOT NULL REFERENCES packages(id) ON DELETE CASCADE,
    role_name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(package_id, role_name)
);

CREATE TABLE IF NOT EXISTS package_features (
    id BIGSERIAL PRIMARY KEY,
    package_id BIGINT NOT NULL REFERENCES packages(id) ON DELETE CASCADE,
    feature_key VARCHAR(100) NOT NULL,
    feature_name VARCHAR(150) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(package_id, feature_key)
);

CREATE TABLE IF NOT EXISTS package_limits (
    id BIGSERIAL PRIMARY KEY,
    package_id BIGINT NOT NULL REFERENCES packages(id) ON DELETE CASCADE,
    limit_key VARCHAR(100) NOT NULL,
    limit_name VARCHAR(150) NOT NULL,
    max_value BIGINT NOT NULL DEFAULT 0,
    current_value BIGINT NOT NULL DEFAULT 0,
    unit VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(package_id, limit_key)
);

CREATE INDEX idx_packages_active ON packages(is_active);
CREATE INDEX idx_package_limits_package ON package_limits(package_id);
