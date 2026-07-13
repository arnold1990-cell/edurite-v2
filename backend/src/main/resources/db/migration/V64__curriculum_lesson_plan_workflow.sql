-- noinspection SqlNoDataSourceInspection
ALTER TABLE curriculum_assets
    ADD COLUMN IF NOT EXISTS teacher_user_id UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS source_curriculum_asset_id UUID REFERENCES curriculum_assets(id),
    ADD COLUMN IF NOT EXISTS source_atp_calendar_item_id UUID REFERENCES atp_calendar_items(id),
    ADD COLUMN IF NOT EXISTS lesson_plan_status VARCHAR(40),
    ADD COLUMN IF NOT EXISTS lesson_topic VARCHAR(255),
    ADD COLUMN IF NOT EXISTS lesson_date DATE,
    ADD COLUMN IF NOT EXISTS lesson_duration_minutes INTEGER,
    ADD COLUMN IF NOT EXISTS class_id UUID REFERENCES school_classes(id),
    ADD COLUMN IF NOT EXISTS language VARCHAR(80),
    ADD COLUMN IF NOT EXISTS generation_request_key VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ai_provider VARCHAR(80),
    ADD COLUMN IF NOT EXISTS ai_model VARCHAR(120),
    ADD COLUMN IF NOT EXISTS ai_generated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS generated_by_ai BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS lesson_plan_payload_json TEXT,
    ADD COLUMN IF NOT EXISTS ai_metadata_json TEXT;

UPDATE curriculum_assets
SET lesson_plan_status = CASE
        WHEN repository_type = 'LESSON_PLAN' THEN 'PUBLISHED'
        ELSE lesson_plan_status
    END
WHERE lesson_plan_status IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_curriculum_assets_generation_request_key
    ON curriculum_assets (generation_request_key)
    WHERE generation_request_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_curriculum_assets_lesson_plan_lookup
    ON curriculum_assets (school_id, repository_type, lesson_plan_status, teacher_user_id, class_id, academic_year, term, week_number);

CREATE INDEX IF NOT EXISTS idx_curriculum_assets_lesson_plan_source
    ON curriculum_assets (source_atp_calendar_item_id, source_curriculum_asset_id);
