ALTER TABLE fee_assignments
    ALTER COLUMN assigned_at TYPE DATE
    USING assigned_at::DATE;
