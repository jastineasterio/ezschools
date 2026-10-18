CREATE TABLE IF NOT EXISTS result_approvals (
    id BIGSERIAL PRIMARY KEY,
    result_id BIGINT NOT NULL REFERENCES results(id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    action VARCHAR(20) NOT NULL,
    actor_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_role VARCHAR(50) NOT NULL,
    comment TEXT,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_result_approvals_result ON result_approvals(result_id);
CREATE INDEX idx_result_approvals_school ON result_approvals(school_id);
CREATE INDEX idx_result_approvals_actor ON result_approvals(actor_user_id);
