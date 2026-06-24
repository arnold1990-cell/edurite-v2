UPDATE users
SET plan_type = 'PREMIUM',
    updated_at = now()
WHERE LOWER(email) = LOWER('recess_thymes0h@icloud.com');

UPDATE subscriptions s
SET plan_code = 'PLAN_PREMIUM',
    status = 'ACTIVE',
    renewal_date = NULL,
    end_date = NULL,
    premium_until = NULL,
    trial_end_date = NULL,
    trial_used = TRUE,
    updated_at = now()
WHERE s.user_id IN (
    SELECT id
    FROM users
    WHERE LOWER(email) = LOWER('recess_thymes0h@icloud.com')
);

INSERT INTO subscriptions (
    id,
    user_id,
    plan_code,
    status,
    start_date,
    trial_used,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    u.id,
    'PLAN_PREMIUM',
    'ACTIVE',
    CURRENT_DATE,
    TRUE,
    now(),
    now()
FROM users u
WHERE LOWER(u.email) = LOWER('recess_thymes0h@icloud.com')
  AND NOT EXISTS (
      SELECT 1
      FROM subscriptions s
      WHERE s.user_id = u.id
  );
