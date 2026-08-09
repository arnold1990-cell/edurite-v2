CREATE INDEX IF NOT EXISTS idx_users_upper_email
    ON users (UPPER(email))
    WHERE email IS NOT NULL;
