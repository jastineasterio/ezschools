ALTER TABLE schools
    ADD COLUMN IF NOT EXISTS time_zone VARCHAR(64) NOT NULL DEFAULT 'UTC';

CREATE TABLE teacher_timetables (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    teacher_assignment_id BIGINT NOT NULL REFERENCES teacher_assignments(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_teacher_timetable_time CHECK (end_time > start_time)
);

CREATE INDEX idx_teacher_timetables_assignment_day
    ON teacher_timetables(teacher_assignment_id, day_of_week)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_teacher_timetables_school
    ON teacher_timetables(school_id)
    WHERE deleted_at IS NULL;
