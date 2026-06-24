ALTER TABLE users
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(30);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_phone_number
    ON users (phone_number)
    WHERE phone_number IS NOT NULL;

DROP TABLE IF EXISTS email_verification_tokens;

DROP INDEX IF EXISTS uk_users_password_reset_token;
DROP INDEX IF EXISTS idx_users_password_reset_token_expires_at;

ALTER TABLE users
    DROP COLUMN IF EXISTS password_reset_token,
    DROP COLUMN IF EXISTS password_reset_token_expires_at;
