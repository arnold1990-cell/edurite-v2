\set ON_ERROR_STOP on
-- Required psql variables: recipient_email, administrator, reason.
BEGIN;
UPDATE complimentary_pro_grants SET status = 'REVOKED', revoked_by = :'administrator', revocation_reason = :'reason'
WHERE recipient_email = normalize_complimentary_email(:'recipient_email') AND status <> 'REVOKED';
SELECT recipient_email, status, activated_at, revoked_at, revoked_by, revocation_reason
FROM complimentary_pro_grants WHERE recipient_email = normalize_complimentary_email(:'recipient_email');
COMMIT;
