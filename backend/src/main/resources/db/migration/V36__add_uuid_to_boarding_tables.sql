ALTER TABLE dormitories
    ADD COLUMN IF NOT EXISTS uuid UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE rooms
    ADD COLUMN IF NOT EXISTS uuid UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE beds
    ADD COLUMN IF NOT EXISTS uuid UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE boarding_students
    ADD COLUMN IF NOT EXISTS uuid UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE boarding_attendance
    ADD COLUMN IF NOT EXISTS uuid UUID NOT NULL DEFAULT gen_random_uuid();

CREATE UNIQUE INDEX IF NOT EXISTS idx_dormitories_uuid
    ON dormitories(uuid);

CREATE UNIQUE INDEX IF NOT EXISTS idx_rooms_uuid
    ON rooms(uuid);

CREATE UNIQUE INDEX IF NOT EXISTS idx_beds_uuid
    ON beds(uuid);

CREATE UNIQUE INDEX IF NOT EXISTS idx_boarding_students_uuid
    ON boarding_students(uuid);

CREATE UNIQUE INDEX IF NOT EXISTS idx_boarding_attendance_uuid
    ON boarding_attendance(uuid);
