CREATE TABLE IF NOT EXISTS book_categories (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS books (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL,
    category_id BIGINT,
    isbn VARCHAR(20),
    title VARCHAR(500) NOT NULL,
    author VARCHAR(255) NOT NULL,
    publisher VARCHAR(255),
    published_date DATE,
    edition VARCHAR(100),
    language VARCHAR(50) NOT NULL DEFAULT 'ENGLISH',
    pages INT,
    shelf_location VARCHAR(100),
    total_copies INT NOT NULL DEFAULT 1,
    available_copies INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS book_copies (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    barcode VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    condition_status VARCHAR(20) NOT NULL DEFAULT 'GOOD',
    remarks TEXT,
    acquired_date DATE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS library_members (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL,
    member_type VARCHAR(20) NOT NULL,
    member_id BIGINT NOT NULL,
    membership_number VARCHAR(100) NOT NULL,
    joined_date DATE NOT NULL,
    membership_expiry DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS borrowings (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL,
    book_copy_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    borrow_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'BORROWED',
    remarks TEXT,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS fines (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL,
    borrowing_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    fine_amount NUMERIC(10,2) NOT NULL DEFAULT 0,
    paid_amount NUMERIC(10,2) NOT NULL DEFAULT 0,
    balance NUMERIC(10,2) NOT NULL DEFAULT 0,
    fine_type VARCHAR(20) NOT NULL DEFAULT 'OVERDUE',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_date DATE,
    remarks TEXT,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_book_categories_school ON book_categories(school_id);
CREATE INDEX IF NOT EXISTS idx_books_school ON books(school_id);
CREATE INDEX IF NOT EXISTS idx_books_category ON books(category_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_books_school_isbn ON books(school_id, isbn) WHERE isbn IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_book_copies_school ON book_copies(school_id);
CREATE INDEX IF NOT EXISTS idx_book_copies_book ON book_copies(book_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_book_copies_school_barcode ON book_copies(school_id, barcode);
CREATE INDEX IF NOT EXISTS idx_library_members_school ON library_members(school_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_library_members_school_number ON library_members(school_id, membership_number);
CREATE INDEX IF NOT EXISTS idx_borrowings_school ON borrowings(school_id);
CREATE INDEX IF NOT EXISTS idx_borrowings_copy ON borrowings(book_copy_id);
CREATE INDEX IF NOT EXISTS idx_borrowings_member ON borrowings(member_id);
CREATE INDEX IF NOT EXISTS idx_borrowings_due ON borrowings(due_date);
CREATE INDEX IF NOT EXISTS idx_fines_school ON fines(school_id);
CREATE INDEX IF NOT EXISTS idx_fines_borrowing ON fines(borrowing_id);
CREATE INDEX IF NOT EXISTS idx_fines_member ON fines(member_id);
