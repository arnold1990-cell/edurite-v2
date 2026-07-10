--noinspection SqlNoDataSourceInspection
CREATE TABLE IF NOT EXISTS university_programmes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    qualification_type VARCHAR(120),
    faculty VARCHAR(180),
    department VARCHAR(180),
    duration VARCHAR(120),
    study_mode VARCHAR(80),
    campus VARCHAR(180),
    programme_url VARCHAR(1200),
    source_url VARCHAR(1200) NOT NULL,
    source_label VARCHAR(120),
    retrieval_status VARCHAR(40) NOT NULL DEFAULT 'VERIFIED',
    last_verified_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_university_programmes_identity
    ON university_programmes(institution_id, name, source_url);
CREATE INDEX IF NOT EXISTS idx_university_programmes_institution_active
    ON university_programmes(institution_id, active);
CREATE INDEX IF NOT EXISTS idx_university_programmes_faculty
    ON university_programmes(faculty);

CREATE TABLE IF NOT EXISTS university_admission_requirements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id) ON DELETE CASCADE,
    programme_id UUID REFERENCES university_programmes(id) ON DELETE SET NULL,
    programme_name VARCHAR(255),
    requirement_title VARCHAR(255),
    aps_minimum INTEGER,
    required_subjects TEXT,
    minimum_marks TEXT,
    nsc_requirement TEXT,
    language_requirement TEXT,
    faculty_specific_requirement TEXT,
    international_requirement TEXT,
    additional_tests TEXT,
    source_url VARCHAR(1200) NOT NULL,
    source_label VARCHAR(120),
    retrieval_status VARCHAR(40) NOT NULL DEFAULT 'VERIFIED',
    last_verified_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_university_admission_identity
    ON university_admission_requirements(institution_id, COALESCE(programme_name, ''), source_url, COALESCE(requirement_title, ''));
CREATE INDEX IF NOT EXISTS idx_university_admission_institution_active
    ON university_admission_requirements(institution_id, active);

CREATE TABLE IF NOT EXISTS university_retrieval_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id) ON DELETE CASCADE,
    source_url VARCHAR(1200),
    status VARCHAR(40) NOT NULL,
    message VARCHAR(1000),
    retrieval_type VARCHAR(80),
    retrieved_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_university_retrieval_logs_institution_retrieved
    ON university_retrieval_logs(institution_id, retrieved_at DESC);

