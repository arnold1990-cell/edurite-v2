ALTER TABLE curriculum_assets
    ADD COLUMN IF NOT EXISTS extraction_status VARCHAR(40) NOT NULL DEFAULT 'PENDING';

ALTER TABLE curriculum_assets
    ADD COLUMN IF NOT EXISTS extraction_error TEXT;

ALTER TABLE curriculum_assets
    ADD COLUMN IF NOT EXISTS extracted_at TIMESTAMPTZ;

ALTER TABLE curriculum_week_plans
    ADD COLUMN IF NOT EXISTS start_date DATE;

ALTER TABLE curriculum_week_plans
    ADD COLUMN IF NOT EXISTS end_date DATE;

ALTER TABLE curriculum_week_plans
    ADD COLUMN IF NOT EXISTS resources_materials TEXT;

ALTER TABLE curriculum_week_plans
    ADD COLUMN IF NOT EXISTS lesson_focus TEXT;

ALTER TABLE curriculum_week_plans
    ADD COLUMN IF NOT EXISTS notes TEXT;

ALTER TABLE curriculum_week_plans
    ADD COLUMN IF NOT EXISTS status VARCHAR(40) NOT NULL DEFAULT 'PUBLISHED';

CREATE TABLE IF NOT EXISTS atp_calendar_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    curriculum_resource_id UUID NOT NULL REFERENCES curriculum_assets(id) ON DELETE CASCADE,
    subject VARCHAR(255) NOT NULL,
    grade VARCHAR(80) NOT NULL,
    phase VARCHAR(120),
    academic_year INTEGER,
    term VARCHAR(40) NOT NULL,
    week_number INTEGER NOT NULL,
    start_date DATE,
    end_date DATE,
    topic VARCHAR(255) NOT NULL,
    subtopic VARCHAR(255),
    learning_objectives TEXT,
    resources TEXT,
    assessment_task TEXT,
    lesson_focus TEXT,
    notes TEXT,
    status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_atp_calendar_items_resource ON atp_calendar_items (curriculum_resource_id, status);
CREATE INDEX IF NOT EXISTS idx_atp_calendar_items_lookup ON atp_calendar_items (subject, grade, academic_year, term, week_number);

CREATE TABLE IF NOT EXISTS atp_teacher_reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    atp_calendar_item_id UUID NOT NULL REFERENCES atp_calendar_items(id) ON DELETE CASCADE,
    school_id UUID NOT NULL REFERENCES schools(id),
    teacher_id UUID REFERENCES users(id),
    subject VARCHAR(255) NOT NULL,
    grade VARCHAR(80) NOT NULL,
    reminder_type VARCHAR(40) NOT NULL,
    reminder_date TIMESTAMPTZ NOT NULL,
    reminder_message TEXT NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_atp_teacher_reminder UNIQUE (atp_calendar_item_id, school_id, teacher_id, reminder_type, reminder_date)
);

CREATE INDEX IF NOT EXISTS idx_atp_teacher_reminders_due ON atp_teacher_reminders (reminder_date, status);
CREATE INDEX IF NOT EXISTS idx_atp_teacher_reminders_teacher ON atp_teacher_reminders (teacher_id, status);

CREATE TABLE IF NOT EXISTS teacher_atp_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id UUID NOT NULL REFERENCES users(id),
    school_id UUID NOT NULL REFERENCES schools(id),
    atp_calendar_item_id UUID NOT NULL REFERENCES atp_calendar_items(id) ON DELETE CASCADE,
    status VARCHAR(40) NOT NULL DEFAULT 'NOT_STARTED',
    completion_percentage INTEGER NOT NULL DEFAULT 0,
    evidence_file TEXT,
    comment TEXT,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_teacher_atp_progress UNIQUE (teacher_id, school_id, atp_calendar_item_id)
);

CREATE INDEX IF NOT EXISTS idx_teacher_atp_progress_teacher ON teacher_atp_progress (teacher_id, school_id, status);
