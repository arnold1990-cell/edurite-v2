-- Additive only. Does not change users, passwords, roles or verification status.
CREATE TABLE email_password_resets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    otp_hash VARCHAR(255),
    expires_at TIMESTAMPTZ,
    reset_token_hash VARCHAR(64),
    reset_token_expires_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    requested_at TIMESTAMPTZ,
    window_started_at TIMESTAMPTZ,
    request_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
