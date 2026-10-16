-- Platform access overrides are independent of payment and billing agreements.
CREATE UNIQUE INDEX subscriptions_one_active_admin_override
    ON subscriptions(user_id) WHERE provider = 'ADMIN_OVERRIDE' AND status = 'ACTIVE';

CREATE TABLE admin_subscription_changes (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id UUID NOT NULL REFERENCES users(id),
    administrator_id UUID NOT NULL REFERENCES users(id),
    administrator_email VARCHAR(255) NOT NULL,
    subscription_id UUID NOT NULL UNIQUE REFERENCES subscriptions(id),
    previous_plan VARCHAR(20) NOT NULL CHECK (previous_plan IN ('BASIC','PREMIUM','PRO')),
    new_plan VARCHAR(20) NOT NULL CHECK (new_plan IN ('BASIC','PREMIUM','PRO')),
    reason VARCHAR(1000) NOT NULL CHECK (length(trim(reason)) BETWEEN 1 AND 1000)
);
CREATE INDEX admin_subscription_changes_user_time ON admin_subscription_changes(user_id, created_at DESC);
