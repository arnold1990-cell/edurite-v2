INSERT INTO roles (id, name, created_at, updated_at)
SELECT gen_random_uuid(), 'ROLE_DISTRICT_DIRECTOR', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_DISTRICT_DIRECTOR');

INSERT INTO roles (id, name, created_at, updated_at)
SELECT gen_random_uuid(), 'ROLE_CIRCUIT_MANAGER', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_CIRCUIT_MANAGER');

INSERT INTO roles (id, name, created_at, updated_at)
SELECT gen_random_uuid(), 'ROLE_SUBJECT_ADVISOR', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_SUBJECT_ADVISOR');

CREATE TABLE IF NOT EXISTS circuits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL UNIQUE,
    district_id UUID NOT NULL,
    manager_user_id UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS school_circuit_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL,
    circuit_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NULL DEFAULT NOW(),
    assigned_by UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS subject_advisor_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    advisor_user_id UUID NOT NULL,
    subject VARCHAR(255) NOT NULL,
    grade VARCHAR(255) NULL,
    phase VARCHAR(255) NULL,
    district_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS school_visit_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    circuit_manager_id UUID NOT NULL,
    school_id UUID NOT NULL,
    visit_date DATE NOT NULL,
    purpose VARCHAR(255) NOT NULL,
    status VARCHAR(100) NOT NULL DEFAULT 'SCHEDULED',
    notes TEXT NULL,
    outcome TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS support_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL,
    requested_by UUID NOT NULL,
    request_type VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NULL,
    grade VARCHAR(255) NULL,
    description TEXT NOT NULL,
    status VARCHAR(100) NOT NULL DEFAULT 'OPEN',
    assigned_to UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE district_interventions ADD COLUMN IF NOT EXISTS description TEXT NULL;
ALTER TABLE district_interventions ADD COLUMN IF NOT EXISTS intervention_type VARCHAR(255) NULL;
ALTER TABLE district_interventions ADD COLUMN IF NOT EXISTS teacher_id UUID NULL;
ALTER TABLE district_interventions ADD COLUMN IF NOT EXISTS subject VARCHAR(255) NULL;
ALTER TABLE district_interventions ADD COLUMN IF NOT EXISTS grade VARCHAR(255) NULL;
ALTER TABLE district_interventions ADD COLUMN IF NOT EXISTS assigned_to UUID NULL;
ALTER TABLE district_interventions ADD COLUMN IF NOT EXISTS due_date DATE NULL;
ALTER TABLE district_interventions ADD COLUMN IF NOT EXISTS support_plan TEXT NULL;

UPDATE district_interventions
SET intervention_type = COALESCE(intervention_type, category),
    description = COALESCE(description, notes),
    due_date = COALESCE(due_date, follow_up_date),
    assigned_to = COALESCE(assigned_to, created_by_user_id)
WHERE intervention_type IS NULL
   OR description IS NULL
   OR due_date IS NULL
   OR assigned_to IS NULL;
