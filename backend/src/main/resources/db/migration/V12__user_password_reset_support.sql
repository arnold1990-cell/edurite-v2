ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_reset_token VARCHAR(255),
    ADD COLUMN IF NOT EXISTS password_reset_token_expires_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_password_reset_token
    ON users (password_reset_token)
    WHERE password_reset_token IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_password_reset_token_expires_at
    ON users (password_reset_token_expires_at);
