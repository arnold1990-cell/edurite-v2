CREATE TABLE IF NOT EXISTS curriculum_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    district_id UUID REFERENCES districts(id),
    school_id UUID REFERENCES schools(id),
    owner_scope VARCHAR(40) NOT NULL,
    repository_type VARCHAR(80) NOT NULL,
    content_source VARCHAR(40) NOT NULL,
    title VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    grade VARCHAR(80) NOT NULL,
    curriculum_phase VARCHAR(120),
    academic_year INTEGER,
    province VARCHAR(120),
    version_number VARCHAR(80),
    description TEXT,
    term VARCHAR(40),
    week_number INTEGER,
    uploaded_by_user_id UUID REFERENCES users(id),
    upload_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    pdf_file_name VARCHAR(255),
    pdf_content_type VARCHAR(120),
    pdf_base64 TEXT,
    docx_file_name VARCHAR(255),
    docx_content_type VARCHAR(120),
    docx_base64 TEXT,
    excel_file_name VARCHAR(255),
    excel_content_type VARCHAR(120),
    excel_base64 TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS curriculum_week_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    curriculum_asset_id UUID NOT NULL REFERENCES curriculum_assets(id) ON DELETE CASCADE,
    district_id UUID REFERENCES districts(id),
    school_id UUID REFERENCES schools(id),
    subject VARCHAR(255) NOT NULL,
    grade VARCHAR(80) NOT NULL,
    curriculum_phase VARCHAR(120),
    academic_year INTEGER,
    province VARCHAR(120),
    term VARCHAR(40) NOT NULL,
    week_number INTEGER NOT NULL,
    topic VARCHAR(255) NOT NULL,
    subtopic VARCHAR(255),
    learning_outcomes TEXT,
    assessment_activities TEXT,
    expected_completion_label VARCHAR(120),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS teacher_curriculum_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    week_plan_id UUID NOT NULL REFERENCES curriculum_week_plans(id) ON DELETE CASCADE,
    teacher_user_id UUID NOT NULL REFERENCES users(id),
    school_id UUID NOT NULL REFERENCES schools(id),
    subject_id UUID REFERENCES school_subjects(id),
    status VARCHAR(40) NOT NULL DEFAULT 'NOT_STARTED',
    completion_percent INTEGER NOT NULL DEFAULT 0,
    notes TEXT,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_teacher_curriculum_progress UNIQUE (week_plan_id, teacher_user_id)
);

CREATE TABLE IF NOT EXISTS curriculum_reminder_dispatches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_user_id UUID NOT NULL REFERENCES users(id),
    week_plan_id UUID NOT NULL REFERENCES curriculum_week_plans(id) ON DELETE CASCADE,
    reminder_type VARCHAR(40) NOT NULL,
    reminder_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_curriculum_reminder_dispatch UNIQUE (teacher_user_id, week_plan_id, reminder_type, reminder_date)
);

CREATE TABLE IF NOT EXISTS curriculum_risk_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    district_id UUID NOT NULL REFERENCES districts(id),
    school_id UUID NOT NULL REFERENCES schools(id),
    teacher_user_id UUID NOT NULL REFERENCES users(id),
    week_plan_id UUID NOT NULL REFERENCES curriculum_week_plans(id) ON DELETE CASCADE,
    subject_id UUID REFERENCES school_subjects(id),
    severity VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'OPEN',
    title VARCHAR(255) NOT NULL,
    detail TEXT NOT NULL,
    notified_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_curriculum_risk_alert UNIQUE (teacher_user_id, week_plan_id)
);

CREATE INDEX IF NOT EXISTS idx_curriculum_assets_district ON curriculum_assets (district_id, repository_type, archived);
CREATE INDEX IF NOT EXISTS idx_curriculum_assets_school ON curriculum_assets (school_id, repository_type, archived);
CREATE INDEX IF NOT EXISTS idx_curriculum_week_plans_lookup ON curriculum_week_plans (district_id, subject, grade, term, week_number);
CREATE INDEX IF NOT EXISTS idx_teacher_curriculum_progress_teacher ON teacher_curriculum_progress (teacher_user_id, school_id);
CREATE INDEX IF NOT EXISTS idx_curriculum_risk_alerts_district ON curriculum_risk_alerts (district_id, status);
