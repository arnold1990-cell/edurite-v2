\set ON_ERROR_STOP on
-- Read-only production evidence for the requested invitation.
SELECT g.id, g.recipient_email, g.reason, g.status, g.created_at, g.activated_at,
       g.revoked_at, g.revoked_by, g.user_id, g.subscription_id,
       u.email_verified, u.email_verification_required,
       s.plan_code, s.status AS subscription_status, s.provider, s.end_date,
       (SELECT count(*) FROM payments p WHERE p.subscription_id = g.subscription_id) AS grant_payment_count,
       (SELECT count(*) FROM student_ai_usage a WHERE a.user_id = g.user_id
           AND a.period_start = date_trunc('month', now() AT TIME ZONE 'UTC')::date
           AND a.status = 'SUCCEEDED') AS current_month_used
FROM complimentary_pro_grants g
LEFT JOIN users u ON u.id = g.user_id
LEFT JOIN subscriptions s ON s.id = g.subscription_id
WHERE g.recipient_email = 's.mngaza07@gmail.com';
