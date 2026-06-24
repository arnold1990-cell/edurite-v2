UPDATE pricing_plans
SET amount = 29.00,
    updated_at = now()
WHERE code = 'PLAN_PREMIUM'
  AND amount <> 29.00;
