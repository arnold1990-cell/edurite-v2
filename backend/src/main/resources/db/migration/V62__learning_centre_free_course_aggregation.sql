--noinspection SqlNoDataSourceInspection
ALTER TABLE learning_resources
    ADD COLUMN IF NOT EXISTS provider VARCHAR(255),
    ADD COLUMN IF NOT EXISTS category VARCHAR(255),
    ADD COLUMN IF NOT EXISTS subject VARCHAR(255),
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS course_url TEXT,
    ADD COLUMN IF NOT EXISTS thumbnail_url TEXT,
    ADD COLUMN IF NOT EXISTS level VARCHAR(100),
    ADD COLUMN IF NOT EXISTS language VARCHAR(100),
    ADD COLUMN IF NOT EXISTS is_free BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_fetched_at TIMESTAMP WITH TIME ZONE;

UPDATE learning_resources
SET provider = COALESCE(NULLIF(provider, ''), 'EduRite'),
    category = COALESCE(NULLIF(category, ''), 'Study Materials'),
    subject = COALESCE(NULLIF(subject, ''), COALESCE(NULLIF(category, ''), 'Study Materials')),
    description = COALESCE(NULLIF(description, ''), summary),
    course_url = COALESCE(NULLIF(course_url, ''), url),
    level = COALESCE(NULLIF(level, ''), difficulty),
    language = COALESCE(NULLIF(language, ''), 'English'),
    source_type = COALESCE(NULLIF(source_type, ''), 'DATABASE')
WHERE TRUE;

CREATE INDEX IF NOT EXISTS idx_learning_resources_free_courses
    ON learning_resources (is_free, resource_type, provider, category, subject);

