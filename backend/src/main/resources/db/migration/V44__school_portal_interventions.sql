CREATE TABLE IF NOT EXISTS school_interventions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL REFERENCES schools(id),
    learner_user_id UUID NOT NULL REFERENCES users(id),
    assigned_by_user_id UUID NOT NULL REFERENCES users(id),
    support_type VARCHAR(120) NOT NULL,
    priority VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'OPEN',
    notes TEXT NOT NULL,
    follow_up_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_school_interventions_scope
    ON school_interventions (school_id, learner_user_id, active);
