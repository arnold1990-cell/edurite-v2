-- Access is resolved from recorded timestamps on every request, even after inactivity.
-- Keep all historical trial dates, payments and saved learner data intact.
UPDATE pricing_plans SET name = 'Basic',
    description = 'Free Basic access. Eligible accounts receive Premium for 14 days from their recorded trial start.',
    billing_interval = 'FREE'
WHERE code = 'PLAN_BASIC';

ALTER TABLE payments ADD COLUMN checkout_payload TEXT;
UPDATE payments SET checkout_payload = (metadata::jsonb -> 'response')::text
WHERE metadata::jsonb -> 'response' -> 'formFields' IS NOT NULL;
