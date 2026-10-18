ALTER TABLE analytics_snapshots ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
