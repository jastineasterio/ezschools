CREATE TABLE IF NOT EXISTS school_branding (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL UNIQUE REFERENCES schools(id) ON DELETE CASCADE,
    primary_color VARCHAR(7) NOT NULL DEFAULT '#3b82f6',
    secondary_color VARCHAR(7) NOT NULL DEFAULT '#64748b',
    accent_color VARCHAR(7) NOT NULL DEFAULT '#f59e0b',
    background_color VARCHAR(7) NOT NULL DEFAULT '#ffffff',
    text_color VARCHAR(7) NOT NULL DEFAULT '#0f172a',
    logo_url TEXT,
    favicon_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
