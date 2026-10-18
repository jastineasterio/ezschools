CREATE TABLE IF NOT EXISTS dormitories (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    capacity INTEGER NOT NULL DEFAULT 0,
    gender VARCHAR(20),
    warden_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rooms (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    dormitory_id BIGINT NOT NULL REFERENCES dormitories(id) ON DELETE CASCADE,
    room_number VARCHAR(50) NOT NULL,
    capacity INTEGER NOT NULL DEFAULT 0,
    current_occupancy INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE(school_id, dormitory_id, room_number)
);

CREATE TABLE IF NOT EXISTS beds (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    room_id BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    bed_number VARCHAR(50) NOT NULL,
    student_id BIGINT REFERENCES student_profiles(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    allocated_date DATE,
    released_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE(school_id, room_id, bed_number)
);

CREATE TABLE IF NOT EXISTS boarding_students (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    dormitory_id BIGINT NOT NULL REFERENCES dormitories(id) ON DELETE CASCADE,
    room_id BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    bed_id BIGINT NOT NULL REFERENCES beds(id) ON DELETE CASCADE,
    allocated_date DATE NOT NULL DEFAULT CURRENT_DATE,
    released_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE(school_id, student_id, status)
);

CREATE TABLE IF NOT EXISTS boarding_attendance (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    dormitory_id BIGINT NOT NULL REFERENCES dormitories(id) ON DELETE CASCADE,
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    remarks TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE(school_id, student_id, dormitory_id, attendance_date)
);

CREATE INDEX idx_dormitories_school ON dormitories(school_id);
CREATE INDEX idx_rooms_school ON rooms(school_id);
CREATE INDEX idx_rooms_dormitory ON rooms(dormitory_id);
CREATE INDEX idx_beds_school ON beds(school_id);
CREATE INDEX idx_beds_room ON beds(room_id);
CREATE INDEX idx_beds_student ON beds(student_id);
CREATE INDEX idx_boarding_students_school ON boarding_students(school_id);
CREATE INDEX idx_boarding_students_student ON boarding_students(student_id);
CREATE INDEX idx_boarding_attendance_school ON boarding_attendance(school_id);
CREATE INDEX idx_boarding_attendance_student ON boarding_attendance(student_id);
CREATE INDEX idx_boarding_attendance_date ON boarding_attendance(attendance_date);
