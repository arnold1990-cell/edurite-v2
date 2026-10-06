-- One-time backfill, anchored to historical account creation (never migration time).
-- Preserve paid subscriptions and every payment/learner record.
-- All free records for an account share the earliest historical start, preventing retries.
WITH anchors AS (
    SELECT u.id, LEAST(u.created_at, MIN(s.trial_start_date)) AS started, MIN(s.trial_end_date) AS previous_expiry
    FROM users u LEFT JOIN subscriptions s ON s.user_id = u.id
    GROUP BY u.id, u.created_at
)
UPDATE subscriptions s
SET trial_start_date = a.started,
    trial_end_date = LEAST(a.previous_expiry, a.started + INTERVAL '336 hours'),
    trial_used = TRUE, premium_until = NULL
FROM anchors a
WHERE s.user_id = a.id AND s.plan_code IN ('PLAN_BASIC', 'BASIC', 'PLAN_TRIAL', 'TRIAL');

-- Accounts without any subscription get a historical record, not a fresh trial.
INSERT INTO subscriptions (id, user_id, plan_code, status, provider,
    start_date, end_date, trial_start_date, trial_end_date, trial_used, created_at, updated_at)
SELECT gen_random_uuid(), u.id, 'PLAN_BASIC', 'ACTIVE', 'internal',
    (u.created_at AT TIME ZONE 'UTC')::date,
    ((u.created_at + INTERVAL '336 hours') AT TIME ZONE 'UTC')::date,
    u.created_at, u.created_at + INTERVAL '336 hours', TRUE, u.created_at, now()
FROM users u
WHERE EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
              WHERE ur.user_id = u.id AND r.name IN ('STUDENT', 'ROLE_STUDENT'))
  AND NOT EXISTS (SELECT 1 FROM subscriptions s WHERE s.user_id = u.id);

UPDATE pricing_plans SET name = 'Free Trial',
    description = '14 days of Basic features. Choose a paid plan to continue after your trial.',
    billing_interval = 'TRIAL'
WHERE code = 'PLAN_BASIC';
