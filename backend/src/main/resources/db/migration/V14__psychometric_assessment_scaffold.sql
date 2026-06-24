CREATE TABLE IF NOT EXISTS psychometric_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    description TEXT,
    version VARCHAR(40) NOT NULL DEFAULT 'v1.0',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    public_available BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS psychometric_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL REFERENCES psychometric_assessments(id) ON DELETE CASCADE,
    question_key VARCHAR(120) NOT NULL,
    prompt TEXT NOT NULL,
    dimension_key VARCHAR(120) NOT NULL,
    min_score INTEGER NOT NULL DEFAULT 1,
    max_score INTEGER NOT NULL DEFAULT 5,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_psychometric_question_assessment_key UNIQUE (assessment_id, question_key)
);

CREATE INDEX IF NOT EXISTS idx_psychometric_questions_assessment
    ON psychometric_questions (assessment_id, active, display_order);

CREATE TABLE IF NOT EXISTS psychometric_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL REFERENCES psychometric_assessments(id) ON DELETE CASCADE,
    student_id UUID REFERENCES students(id) ON DELETE SET NULL,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    submission_mode VARCHAR(30) NOT NULL,
    public_session_id VARCHAR(120),
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_psychometric_attempt_submission_mode CHECK (submission_mode IN ('AUTHENTICATED', 'PUBLIC'))
);

CREATE INDEX IF NOT EXISTS idx_psychometric_attempts_student_assessment
    ON psychometric_attempts (student_id, assessment_id, submitted_at DESC);

CREATE TABLE IF NOT EXISTS psychometric_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL REFERENCES psychometric_attempts(id) ON DELETE CASCADE,
    question_id UUID REFERENCES psychometric_questions(id) ON DELETE SET NULL,
    dimension_key VARCHAR(120) NOT NULL,
    score INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_psychometric_answers_attempt
    ON psychometric_answers (attempt_id);

CREATE TABLE IF NOT EXISTS psychometric_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL UNIQUE REFERENCES psychometric_attempts(id) ON DELETE CASCADE,
    student_id UUID REFERENCES students(id) ON DELETE SET NULL,
    summary_text TEXT,
    strongest_areas JSONB NOT NULL DEFAULT '[]'::jsonb,
    growth_areas JSONB NOT NULL DEFAULT '[]'::jsonb,
    scores JSONB NOT NULL DEFAULT '{}'::jsonb,
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_psychometric_results_student
    ON psychometric_results (student_id, calculated_at DESC);

INSERT INTO psychometric_assessments (code, name, description, version, active, public_available)
VALUES (
    'EDURITE_CORE_V1',
    'EduRite Core Readiness Assessment',
    'Initial psychometric scaffold for strengths, growth areas, and recommendation support.',
    'v1.0',
    TRUE,
    FALSE
)
ON CONFLICT (code) DO NOTHING;

WITH assessment AS (
    SELECT id
    FROM psychometric_assessments
    WHERE code = 'EDURITE_CORE_V1'
    LIMIT 1
)
INSERT INTO psychometric_questions (assessment_id, question_key, prompt, dimension_key, min_score, max_score, display_order, active)
SELECT assessment.id, payload.question_key, payload.prompt, payload.dimension_key, 1, 5, payload.display_order, TRUE
FROM assessment
JOIN (
    VALUES
        ('analytical_01', 'I enjoy solving complex problems step by step.', 'analytical', 1),
        ('communication_01', 'I can explain ideas clearly to different audiences.', 'communication', 2),
        ('creativity_01', 'I often suggest original approaches to tasks.', 'creativity', 3),
        ('leadership_01', 'I feel comfortable guiding group work to completion.', 'leadership', 4),
        ('technical_01', 'I am confident learning and using technical tools.', 'technical', 5)
) AS payload(question_key, prompt, dimension_key, display_order) ON TRUE
ON CONFLICT (assessment_id, question_key) DO NOTHING;
