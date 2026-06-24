ALTER TABLE users
    ADD COLUMN IF NOT EXISTS plan_type VARCHAR(20) NOT NULL DEFAULT 'BASIC';

UPDATE users u
SET plan_type = 'PREMIUM'
WHERE EXISTS (
    SELECT 1
    FROM subscriptions s
    WHERE s.user_id = u.id
      AND UPPER(COALESCE(s.plan_code, '')) IN ('PLAN_PREMIUM', 'PREMIUM')
      AND UPPER(COALESCE(s.status, '')) = 'ACTIVE'
);
