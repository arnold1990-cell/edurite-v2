\set ON_ERROR_STOP on
BEGIN;
-- Resolve only existing, uniquely named, active accounts. Never infer an email.
LOCK TABLE users IN SHARE MODE;
LOCK TABLE subscriptions IN SHARE ROW EXCLUSIVE MODE;
DO $$
DECLARE target_name text; matches uuid[]; target_id uuid;
BEGIN
  -- Siphelele is handled exclusively by the email-verified invitation in migration V76.
  FOREACH target_name IN ARRAY ARRAY['Arnold Tyvern Madamombe'] LOOP
    SELECT array_agg(id) INTO matches FROM users
      WHERE lower(regexp_replace(trim(concat_ws(' ', first_name, last_name)), '\s+', ' ', 'g')) = lower(target_name)
        AND deleted_at IS NULL;
    IF coalesce(cardinality(matches), 0) <> 1 THEN
      RAISE NOTICE 'NOT GRANTED: %, matches=%. Supply the confirmed existing user UUID.', target_name, coalesce(cardinality(matches), 0);
      CONTINUE;
    END IF;
    target_id := matches[1];
    IF NOT EXISTS (SELECT 1 FROM users WHERE id = target_id AND status = 'ACTIVE') THEN
      RAISE NOTICE 'NOT GRANTED: % is inactive (UUID %).', target_name, target_id;
      CONTINUE;
    END IF;
    INSERT INTO subscriptions (id, user_id, plan_code, status, provider, start_date, trial_used, created_at, updated_at)
      SELECT gen_random_uuid(), target_id, 'PLAN_PRO', 'ACTIVE', 'manual-testing', (now() AT TIME ZONE 'UTC')::date, true, now(), now()
      WHERE NOT EXISTS (SELECT 1 FROM subscriptions WHERE user_id = target_id AND provider = 'manual-testing' AND plan_code = 'PLAN_PRO' AND status = 'ACTIVE');
    UPDATE users SET plan_type = 'PRO', updated_at = now() WHERE id = target_id;
    RAISE NOTICE 'GRANTED Pro testing: % (UUID %)', target_name, target_id;
  END LOOP;
END $$;
SELECT u.id, u.first_name, u.last_name, s.plan_code, s.status, s.provider
FROM users u JOIN subscriptions s ON s.user_id = u.id
WHERE s.provider = 'manual-testing' AND s.status = 'ACTIVE';
COMMIT;
