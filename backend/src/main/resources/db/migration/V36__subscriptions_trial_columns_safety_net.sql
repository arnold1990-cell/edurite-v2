ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS trial_start_date TIMESTAMP WITH TIME ZONE;

ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS trial_end_date TIMESTAMP WITH TIME ZONE;

ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS premium_until TIMESTAMP WITH TIME ZONE;

ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS trial_used BOOLEAN;

UPDATE subscriptions
SET trial_used = FALSE
WHERE trial_used IS NULL;

ALTER TABLE subscriptions
    ALTER COLUMN trial_used SET DEFAULT FALSE;

ALTER TABLE subscriptions
    ALTER COLUMN trial_used SET NOT NULL;
