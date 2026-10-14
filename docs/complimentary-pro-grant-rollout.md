# Pending complimentary Pro invitation

Migration `V76__pending_complimentary_pro_grants.sql` records `s.mngaza07@gmail.com` as a server-only pending invitation. Matching trims leading/trailing whitespace and lowercases the address. It never matches names, aliases or Gmail plus addresses. A normalized address matching multiple non-deleted accounts is left pending for administrator review.

The database activation trigger runs in the transaction that inserts or updates the account's verification state. This covers the existing email-link verification service and verified Google registration/sign-in without duplicating authentication logic. The account must be active, not deleted, `email_verified=true` and `email_verification_required=false`. The migration also activates an already verified existing account immediately.

Activation locks the pending invitation and creates one non-expiring `PLAN_PRO` / `manual-testing` subscription using the existing complimentary-access mechanism. It records the recipient, reason, account, subscription and activation timestamp. No payment is fabricated, no paid subscription is replaced, and no usage record is inserted or reset. Existing entitlement checks provide every Pro feature and the normal 100-use monthly AI allowance minus existing usage. Successful payment synchronization preserves manual-testing grants.

Repeated verification cannot reactivate an active or revoked invitation. Verification and activation roll back together if the transaction fails. Revocation cancels only the linked complimentary subscription; an existing paid subscription continues to resolve normally. Historical activation and revocation details remain stored. Future email changes do not transfer or reset an activated grant.

## Production status

Production persistence and activation are **not confirmed**. The available `.env` points to localhost; the repository's production SSH host is held in GitHub Actions secrets. No production host/database connection has been supplied for this task. Local tests use isolated PostgreSQL schemas and are not production evidence.

The prior name-based grant script now handles only Arnold. Siphelele's grant is exclusively the email-verified invitation in V76.

## Deploy and persist the invitation

These commands assume the production checkout contains this change and the preceding V75 subscription changes, and its existing `.env` contains the production connection settings. Run on the production server from `/opt/edurite-v2`:

```bash
cd /opt/edurite-v2
docker compose build backend
docker compose up -d --no-deps backend
docker compose logs --tail=200 backend
curl --fail http://localhost:8080/actuator/health
docker compose exec -T postgres sh -c 'psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<'SQL'
SELECT version, description, success FROM flyway_schema_history WHERE version IN ('75', '76') ORDER BY installed_rank;
SQL
docker compose exec -T postgres sh -c 'psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < scripts/inspect-complimentary-pro.sql
```

Flyway applies V76 transactionally at backend startup. Keep Flyway enabled; do not manually run the migration and then start Flyway without recording it through Flyway. A successful build alone does not save the pending invitation.

The inspection must return exactly one row. Before verified registration expect `status=PENDING`, null user/subscription/activation fields and zero grant payments. If a verified account already exists, expect `status=ACTIVE`, a non-null activation timestamp, `PLAN_PRO`, active `manual-testing`, null end date and zero grant payments. Record this output as production evidence.

After the recipient registers and verifies normally, rerun the inspection. With that account's normally issued access token, also verify the runtime entitlement response:

```bash
curl --fail -H "Authorization: Bearer $ACCESS_TOKEN" "$BASE_URL/api/subscriptions/entitlements"
```

Set `BASE_URL` to the production HTTPS origin. Expect `plan=PRO` and the Pro entitlements. No email delivery or live authentication is fabricated by the migration.

## Administrator revocation

Run using an authorized database administrator connection; the SQL is not exposed as a public API. Supply the real administrator identity and reason:

```bash
docker compose exec -T postgres sh -c 'psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v recipient_email="$1" -v administrator="$2" -v reason="$3"' sh \
  's.mngaza07@gmail.com' 'ADMINISTRATOR_ID' 'REASON_FOR_REVOCATION' < scripts/revoke-complimentary-pro.sql
docker compose exec -T postgres sh -c 'psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < scripts/inspect-complimentary-pro.sql
```

Revocation is idempotent and also works before registration. Do not delete the invitation, reset its status or rerun an old name-based script to grant this recipient.

## Checks

On 2026-10-08, `mvn verify` passed: 355 tests run, zero failures/errors, 9 unrelated environment-gated tests skipped. All 8 PostgreSQL complimentary-grant integration tests ran and passed, including concurrent verification. No frontend code changed in this task, and the recipient email is absent from `frontend/src`.

Set these environment variables for a disposable local PostgreSQL database using a role that can create test schemas: `EDURITE_TRIAL_TEST_DB_URL=jdbc:postgresql://127.0.0.1:5432/TEST_DATABASE`, `EDURITE_TRIAL_TEST_DB_USER`, `EDURITE_TRIAL_TEST_DB_PASSWORD`. Then:

```bash
cd backend
mvn -Dtest=ComplimentaryProGrantIntegrationTest,StudentPlanAccessServiceTest,SubscriptionServiceTest,EmailVerificationServiceTest test
mvn verify
```

The grant integration tests cover pending/unverified registration, whitespace/case normalization, existing verified accounts, verified Google-style registration, unrelated emails, ambiguous legacy accounts, repeat and concurrent verification, rollback, all Pro features, unchanged used credits, existing paid subscriptions/payments and active/pending revocation. They create isolated schemas and remove them afterward. Without the database environment variables, these integration tests are skipped.
