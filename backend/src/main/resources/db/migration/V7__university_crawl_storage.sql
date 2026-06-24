CREATE TABLE IF NOT EXISTS crawled_university_pages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    university_name VARCHAR(160) NOT NULL,
    source_url VARCHAR(1200) NOT NULL UNIQUE,
    page_title VARCHAR(500),
    page_type VARCHAR(80),
    qualification_level VARCHAR(80),
    faculty_name VARCHAR(180),
    campus_name VARCHAR(180),
    cleaned_content TEXT,
    summary_excerpt VARCHAR(500),
    content_hash VARCHAR(128),
    crawl_status VARCHAR(20) NOT NULL,
    last_crawled_at TIMESTAMPTZ,
    last_successful_crawled_at TIMESTAMPTZ,
    last_failure_at TIMESTAMPTZ,
    error_type VARCHAR(100),
    failure_reason VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS crawled_university_page_keywords (
    page_id UUID NOT NULL REFERENCES crawled_university_pages(id) ON DELETE CASCADE,
    keyword VARCHAR(120) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_crawled_pages_university ON crawled_university_pages(university_name);
CREATE INDEX IF NOT EXISTS idx_crawled_pages_status ON crawled_university_pages(crawl_status);
CREATE INDEX IF NOT EXISTS idx_crawled_pages_last_crawled ON crawled_university_pages(last_crawled_at);
CREATE INDEX IF NOT EXISTS idx_crawled_pages_active_status ON crawled_university_pages(active, crawl_status);
CREATE INDEX IF NOT EXISTS idx_crawled_keywords_page ON crawled_university_page_keywords(page_id);
