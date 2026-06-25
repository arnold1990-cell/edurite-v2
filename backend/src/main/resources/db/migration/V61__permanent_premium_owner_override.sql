UPDATE users
SET plan_type = 'PREMIUM',
    updated_at = now()
WHERE LOWER(email) = LOWER('arnoldmadaz@gmail.com');

UPDATE subscriptions s
SET plan_code = 'PLAN_PREMIUM',
    status = 'ACTIVE',
    provider = 'internal',
    renewal_date = NULL,
    end_date = NULL,
    premium_until = NULL,
    trial_start_date = NULL,
    trial_end_date = NULL,
    trial_used = TRUE,
    cancel_at_period_end = FALSE,
    updated_at = now()
WHERE s.user_id IN (
    SELECT id
    FROM users
    WHERE LOWER(email) = LOWER('arnoldmadaz@gmail.com')
);

INSERT INTO subscriptions (
    id,
    user_id,
    plan_code,
    status,
    provider,
    start_date,
    end_date,
    renewal_date,
    premium_until,
    trial_start_date,
    trial_end_date,
    trial_used,
    cancel_at_period_end,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    'PLAN_PREMIUM',
    'ACTIVE',
    'internal',
    CURRENT_DATE,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    TRUE,
    FALSE,
    now(),
    now()
FROM users u
WHERE LOWER(u.email) = LOWER('arnoldmadaz@gmail.com')
  AND NOT EXISTS (
      SELECT 1
      FROM subscriptions s
      WHERE s.user_id = u.id
  );
