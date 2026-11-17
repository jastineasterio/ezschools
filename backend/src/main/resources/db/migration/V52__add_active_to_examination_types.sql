ALTER TABLE examination_types ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
CREATE INDEX IF NOT EXISTS idx_examination_types_school_active ON examination_types(school_id, active);
UPDATE examination_types SET active = TRUE WHERE active IS NULL;
