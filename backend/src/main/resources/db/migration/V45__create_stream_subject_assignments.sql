CREATE TABLE stream_subject_assignments (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    class_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    stream_id BIGINT NOT NULL REFERENCES streams(id) ON DELETE CASCADE,
    academic_year_id BIGINT NOT NULL REFERENCES academic_years(id) ON DELETE CASCADE,
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    UNIQUE (stream_id, academic_year_id, subject_id)
);

CREATE INDEX idx_stream_subject_assignments_school
    ON stream_subject_assignments(school_id);
CREATE INDEX idx_stream_subject_assignments_class_stream_year
    ON stream_subject_assignments(class_id, stream_id, academic_year_id);
CREATE INDEX idx_stream_subject_assignments_subject
    ON stream_subject_assignments(subject_id);

ALTER TABLE teacher_assignments
    DROP CONSTRAINT IF EXISTS teacher_assignments_teacher_id_subject_id_class_id_stream_id_academic_year_id_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_active_teacher_assignment_context
    ON teacher_assignments (
        teacher_id,
        subject_id,
        class_id,
        COALESCE(stream_id, 0),
        academic_year_id
    )
    WHERE deleted_at IS NULL;
