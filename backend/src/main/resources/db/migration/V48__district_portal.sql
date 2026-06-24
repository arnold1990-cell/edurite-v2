INSERT INTO roles (id, name)
SELECT gen_random_uuid(), 'ROLE_DISTRICT_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_DISTRICT_ADMIN');

CREATE TABLE IF NOT EXISTS districts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    district_name VARCHAR(255) NOT NULL,
    district_code VARCHAR(120),
    province VARCHAR(120),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(40),
    licensing_status VARCHAR(80) NOT NULL DEFAULT 'ACTIVE',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_district_name UNIQUE (district_name)
);

ALTER TABLE schools
ADD COLUMN IF NOT EXISTS district_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_schools_district'
          AND table_name = 'schools'
    ) THEN
        ALTER TABLE schools
        ADD CONSTRAINT fk_schools_district
        FOREIGN KEY (district_id) REFERENCES districts(id);
    END IF;
END $$;

INSERT INTO districts (id, district_name, district_code, province, contact_email, licensing_status, active, created_at, updated_at)
SELECT
    gen_random_uuid(),
    s.district,
    UPPER(REPLACE(REGEXP_REPLACE(COALESCE(s.district, 'DISTRICT'), '[^A-Za-z0-9]+', '_', 'g'), '__', '_')),
    MAX(s.province),
    MIN(s.contact_email),
    'ACTIVE',
    TRUE,
    now(),
    now()
FROM schools s
WHERE COALESCE(NULLIF(TRIM(s.district), ''), '') <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM districts d
      WHERE LOWER(d.district_name) = LOWER(s.district)
  )
GROUP BY s.district;

UPDATE schools s
SET district_id = d.id
FROM districts d
WHERE s.district_id IS NULL
  AND COALESCE(NULLIF(TRIM(s.district), ''), '') <> ''
  AND LOWER(d.district_name) = LOWER(s.district);

CREATE TABLE IF NOT EXISTS district_admin_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    district_id UUID NOT NULL REFERENCES districts(id),
    user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(120),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_district_admin_profile UNIQUE (district_id, user_id)
);

CREATE TABLE IF NOT EXISTS district_interventions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    district_id UUID NOT NULL REFERENCES districts(id),
    school_id UUID REFERENCES schools(id),
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    category VARCHAR(120) NOT NULL,
    priority VARCHAR(80) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(80) NOT NULL DEFAULT 'OPEN',
    notes TEXT,
    target_scope VARCHAR(80) NOT NULL DEFAULT 'DISTRICT',
    follow_up_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS district_announcements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    district_id UUID NOT NULL REFERENCES districts(id),
    school_id UUID REFERENCES schools(id),
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    audience VARCHAR(120) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    delivery_scope VARCHAR(120) NOT NULL DEFAULT 'ALL_SCHOOLS',
    status VARCHAR(80) NOT NULL DEFAULT 'SENT',
    sent_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_schools_district_id ON schools (district_id);
CREATE INDEX IF NOT EXISTS idx_district_admin_profiles_user_id ON district_admin_profiles (user_id);
CREATE INDEX IF NOT EXISTS idx_district_admin_profiles_district_id ON district_admin_profiles (district_id);
CREATE INDEX IF NOT EXISTS idx_district_interventions_district_id ON district_interventions (district_id);
CREATE INDEX IF NOT EXISTS idx_district_announcements_district_id ON district_announcements (district_id);
