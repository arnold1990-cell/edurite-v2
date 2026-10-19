-- Additive intelligence metadata only. Existing programmes, requirements and crawl data are retained.
CREATE TABLE university_directory_records (
    id UUID PRIMARY KEY,
    university_name VARCHAR(200) NOT NULL UNIQUE,
    metadata_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
ALTER TABLE university_programmes ADD COLUMN published_evidence_json TEXT;
CREATE TABLE university_crawl_attempts (
    id UUID PRIMARY KEY,
    university_name VARCHAR(200) NOT NULL,
    source_url VARCHAR(1200) NOT NULL,
    status VARCHAR(40) NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_university_crawl_attempts_history ON university_crawl_attempts(university_name, created_at DESC);
