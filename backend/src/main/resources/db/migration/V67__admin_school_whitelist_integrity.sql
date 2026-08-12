DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = 'public'
          AND indexname = 'uk_schools_registration_number_ci'
    ) AND NOT EXISTS (
        SELECT LOWER(registration_number)
        FROM schools
        WHERE registration_number IS NOT NULL AND TRIM(registration_number) <> ''
        GROUP BY LOWER(registration_number)
        HAVING COUNT(*) > 1
    ) THEN
        CREATE UNIQUE INDEX uk_schools_registration_number_ci
            ON schools (LOWER(registration_number))
            WHERE registration_number IS NOT NULL AND TRIM(registration_number) <> '';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = 'public'
          AND indexname = 'uk_schools_school_code_ci'
    ) AND NOT EXISTS (
        SELECT LOWER(school_code)
        FROM schools
        WHERE school_code IS NOT NULL AND TRIM(school_code) <> ''
        GROUP BY LOWER(school_code)
        HAVING COUNT(*) > 1
    ) THEN
        CREATE UNIQUE INDEX uk_schools_school_code_ci
            ON schools (LOWER(school_code))
            WHERE school_code IS NOT NULL AND TRIM(school_code) <> '';
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_school_user_profiles_role_school
    ON school_user_profiles (school_id, role_name, active, deleted);
