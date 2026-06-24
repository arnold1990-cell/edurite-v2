CREATE TABLE IF NOT EXISTS saved_career_roadmaps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    learner_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    career_name VARCHAR(180) NOT NULL,
    roadmap_json JSONB NOT NULL,
    learner_aps INTEGER NOT NULL DEFAULT 0,
    required_aps INTEGER,
    aps_gap INTEGER,
    readiness_score INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_saved_career_roadmaps_learner_created
    ON saved_career_roadmaps(learner_id, created_at DESC);

CREATE TABLE IF NOT EXISTS career_program_requirements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    career_name VARCHAR(180) NOT NULL,
    institution_name VARCHAR(255) NOT NULL,
    institution_type VARCHAR(120),
    province VARCHAR(120),
    qualification_name VARCHAR(255) NOT NULL,
    faculty VARCHAR(180),
    aps_required INTEGER,
    mathematics_requirement VARCHAR(120),
    mathematical_literacy_requirement VARCHAR(120),
    english_requirement VARCHAR(120),
    accounting_requirement VARCHAR(120),
    physical_sciences_requirement VARCHAR(120),
    life_sciences_requirement VARCHAR(120),
    duration VARCHAR(120),
    nqf_level VARCHAR(80),
    application_url VARCHAR(500),
    notes TEXT,
    source VARCHAR(255),
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_career_program_requirements_career
    ON career_program_requirements(career_name, verified, aps_required);
