CREATE TABLE IF NOT EXISTS fee_structures (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    amount NUMERIC(12,2) NOT NULL,
    due_date DATE,
    academic_year_id BIGINT,
    class_id BIGINT,
    fee_type VARCHAR(50) NOT NULL DEFAULT 'TUITION',
    is_mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS fee_assignments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    fee_structure_id BIGINT NOT NULL,
    amount_due NUMERIC(12,2) NOT NULL,
    amount_paid NUMERIC(12,2) NOT NULL DEFAULT 0,
    balance NUMERIC(12,2) NOT NULL,
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    fee_assignment_id BIGINT,
    amount NUMERIC(12,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL DEFAULT 'CASH',
    payment_date DATE NOT NULL,
    reference_number VARCHAR(255),
    receipt_number VARCHAR(255),
    remarks TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS financial_categories (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    category_type VARCHAR(20) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS financial_transactions (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL,
    category_id BIGINT,
    amount NUMERIC(12,2) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    transaction_date DATE NOT NULL,
    reference_number VARCHAR(255),
    description TEXT,
    payment_id BIGINT,
    fee_assignment_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_fee_structures_school ON fee_structures(school_id);
CREATE INDEX IF NOT EXISTS idx_fee_structures_class ON fee_structures(class_id);
CREATE INDEX IF NOT EXISTS idx_fee_assignments_school ON fee_assignments(school_id);
CREATE INDEX IF NOT EXISTS idx_fee_assignments_student ON fee_assignments(student_id);
CREATE INDEX IF NOT EXISTS idx_fee_assignments_fee_structure ON fee_assignments(fee_structure_id);
CREATE INDEX IF NOT EXISTS idx_payments_school ON payments(school_id);
CREATE INDEX IF NOT EXISTS idx_payments_student ON payments(student_id);
CREATE INDEX IF NOT EXISTS idx_payments_fee_assignment ON payments(fee_assignment_id);
CREATE INDEX IF NOT EXISTS idx_financial_categories_school ON financial_categories(school_id);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_school ON financial_transactions(school_id);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_category ON financial_transactions(category_id);
