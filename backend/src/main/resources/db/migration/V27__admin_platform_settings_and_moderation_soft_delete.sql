CREATE TABLE IF NOT EXISTS platform_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_self_registration_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    manual_company_approval_required BOOLEAN NOT NULL DEFAULT TRUE,
    bursary_posting_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    student_registration_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    bursary_moderation_required BOOLEAN NOT NULL DEFAULT FALSE,
    ai_guidance_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    maintenance_mode_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    support_email VARCHAR(255),
    platform_contact_info TEXT,
    max_csv_bulk_upload_rows INTEGER NOT NULL DEFAULT 500,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO platform_settings (
    company_self_registration_enabled,
    manual_company_approval_required,
    bursary_posting_enabled,
    student_registration_enabled,
    bursary_moderation_required,
    ai_guidance_enabled,
    maintenance_mode_enabled,
    max_csv_bulk_upload_rows
)
SELECT TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE, 500
WHERE NOT EXISTS (SELECT 1 FROM platform_settings);

ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS deletion_reason TEXT;

ALTER TABLE bursaries
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS deletion_reason TEXT;

CREATE INDEX IF NOT EXISTS idx_companies_deleted_at ON companies(deleted_at);
CREATE INDEX IF NOT EXISTS idx_bursaries_deleted_at ON bursaries(deleted_at);

