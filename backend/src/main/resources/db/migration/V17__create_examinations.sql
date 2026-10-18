CREATE TABLE IF NOT EXISTS examination_types (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(school_id, name)
);

CREATE TABLE IF NOT EXISTS examinations (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    academic_year_id BIGINT NOT NULL REFERENCES academic_years(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    type_id BIGINT NOT NULL REFERENCES examination_types(id) ON DELETE CASCADE,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS examination_subjects (
    id BIGSERIAL PRIMARY KEY,
    examination_id BIGINT NOT NULL REFERENCES examinations(id) ON DELETE CASCADE,
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    class_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    stream_id BIGINT REFERENCES streams(id) ON DELETE SET NULL,
    maximum_marks NUMERIC(10,2) NOT NULL,
    pass_mark NUMERIC(10,2) NOT NULL,
    grading_scheme_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(examination_id, subject_id, class_id, stream_id)
);

CREATE INDEX idx_examinations_school ON examinations(school_id);
CREATE INDEX idx_examinations_year ON examinations(academic_year_id);
CREATE INDEX idx_examination_subjects_exam ON examination_subjects(examination_id);
CREATE INDEX idx_examination_subjects_subject ON examination_subjects(subject_id);
CREATE INDEX idx_examination_subjects_class ON examination_subjects(class_id);
