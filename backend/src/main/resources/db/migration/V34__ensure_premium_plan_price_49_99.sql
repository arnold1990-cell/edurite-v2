UPDATE pricing_plans
SET amount = 49.99,
    updated_at = NOW()
WHERE code = 'PLAN_PREMIUM'
  AND amount <> 49.99;
