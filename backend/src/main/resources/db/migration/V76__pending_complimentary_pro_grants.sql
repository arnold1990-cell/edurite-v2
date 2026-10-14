-- Server-only invitations. No payment records, paid subscriptions or usage are changed.
CREATE FUNCTION normalize_complimentary_email(value TEXT) RETURNS TEXT
LANGUAGE sql IMMUTABLE STRICT AS $$
    SELECT lower(regexp_replace(value, '^[[:space:]]+|[[:space:]]+$', '', 'g'))
$$;

CREATE TABLE complimentary_pro_grants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_email VARCHAR(255) NOT NULL UNIQUE,
    reason TEXT NOT NULL CHECK (length(trim(reason)) > 0),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE', 'REVOKED')),
    user_id UUID REFERENCES users(id),
    subscription_id UUID UNIQUE REFERENCES subscriptions(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    activated_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    revoked_by TEXT,
    revocation_reason TEXT,
    CHECK (recipient_email = normalize_complimentary_email(recipient_email) AND length(recipient_email) > 0),
    CHECK ((activated_at IS NULL AND user_id IS NULL AND subscription_id IS NULL)
        OR (activated_at IS NOT NULL AND user_id IS NOT NULL AND subscription_id IS NOT NULL)),
    CHECK (status <> 'ACTIVE' OR activated_at IS NOT NULL),
    CHECK (status <> 'PENDING' OR activated_at IS NULL),
    CHECK (status <> 'REVOKED' OR (revoked_at IS NOT NULL AND length(trim(revoked_by)) > 0 AND length(trim(revocation_reason)) > 0))
);

CREATE FUNCTION activate_verified_complimentary_pro() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE invitation complimentary_pro_grants%ROWTYPE; granted_subscription UUID;
BEGIN
    IF NOT NEW.email_verified OR NEW.email_verification_required
        OR NEW.status <> 'ACTIVE' OR NEW.deleted_at IS NOT NULL THEN
        RETURN NEW;
    END IF;
    -- The users row is already locked by INSERT/UPDATE. Locking the unique invitation
    -- also serializes competing accounts with differently cased versions of an email.
    SELECT * INTO invitation FROM complimentary_pro_grants
    WHERE recipient_email = normalize_complimentary_email(NEW.email) AND status = 'PENDING' FOR UPDATE;
    IF NOT FOUND THEN RETURN NEW; END IF;
    -- Do not guess between legacy accounts sharing a normalized email.
    IF (SELECT count(*) FROM users WHERE normalize_complimentary_email(email) = invitation.recipient_email
        AND deleted_at IS NULL) <> 1 THEN RETURN NEW; END IF;

    granted_subscription := gen_random_uuid();
    INSERT INTO subscriptions (id, user_id, plan_code, status, provider, start_date, trial_used, created_at, updated_at)
    VALUES (granted_subscription, NEW.id, 'PLAN_PRO', 'ACTIVE', 'manual-testing',
        (now() AT TIME ZONE 'UTC')::date, true, now(), now());
    UPDATE complimentary_pro_grants SET status = 'ACTIVE', user_id = NEW.id,
        subscription_id = granted_subscription, activated_at = now() WHERE id = invitation.id;
    -- Plan checks use subscriptions, including after revocation; this is only the legacy cache.
    UPDATE users SET plan_type = 'PRO', updated_at = now() WHERE id = NEW.id;
    RETURN NEW;
END $$;

CREATE TRIGGER users_activate_complimentary_pro
AFTER INSERT OR UPDATE OF email, email_verified, email_verification_required, status, deleted_at ON users
FOR EACH ROW EXECUTE FUNCTION activate_verified_complimentary_pro();

CREATE FUNCTION protect_complimentary_pro_audit() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.recipient_email IS DISTINCT FROM OLD.recipient_email OR NEW.reason IS DISTINCT FROM OLD.reason
        OR NEW.created_at IS DISTINCT FROM OLD.created_at
        OR (OLD.status = 'REVOKED' AND NEW IS DISTINCT FROM OLD)
        OR (OLD.activated_at IS NOT NULL AND (NEW.activated_at IS DISTINCT FROM OLD.activated_at
            OR NEW.user_id IS DISTINCT FROM OLD.user_id OR NEW.subscription_id IS DISTINCT FROM OLD.subscription_id
            OR NEW.status = 'PENDING')) THEN
        RAISE EXCEPTION 'Complimentary grant audit is immutable; revoke instead of resetting';
    END IF;
    IF NEW.status = 'REVOKED' AND OLD.status <> 'REVOKED' THEN
        NEW.revoked_at := now();
        IF coalesce(length(trim(NEW.revoked_by)), 0) = 0 OR coalesce(length(trim(NEW.revocation_reason)), 0) = 0 THEN
            RAISE EXCEPTION 'Administrator identity and revocation reason are required';
        END IF;
        -- Revoke only this grant. Existing paid subscriptions and usage remain intact.
        UPDATE subscriptions SET status = 'CANCELLED', updated_at = now()
        WHERE id = OLD.subscription_id AND provider = 'manual-testing';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER complimentary_pro_audit
BEFORE UPDATE ON complimentary_pro_grants
FOR EACH ROW EXECUTE FUNCTION protect_complimentary_pro_audit();

INSERT INTO complimentary_pro_grants (recipient_email, reason)
VALUES ('s.mngaza07@gmail.com', 'Complimentary Pro access requested for Siphelele; valid until administrator revocation')
ON CONFLICT (recipient_email) DO NOTHING;

-- Activate an already verified existing account immediately. Otherwise the invitation
-- remains pending until the normal verification transaction updates the users row.
UPDATE users SET email = email
WHERE normalize_complimentary_email(email) = 's.mngaza07@gmail.com' AND email_verified
    AND NOT email_verification_required AND status = 'ACTIVE' AND deleted_at IS NULL;
