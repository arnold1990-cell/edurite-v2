\set ON_ERROR_STOP on
-- Disposable local journey database ONLY; never execute against an application database.
DO $$ BEGIN
  IF current_database() <> 'edurite_journey_20261014' THEN RAISE EXCEPTION 'Test database required'; END IF;
END $$;
INSERT INTO roles(name) VALUES ('ROLE_STUDENT') ON CONFLICT(name) DO NOTHING;
INSERT INTO users(email,password_hash,first_name,last_name,status,email_verified,email_verification_required,must_change_password,plan_type)
SELECT email,crypt('JourneyFixture-2026!',gen_salt('bf')),'Journey','Fixture','ACTIVE',true,false,false,'BASIC'
FROM (VALUES ('journey-pro@example.test'),('journey-basic@example.test')) AS fixture(email)
ON CONFLICT(email) DO NOTHING;
INSERT INTO user_roles(user_id,role_id)
SELECT u.id,r.id FROM users u CROSS JOIN roles r
WHERE u.email IN ('journey-pro@example.test','journey-basic@example.test') AND r.name='ROLE_STUDENT'
ON CONFLICT DO NOTHING;
INSERT INTO students(user_id) SELECT id FROM users WHERE email IN ('journey-pro@example.test','journey-basic@example.test') ON CONFLICT(user_id) DO NOTHING;
INSERT INTO subscriptions(user_id,plan_code,status,provider,start_date,trial_used)
SELECT u.id,'PLAN_PRO','ACTIVE','manual-testing',current_date,true FROM users u
WHERE email='journey-pro@example.test' AND NOT EXISTS (SELECT 1 FROM subscriptions s WHERE s.user_id=u.id);
