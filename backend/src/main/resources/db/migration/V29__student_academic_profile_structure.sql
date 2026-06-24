ALTER TABLE students
    ADD COLUMN IF NOT EXISTS selected_grade VARCHAR(30),
    ADD COLUMN IF NOT EXISTS subject_achievements_json JSONB NOT NULL DEFAULT '[]'::jsonb;
