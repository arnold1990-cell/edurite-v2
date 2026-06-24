ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS callback_received_at TIMESTAMPTZ;

ALTER TABLE learning_resources
    ADD COLUMN IF NOT EXISTS resource_type VARCHAR(40) NOT NULL DEFAULT 'LINK';
