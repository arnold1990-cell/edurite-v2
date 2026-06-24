ALTER TABLE school_subjects
    ADD COLUMN IF NOT EXISTS hod_user_id UUID NULL,
    ADD COLUMN IF NOT EXISTS caps_aligned BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_school_subjects_hod_user_id
    ON school_subjects (school_id, hod_user_id);
