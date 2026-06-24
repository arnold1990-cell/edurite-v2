ALTER TABLE schools
    ADD COLUMN IF NOT EXISTS school_code VARCHAR(32),
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE TABLE IF NOT EXISTS student_school_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES users(id),
    school_id UUID NOT NULL REFERENCES schools(id),
    school_code VARCHAR(32),
    status VARCHAR(20) NOT NULL,
    generated_username VARCHAR(120),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_at TIMESTAMPTZ,
    approved_by UUID REFERENCES users(id),
    rejected_at TIMESTAMPTZ,
    rejected_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_student_school_link_student UNIQUE (student_id),
    CONSTRAINT uk_student_school_link_username UNIQUE (generated_username)
);

CREATE INDEX IF NOT EXISTS idx_schools_status_name ON schools (status, school_name);
CREATE INDEX IF NOT EXISTS idx_student_school_links_school_status ON student_school_links (school_id, status);
