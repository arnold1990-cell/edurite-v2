ALTER TABLE saved_opportunities
    ADD COLUMN IF NOT EXISTS opportunity_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS external_opportunity_key VARCHAR(120),
    ADD COLUMN IF NOT EXISTS title_snapshot VARCHAR(255);

UPDATE saved_opportunities
SET opportunity_type = 'CAREER'
WHERE opportunity_type IS NULL
  AND career_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_saved_opportunities_type_external
    ON saved_opportunities(student_id, opportunity_type, external_opportunity_key);
