CREATE TABLE IF NOT EXISTS student_certificates (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    academic_year_id BIGINT NOT NULL REFERENCES academic_years(id),
    class_id BIGINT NOT NULL REFERENCES classes(id),
    stream_id BIGINT REFERENCES streams(id),
    subject_id BIGINT REFERENCES subjects(id),
    result_id BIGINT REFERENCES results(id),
    award_type VARCHAR(40) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    certificate_number VARCHAR(80) NOT NULL UNIQUE,
    verification_code VARCHAR(80) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATED',
    version INTEGER NOT NULL DEFAULT 1,
    issued_by BIGINT REFERENCES users(id),
    issued_at TIMESTAMP,
    revoked_by BIGINT REFERENCES users(id),
    revoked_at TIMESTAMP,
    revocation_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_student_certificates_school ON student_certificates(school_id);
CREATE INDEX idx_student_certificates_student ON student_certificates(student_id);
CREATE INDEX idx_student_certificates_scope ON student_certificates(academic_year_id, class_id, stream_id, subject_id);
CREATE INDEX idx_student_certificates_status ON student_certificates(status);
CREATE UNIQUE INDEX uq_student_certificates_active_scope
    ON student_certificates(school_id, student_id, academic_year_id, class_id,
                            COALESCE(stream_id, -1), COALESCE(subject_id, -1), award_type)
    WHERE status IN ('GENERATED', 'RELEASED') AND deleted_at IS NULL;
