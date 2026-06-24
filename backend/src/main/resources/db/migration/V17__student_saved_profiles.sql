CREATE TABLE IF NOT EXISTS student_saved_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    student_id UUID REFERENCES students(id) ON DELETE SET NULL,
    name VARCHAR(120) NOT NULL,
    profile_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_student_saved_profiles_user_id
    ON student_saved_profiles (user_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_student_saved_profiles_student_id
    ON student_saved_profiles (student_id);
