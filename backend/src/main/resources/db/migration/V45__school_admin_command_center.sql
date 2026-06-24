ALTER TABLE school_user_profiles
    ADD COLUMN IF NOT EXISTS portal_username VARCHAR(255),
    ADD COLUMN IF NOT EXISTS initial_password VARCHAR(255),
    ADD COLUMN IF NOT EXISTS guardian_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS guardian_phone VARCHAR(255),
    ADD COLUMN IF NOT EXISTS guardian_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS consent_status VARCHAR(50),
    ADD COLUMN IF NOT EXISTS report_upload_status VARCHAR(50);

CREATE TABLE IF NOT EXISTS school_announcements (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    school_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    audience VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    sent_at TIMESTAMP WITH TIME ZONE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_school_announcements_school_id
    ON school_announcements (school_id, created_at DESC);

CREATE TABLE IF NOT EXISTS school_support_requests (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    school_id UUID NOT NULL,
    requester_user_id UUID NOT NULL,
    category VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_school_support_requests_school_id
    ON school_support_requests (school_id, created_at DESC);
