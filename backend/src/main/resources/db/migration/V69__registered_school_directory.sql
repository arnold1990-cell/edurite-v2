CREATE TABLE IF NOT EXISTS registered_schools (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_name VARCHAR(255) NOT NULL,
    emis_number VARCHAR(120) NOT NULL UNIQUE,
    school_code VARCHAR(120),
    province_id UUID REFERENCES provinces(id),
    district_id UUID REFERENCES districts(id),
    province VARCHAR(120) NOT NULL,
    district VARCHAR(255) NOT NULL,
    circuit VARCHAR(255),
    school_type VARCHAR(120),
    physical_address VARCHAR(2000),
    principal_name VARCHAR(255),
    school_email VARCHAR(255),
    contact_number VARCHAR(60),
    source VARCHAR(120) NOT NULL DEFAULT 'DIRECTORY',
    registered_status VARCHAR(60) NOT NULL DEFAULT 'REGISTERED',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

ALTER TABLE school_registration_requests
    ADD COLUMN IF NOT EXISTS registered_school_id UUID REFERENCES registered_schools(id),
    ADD COLUMN IF NOT EXISTS entry_source VARCHAR(60) NOT NULL DEFAULT 'MANUAL';

CREATE INDEX IF NOT EXISTS idx_registered_schools_district_active_name
    ON registered_schools (district_id, active, school_name);

CREATE INDEX IF NOT EXISTS idx_registered_schools_emis_ci
    ON registered_schools (LOWER(emis_number));

CREATE INDEX IF NOT EXISTS idx_registered_schools_province_active_name
    ON registered_schools (province_id, active, school_name);

CREATE INDEX IF NOT EXISTS idx_registered_schools_search_ci
    ON registered_schools (LOWER(school_name), LOWER(emis_number), LOWER(school_code));

INSERT INTO registered_schools (
    school_name,
    emis_number,
    school_code,
    province_id,
    district_id,
    province,
    district,
    circuit,
    school_type,
    physical_address,
    principal_name,
    school_email,
    contact_number,
    source,
    registered_status,
    active
)
SELECT
    seed.school_name,
    seed.emis_number,
    seed.school_code,
    p.id,
    d.id,
    p.name,
    d.district_name,
    seed.circuit,
    seed.school_type,
    seed.physical_address,
    seed.principal_name,
    seed.school_email,
    seed.contact_number,
    'DBE_SAMPLE',
    'REGISTERED',
    TRUE
FROM (
    VALUES
        ('Tsholomnqa SS', '200200857', 'TSHSS', 'Eastern Cape', 'Buffalo City', NULL, 'Secondary School', 'Mpongo Location, Kidds Beach', NULL, 'tsholomnqahighschool@gmail.com', NULL),
        ('EduRite Secondary School', '99999999', 'EDU-SS', 'Gauteng', 'City of Johannesburg', NULL, 'Secondary School', '1 EduRite Road, Johannesburg', 'School Admin', 'school@edurite.com', '+27821234567')
) AS seed(
    school_name,
    emis_number,
    school_code,
    province,
    district,
    circuit,
    school_type,
    physical_address,
    principal_name,
    school_email,
    contact_number
)
JOIN provinces p ON LOWER(p.name) = LOWER(seed.province)
JOIN districts d ON d.province_id = p.id AND LOWER(d.district_name) = LOWER(seed.district)
ON CONFLICT DO NOTHING;
