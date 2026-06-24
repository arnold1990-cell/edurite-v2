DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'school_announcements'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'school_announcements'
          AND constraint_name = 'fk_school_announcements_school'
    ) THEN
        ALTER TABLE school_announcements
            ADD CONSTRAINT fk_school_announcements_school
            FOREIGN KEY (school_id) REFERENCES schools(id);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'school_announcements'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'school_announcements'
          AND constraint_name = 'fk_school_announcements_created_by'
    ) THEN
        ALTER TABLE school_announcements
            ADD CONSTRAINT fk_school_announcements_created_by
            FOREIGN KEY (created_by_user_id) REFERENCES users(id);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'school_support_requests'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'school_support_requests'
          AND constraint_name = 'fk_school_support_requests_school'
    ) THEN
        ALTER TABLE school_support_requests
            ADD CONSTRAINT fk_school_support_requests_school
            FOREIGN KEY (school_id) REFERENCES schools(id);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'school_support_requests'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'school_support_requests'
          AND constraint_name = 'fk_school_support_requests_requester'
    ) THEN
        ALTER TABLE school_support_requests
            ADD CONSTRAINT fk_school_support_requests_requester
            FOREIGN KEY (requester_user_id) REFERENCES users(id);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'school_tasks'
          AND column_name = 'atp_topic_id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'school_tasks'
          AND constraint_name = 'fk_school_tasks_atp_topic'
    ) THEN
        ALTER TABLE school_tasks
            ADD CONSTRAINT fk_school_tasks_atp_topic
            FOREIGN KEY (atp_topic_id) REFERENCES atp_topics(id);
    END IF;
END $$;
