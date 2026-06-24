ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS verification_token VARCHAR(128),
    ADD COLUMN IF NOT EXISTS verification_token_expiry TIMESTAMPTZ;

ALTER TABLE users
    ALTER COLUMN email_verified SET DEFAULT FALSE;

UPDATE users
SET email_verified = FALSE
WHERE email_verified IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_verification_token
    ON users (verification_token)
    WHERE verification_token IS NOT NULL;
