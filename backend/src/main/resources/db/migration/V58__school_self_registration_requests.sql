CREATE TABLE IF NOT EXISTS school_registration_requests (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    district_id UUID NOT NULL REFERENCES districts(id),
    school_id UUID NULL REFERENCES schools(id),
    school_name VARCHAR(255) NOT NULL,
    emis_number VARCHAR(120) NOT NULL UNIQUE,
    province VARCHAR(255) NOT NULL,
    district_name VARCHAR(255) NOT NULL,
    circuit VARCHAR(255),
    school_type VARCHAR(255),
    principal_name VARCHAR(255) NOT NULL,
    principal_email VARCHAR(255) NOT NULL,
    school_email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(60) NOT NULL,
    physical_address TEXT NOT NULL,
    status VARCHAR(80) NOT NULL,
    rejection_reason TEXT,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    approved_at TIMESTAMP WITH TIME ZONE,
    rejected_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_school_registration_requests_district_status
    ON school_registration_requests (district_id, status, submitted_at DESC);
