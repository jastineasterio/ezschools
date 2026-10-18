CREATE TABLE IF NOT EXISTS vehicles (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL,
    registration_number VARCHAR(100) NOT NULL,
    vehicle_type VARCHAR(50) NOT NULL DEFAULT 'BUS',
    capacity INTEGER NOT NULL,
    driver_id BIGINT,
    driver_name VARCHAR(255),
    driver_phone VARCHAR(20),
    insurance_number VARCHAR(100),
    insurance_expiry DATE,
    last_service_date DATE,
    next_service_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    remarks TEXT,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS routes (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50),
    start_point VARCHAR(255) NOT NULL,
    end_point VARCHAR(255) NOT NULL,
    distance_km NUMERIC(8,2),
    estimated_duration_minutes INTEGER,
    morning_departure TIME,
    afternoon_departure TIME,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    remarks TEXT,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS route_stops (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    route_id BIGINT NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    stop_order INTEGER NOT NULL,
    latitude NUMERIC(10,8),
    longitude NUMERIC(11,8),
    arrival_time TIME,
    departure_time TIME,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    remarks TEXT,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS transport_assignments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    school_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    stop_id BIGINT NOT NULL REFERENCES route_stops(id) ON DELETE CASCADE,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    pickup_point VARCHAR(255),
    drop_point VARCHAR(255),
    academic_year_id BIGINT,
    start_date DATE NOT NULL,
    end_date DATE,
    fee_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    monthly_fee NUMERIC(10,2),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    remarks TEXT,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_vehicles_school_registration ON vehicles(school_id, registration_number);
CREATE INDEX IF NOT EXISTS idx_vehicles_school ON vehicles(school_id);
CREATE INDEX IF NOT EXISTS idx_vehicles_driver ON vehicles(driver_id);
CREATE INDEX IF NOT EXISTS idx_vehicles_status ON vehicles(status);

CREATE INDEX IF NOT EXISTS idx_routes_school ON routes(school_id);
CREATE INDEX IF NOT EXISTS idx_routes_status ON routes(status);

CREATE INDEX IF NOT EXISTS idx_route_stops_route ON route_stops(route_id);
CREATE INDEX IF NOT EXISTS idx_route_stops_school ON route_stops(school_id);
CREATE INDEX IF NOT EXISTS idx_route_stops_order ON route_stops(route_id, stop_order);

CREATE INDEX IF NOT EXISTS idx_transport_assignments_school ON transport_assignments(school_id);
CREATE INDEX IF NOT EXISTS idx_transport_assignments_student ON transport_assignments(student_id);
CREATE INDEX IF NOT EXISTS idx_transport_assignments_route ON transport_assignments(route_id);
CREATE INDEX IF NOT EXISTS idx_transport_assignments_vehicle ON transport_assignments(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_transport_assignments_stop ON transport_assignments(stop_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_transport_assignments_student_route ON transport_assignments(school_id, student_id, route_id) WHERE deleted_at IS NULL;
