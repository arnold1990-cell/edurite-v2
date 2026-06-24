ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS provider VARCHAR(40),
    ADD COLUMN IF NOT EXISTS provider_subscription_id VARCHAR(180),
    ADD COLUMN IF NOT EXISTS last_payment_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS provider_order_id VARCHAR(180),
    ADD COLUMN IF NOT EXISTS provider_session_id VARCHAR(180),
    ADD COLUMN IF NOT EXISTS provider_payment_id VARCHAR(180),
    ADD COLUMN IF NOT EXISTS provider_subscription_id VARCHAR(180),
    ADD COLUMN IF NOT EXISTS checkout_url TEXT,
    ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_payments_provider_order_id
    ON payments (provider, provider_order_id);

CREATE INDEX IF NOT EXISTS idx_payments_provider_session_id
    ON payments (provider, provider_session_id);

CREATE INDEX IF NOT EXISTS idx_payments_provider_payment_id
    ON payments (provider, provider_payment_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_reference
    ON payments (reference)
    WHERE reference IS NOT NULL;

CREATE TABLE IF NOT EXISTS payment_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID REFERENCES payments(id) ON DELETE SET NULL,
    provider VARCHAR(40) NOT NULL,
    event_id VARCHAR(180) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_events_provider_event_id
    ON payment_events (provider, event_id);

CREATE INDEX IF NOT EXISTS idx_payment_events_payment_id
    ON payment_events (payment_id);
