-- Additive only: existing accounts retain their authentication state.
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verification_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verification_hash VARCHAR(64);
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verification_expires_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verification_sent_at TIMESTAMPTZ;
ALTER TABLE students ADD COLUMN IF NOT EXISTS school_name VARCHAR(200);
ALTER TABLE students ADD COLUMN IF NOT EXISTS transcript_history_json JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE students ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;
