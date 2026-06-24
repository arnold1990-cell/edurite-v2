ALTER TABLE curriculum_assets
    ADD COLUMN IF NOT EXISTS source VARCHAR(40);

ALTER TABLE curriculum_assets
    ADD COLUMN IF NOT EXISTS visibility VARCHAR(40);

ALTER TABLE curriculum_assets
    ADD COLUMN IF NOT EXISTS status VARCHAR(40);

ALTER TABLE curriculum_assets
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE curriculum_assets
SET source = CASE
        WHEN owner_scope = 'SCHOOL' THEN 'SCHOOL'
        ELSE 'DISTRICT'
    END
WHERE source IS NULL OR btrim(source) = '';

UPDATE curriculum_assets
SET visibility = CASE
        WHEN owner_scope = 'SCHOOL' THEN 'SCHOOL_ONLY'
        ELSE 'DISTRICT_WIDE'
    END
WHERE visibility IS NULL OR btrim(visibility) = '';

UPDATE curriculum_assets
SET status = 'ACTIVE'
WHERE status IS NULL OR btrim(status) = '';

UPDATE curriculum_assets
SET deleted = FALSE
WHERE deleted IS NULL;

ALTER TABLE curriculum_assets
    ALTER COLUMN source SET DEFAULT 'DISTRICT';

ALTER TABLE curriculum_assets
    ALTER COLUMN visibility SET DEFAULT 'DISTRICT_WIDE';

ALTER TABLE curriculum_assets
    ALTER COLUMN status SET DEFAULT 'ACTIVE';

ALTER TABLE curriculum_assets
    ALTER COLUMN source SET NOT NULL;

ALTER TABLE curriculum_assets
    ALTER COLUMN visibility SET NOT NULL;

ALTER TABLE curriculum_assets
    ALTER COLUMN status SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_curriculum_assets_visibility_status
    ON curriculum_assets (district_id, source, visibility, status, archived, deleted, active);
