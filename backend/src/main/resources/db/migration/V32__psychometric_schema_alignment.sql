CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS psychometric_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(80) NOT NULL,
    name VARCHAR(180) NOT NULL,
    description TEXT,
    version VARCHAR(40) NOT NULL DEFAULT 'v1.0',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    public_available BOOLEAN NOT NULL DEFAULT FALSE,
    student_id UUID,
    user_id UUID,
    assessment_type VARCHAR(80),
    status VARCHAR(30),
    total_score INTEGER,
    recommendation_summary TEXT,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE psychometric_assessments
    ADD COLUMN IF NOT EXISTS code VARCHAR(80),
    ADD COLUMN IF NOT EXISTS name VARCHAR(180),
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS version VARCHAR(40) NOT NULL DEFAULT 'v1.0',
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS public_available BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS student_id UUID,
    ADD COLUMN IF NOT EXISTS user_id UUID,
    ADD COLUMN IF NOT EXISTS assessment_type VARCHAR(80),
    ADD COLUMN IF NOT EXISTS status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS total_score INTEGER,
    ADD COLUMN IF NOT EXISTS recommendation_summary TEXT,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

UPDATE psychometric_assessments
SET assessment_type = COALESCE(assessment_type, code),
    status = COALESCE(status, CASE WHEN active THEN 'ACTIVE' ELSE 'INACTIVE' END)
WHERE assessment_type IS NULL OR status IS NULL;

CREATE INDEX IF NOT EXISTS idx_psychometric_assessments_code
    ON psychometric_assessments (code);

CREATE INDEX IF NOT EXISTS idx_psychometric_assessments_student_id
    ON psychometric_assessments (student_id);

CREATE INDEX IF NOT EXISTS idx_psychometric_assessments_user_id
    ON psychometric_assessments (user_id);

CREATE INDEX IF NOT EXISTS idx_psychometric_assessments_status
    ON psychometric_assessments (status);

INSERT INTO psychometric_assessments (code, name, description, version, active, public_available, assessment_type, status)
SELECT
    'EDURITE_CORE_V1',
    'EduRite Core Readiness Assessment',
    'Initial psychometric scaffold for strengths, growth areas, and recommendation support.',
    'v1.0',
    TRUE,
    FALSE,
    'EDURITE_CORE_V1',
    'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1
    FROM psychometric_assessments
    WHERE code = 'EDURITE_CORE_V1'
);

CREATE TABLE IF NOT EXISTS psychometric_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL,
    question_key VARCHAR(120) NOT NULL,
    prompt TEXT NOT NULL,
    dimension_key VARCHAR(120) NOT NULL,
    min_score INTEGER NOT NULL DEFAULT 1,
    max_score INTEGER NOT NULL DEFAULT 5,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE psychometric_questions
    ADD COLUMN IF NOT EXISTS assessment_id UUID,
    ADD COLUMN IF NOT EXISTS question_key VARCHAR(120),
    ADD COLUMN IF NOT EXISTS prompt TEXT,
    ADD COLUMN IF NOT EXISTS dimension_key VARCHAR(120),
    ADD COLUMN IF NOT EXISTS min_score INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS max_score INTEGER NOT NULL DEFAULT 5,
    ADD COLUMN IF NOT EXISTS display_order INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_psychometric_questions_assessment
    ON psychometric_questions (assessment_id, active, display_order);

CREATE INDEX IF NOT EXISTS idx_psychometric_questions_dimension_key
    ON psychometric_questions (dimension_key);

CREATE TABLE IF NOT EXISTS psychometric_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL,
    student_id UUID,
    user_id UUID,
    submission_mode VARCHAR(30) NOT NULL DEFAULT 'AUTHENTICATED',
    public_session_id VARCHAR(120),
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE psychometric_attempts
    ADD COLUMN IF NOT EXISTS assessment_id UUID,
    ADD COLUMN IF NOT EXISTS student_id UUID,
    ADD COLUMN IF NOT EXISTS user_id UUID,
    ADD COLUMN IF NOT EXISTS submission_mode VARCHAR(30) NOT NULL DEFAULT 'AUTHENTICATED',
    ADD COLUMN IF NOT EXISTS public_session_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_psychometric_attempts_assessment
    ON psychometric_attempts (assessment_id);

CREATE INDEX IF NOT EXISTS idx_psychometric_attempts_student_assessment
    ON psychometric_attempts (student_id, assessment_id, submitted_at DESC);

CREATE INDEX IF NOT EXISTS idx_psychometric_attempts_user_id
    ON psychometric_attempts (user_id);

CREATE INDEX IF NOT EXISTS idx_psychometric_attempts_public_session_id
    ON psychometric_attempts (public_session_id);

CREATE TABLE IF NOT EXISTS psychometric_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL,
    question_id UUID,
    dimension_key VARCHAR(120) NOT NULL,
    score INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE psychometric_answers
    ADD COLUMN IF NOT EXISTS attempt_id UUID,
    ADD COLUMN IF NOT EXISTS question_id UUID,
    ADD COLUMN IF NOT EXISTS dimension_key VARCHAR(120),
    ADD COLUMN IF NOT EXISTS score INTEGER,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_psychometric_answers_attempt
    ON psychometric_answers (attempt_id);

CREATE INDEX IF NOT EXISTS idx_psychometric_answers_question_id
    ON psychometric_answers (question_id);

CREATE INDEX IF NOT EXISTS idx_psychometric_answers_dimension_key
    ON psychometric_answers (dimension_key);

CREATE TABLE IF NOT EXISTS psychometric_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL,
    student_id UUID,
    summary_text TEXT,
    strongest_areas JSONB NOT NULL DEFAULT '[]'::jsonb,
    growth_areas JSONB NOT NULL DEFAULT '[]'::jsonb,
    scores JSONB NOT NULL DEFAULT '{}'::jsonb,
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE psychometric_results
    ADD COLUMN IF NOT EXISTS attempt_id UUID,
    ADD COLUMN IF NOT EXISTS student_id UUID,
    ADD COLUMN IF NOT EXISTS summary_text TEXT,
    ADD COLUMN IF NOT EXISTS strongest_areas JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS growth_areas JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS scores JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS calculated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_psychometric_results_attempt_id
    ON psychometric_results (attempt_id);

CREATE INDEX IF NOT EXISTS idx_psychometric_results_student
    ON psychometric_results (student_id, calculated_at DESC);

CREATE TABLE IF NOT EXISTS psychometric_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID,
    user_id UUID,
    submission_mode VARCHAR(30) NOT NULL DEFAULT 'AUTHENTICATED',
    public_session_id VARCHAR(120),
    answers JSONB NOT NULL DEFAULT '[]'::jsonb,
    scores JSONB NOT NULL DEFAULT '{}'::jsonb,
    interpretation TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE psychometric_submissions
    ADD COLUMN IF NOT EXISTS student_id UUID,
    ADD COLUMN IF NOT EXISTS user_id UUID,
    ADD COLUMN IF NOT EXISTS submission_mode VARCHAR(30) NOT NULL DEFAULT 'AUTHENTICATED',
    ADD COLUMN IF NOT EXISTS public_session_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS answers JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS scores JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS interpretation TEXT,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_psychometric_submissions_student_id
    ON psychometric_submissions (student_id);

CREATE INDEX IF NOT EXISTS idx_psychometric_submissions_user_id
    ON psychometric_submissions (user_id);

CREATE INDEX IF NOT EXISTS idx_psychometric_submissions_public_session_id
    ON psychometric_submissions (public_session_id);
