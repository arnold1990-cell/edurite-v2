ALTER TABLE users
    ADD COLUMN IF NOT EXISTS username VARCHAR(255),
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_username
    ON users (LOWER(username))
    WHERE username IS NOT NULL;

ALTER TABLE districts
    ADD COLUMN IF NOT EXISTS director_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS admin_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS admin_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(30),
    ADD COLUMN IF NOT EXISTS address TEXT,
    ADD COLUMN IF NOT EXISTS status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE';

UPDATE districts
SET phone_number = COALESCE(phone_number, contact_phone),
    admin_email = COALESCE(admin_email, contact_email),
    address = COALESCE(address, ''),
    status = COALESCE(NULLIF(status, ''), COALESCE(NULLIF(licensing_status, ''), 'ACTIVE'))
WHERE TRUE;
