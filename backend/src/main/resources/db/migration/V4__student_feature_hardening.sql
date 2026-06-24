CREATE INDEX IF NOT EXISTS idx_saved_opportunities_student_id ON saved_opportunities(student_id);
CREATE INDEX IF NOT EXISTS idx_saved_opportunities_career_id ON saved_opportunities(career_id);
CREATE INDEX IF NOT EXISTS idx_saved_bursaries_student_id ON saved_bursaries(student_id);
CREATE INDEX IF NOT EXISTS idx_saved_bursaries_bursary_id ON saved_bursaries(bursary_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, is_read);

ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(120),
    ADD COLUMN IF NOT EXISTS start_date DATE,
    ADD COLUMN IF NOT EXISTS end_date DATE;

ALTER TABLE subscriptions
    ADD CONSTRAINT chk_subscription_plan_code
    CHECK (plan_code IN ('PLAN_BASIC', 'PLAN_PREMIUM'));

ALTER TABLE subscriptions
    ADD CONSTRAINT chk_subscription_status
    CHECK (status IN ('ACTIVE', 'PENDING', 'CANCELLED', 'EXPIRED'));
