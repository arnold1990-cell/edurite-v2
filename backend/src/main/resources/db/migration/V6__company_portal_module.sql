ALTER TABLE companies
    RENAME COLUMN name TO company_name;

ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS registration_number VARCHAR(120),
    ADD COLUMN IF NOT EXISTS official_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS mobile_number VARCHAR(30),
    ADD COLUMN IF NOT EXISTS contact_person_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS address VARCHAR(255),
    ADD COLUMN IF NOT EXISTS website VARCHAR(255),
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS mobile_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS reviewed_by UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS review_notes TEXT;

UPDATE companies c
SET official_email = u.email
    FROM users u
WHERE c.user_id = u.id
  AND c.official_email IS NULL;

UPDATE companies
SET official_email = CONCAT('company-', id, '@placeholder.local')
WHERE official_email IS NULL;

UPDATE companies
SET company_name = 'Unknown Company'
WHERE company_name IS NULL;

UPDATE companies
SET registration_number = CONCAT('TEMP-', id)
WHERE registration_number IS NULL;

ALTER TABLE companies
    ALTER COLUMN official_email SET NOT NULL,
ALTER COLUMN company_name SET NOT NULL,
    ALTER COLUMN registration_number SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_companies_registration_number ON companies(registration_number);
CREATE UNIQUE INDEX IF NOT EXISTS uk_companies_official_email ON companies(official_email);
CREATE INDEX IF NOT EXISTS idx_companies_status ON companies(status);
CREATE INDEX IF NOT EXISTS idx_companies_reviewed_by ON companies(reviewed_by);

ALTER TABLE company_documents
    ADD COLUMN IF NOT EXISTS file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS uploaded_by UUID REFERENCES users(id);

CREATE TABLE IF NOT EXISTS company_password_reset_tokens (
                                                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    token VARCHAR(120) UNIQUE NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_company_password_reset_company ON company_password_reset_tokens(company_id);
CREATE INDEX IF NOT EXISTS idx_company_password_reset_expires ON company_password_reset_tokens(expires_at);

ALTER TABLE bursaries
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS field_of_study VARCHAR(150),
    ADD COLUMN IF NOT EXISTS application_start_date DATE,
    ADD COLUMN IF NOT EXISTS application_end_date DATE,
    ADD COLUMN IF NOT EXISTS funding_amount DECIMAL(14,2),
    ADD COLUMN IF NOT EXISTS benefits TEXT,
    ADD COLUMN IF NOT EXISTS required_subjects TEXT,
    ADD COLUMN IF NOT EXISTS minimum_grade VARCHAR(20),
    ADD COLUMN IF NOT EXISTS demographics TEXT,
    ADD COLUMN IF NOT EXISTS location VARCHAR(120),
    ADD COLUMN IF NOT EXISTS eligibility_filters JSONB;

UPDATE bursaries
SET application_end_date = COALESCE(application_end_date, deadline),
    funding_amount = COALESCE(funding_amount, amount),
    location = COALESCE(location, region);

CREATE INDEX IF NOT EXISTS idx_bursaries_company_status ON bursaries(company_id, status);