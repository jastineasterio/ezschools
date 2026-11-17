ALTER TABLE result_grade_snapshots
    ADD COLUMN IF NOT EXISTS uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid();

ALTER TABLE result_grade_snapshots
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE result_grade_snapshots
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

DROP TRIGGER IF EXISTS update_result_grade_snapshots_updated_at ON result_grade_snapshots;
CREATE TRIGGER update_result_grade_snapshots_updated_at
    BEFORE UPDATE ON result_grade_snapshots
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
