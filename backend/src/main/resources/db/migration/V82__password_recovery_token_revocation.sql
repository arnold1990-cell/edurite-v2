-- Additive: existing tokens remain valid at version zero until an account resets its password.
ALTER TABLE users ADD COLUMN IF NOT EXISTS token_version BIGINT NOT NULL DEFAULT 0;
