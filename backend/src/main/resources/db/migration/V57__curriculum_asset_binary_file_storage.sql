ALTER TABLE curriculum_assets
    ADD COLUMN IF NOT EXISTS pdf_bytes BYTEA,
    ADD COLUMN IF NOT EXISTS docx_bytes BYTEA,
    ADD COLUMN IF NOT EXISTS excel_bytes BYTEA;

UPDATE curriculum_assets
SET pdf_bytes = decode(regexp_replace(pdf_base64, '\s', '', 'g'), 'base64')
WHERE pdf_bytes IS NULL
  AND pdf_base64 IS NOT NULL
  AND btrim(pdf_base64) <> ''
  AND regexp_replace(pdf_base64, '\s', '', 'g') ~ '^[A-Za-z0-9+/]*={0,2}$';

UPDATE curriculum_assets
SET docx_bytes = decode(regexp_replace(docx_base64, '\s', '', 'g'), 'base64')
WHERE docx_bytes IS NULL
  AND docx_base64 IS NOT NULL
  AND btrim(docx_base64) <> ''
  AND regexp_replace(docx_base64, '\s', '', 'g') ~ '^[A-Za-z0-9+/]*={0,2}$';

UPDATE curriculum_assets
SET excel_bytes = decode(regexp_replace(excel_base64, '\s', '', 'g'), 'base64')
WHERE excel_bytes IS NULL
  AND excel_base64 IS NOT NULL
  AND btrim(excel_base64) <> ''
  AND regexp_replace(excel_base64, '\s', '', 'g') ~ '^[A-Za-z0-9+/]*={0,2}$';
