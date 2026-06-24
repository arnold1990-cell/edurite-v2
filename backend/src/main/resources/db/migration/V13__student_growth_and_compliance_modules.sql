ALTER TABLE users
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(255),
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;

ALTER TABLE students
    ADD COLUMN IF NOT EXISTS preferences_json JSONB NOT NULL DEFAULT '{}'::jsonb;

CREATE TABLE IF NOT EXISTS consent_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    consent_type VARCHAR(80) NOT NULL,
    consent_version VARCHAR(40) NOT NULL,
    accepted_at TIMESTAMPTZ NOT NULL,
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_consent_records_user_id
    ON consent_records (user_id);

CREATE INDEX IF NOT EXISTS idx_consent_records_user_type
    ON consent_records (user_id, consent_type);

CREATE TABLE IF NOT EXISTS student_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID UNIQUE NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    preferred_industries TEXT,
    preferred_locations TEXT,
    notification_preferences JSONB NOT NULL DEFAULT '{}'::jsonb,
    extra JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS psychometric_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID REFERENCES students(id) ON DELETE SET NULL,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    submission_mode VARCHAR(30) NOT NULL,
    public_session_id VARCHAR(120),
    answers JSONB NOT NULL DEFAULT '[]'::jsonb,
    scores JSONB NOT NULL DEFAULT '{}'::jsonb,
    interpretation TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_psychometric_submission_mode CHECK (submission_mode IN ('AUTHENTICATED', 'PUBLIC'))
);

CREATE INDEX IF NOT EXISTS idx_psychometric_submissions_student_id
    ON psychometric_submissions (student_id);

CREATE INDEX IF NOT EXISTS idx_psychometric_submissions_public_session_id
    ON psychometric_submissions (public_session_id);

CREATE TABLE IF NOT EXISTS learning_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(180) NOT NULL UNIQUE,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS learning_resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID REFERENCES learning_categories(id) ON DELETE SET NULL,
    title VARCHAR(220) NOT NULL,
    summary TEXT,
    url VARCHAR(500) NOT NULL,
    difficulty VARCHAR(30),
    estimated_minutes INTEGER,
    tags TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_learning_resources_category_id
    ON learning_resources (category_id);

CREATE TABLE IF NOT EXISTS learning_outcome_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outcome_key VARCHAR(120) NOT NULL,
    outcome_label VARCHAR(220),
    resource_id UUID NOT NULL REFERENCES learning_resources(id) ON DELETE CASCADE,
    priority INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_learning_outcome_mappings_outcome_key
    ON learning_outcome_mappings (outcome_key);

CREATE TABLE IF NOT EXISTS student_terms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    term_code VARCHAR(30) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_student_terms UNIQUE (student_id, term_code),
    CONSTRAINT chk_student_term_status CHECK (status IN ('ACTIVE', 'CLOSED'))
);

CREATE TABLE IF NOT EXISTS student_points_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    event_type VARCHAR(80) NOT NULL,
    points INTEGER NOT NULL,
    reference_id VARCHAR(120),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    awarded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    term_code VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_student_points_ledger_student_id
    ON student_points_ledger (student_id);

CREATE INDEX IF NOT EXISTS idx_student_points_ledger_term_code
    ON student_points_ledger (term_code);

CREATE TABLE IF NOT EXISTS reward_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    event_type VARCHAR(80) NOT NULL,
    points_per_event INTEGER NOT NULL,
    max_per_term INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS reward_claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    term_code VARCHAR(30) NOT NULL,
    reward_name VARCHAR(180) NOT NULL,
    reward_description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    claimed_points INTEGER NOT NULL,
    claimed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_reward_claim_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'FULFILLED'))
);

CREATE INDEX IF NOT EXISTS idx_reward_claims_student_term
    ON reward_claims (student_id, term_code);

CREATE TABLE IF NOT EXISTS pricing_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    currency VARCHAR(8) NOT NULL DEFAULT 'ZAR',
    amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    billing_interval VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    features JSONB NOT NULL DEFAULT '[]'::jsonb,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    premium BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO pricing_plans (code, name, description, currency, amount, billing_interval, features, active, premium, display_order)
VALUES
    ('PLAN_BASIC', 'Basic', 'Essential profile and recommendation tools.', 'ZAR', 0.00, 'MONTHLY',
     '["Profile management","Career and bursary discovery","Basic AI suggestions"]'::jsonb, TRUE, FALSE, 1),
    ('PLAN_PREMIUM', 'Premium', 'Advanced guidance and deeper psychometric insights.', 'ZAR', 99.00, 'MONTHLY',
     '["Everything in Basic","Advanced AI guidance","Priority opportunities","Premium learning centre tracks"]'::jsonb, TRUE, TRUE, 2)
ON CONFLICT (code) DO NOTHING;

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS provider VARCHAR(80),
    ADD COLUMN IF NOT EXISTS reference VARCHAR(160),
    ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS metadata JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE subscriptions
    DROP CONSTRAINT IF EXISTS chk_subscription_plan_code;

ALTER TABLE subscriptions
    DROP CONSTRAINT IF EXISTS chk_subscription_status;

ALTER TABLE subscriptions
    ADD CONSTRAINT chk_subscription_status
    CHECK (status IN ('ACTIVE', 'PENDING', 'CANCELLED', 'EXPIRED', 'PAST_DUE', 'PAYMENT_FAILED'));

CREATE TABLE IF NOT EXISTS user_subscription_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subscription_id UUID REFERENCES subscriptions(id) ON DELETE SET NULL,
    payment_id UUID REFERENCES payments(id) ON DELETE SET NULL,
    event_type VARCHAR(80) NOT NULL,
    event_status VARCHAR(80) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_user_subscription_events_user_id
    ON user_subscription_events (user_id);

CREATE TABLE IF NOT EXISTS account_deletion_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    reason VARCHAR(255),
    deleted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO reward_rules (code, name, description, event_type, points_per_event, max_per_term, active)
VALUES
    ('LOGIN_DAILY', 'Daily login', 'Awarded for the first login of the day.', 'LOGIN_DAILY', 5, 300, TRUE),
    ('TASK_COMPLETED', 'Task completion', 'Awarded when a learning or profile task is completed.', 'TASK_COMPLETED', 15, 1200, TRUE)
ON CONFLICT (code) DO NOTHING;
