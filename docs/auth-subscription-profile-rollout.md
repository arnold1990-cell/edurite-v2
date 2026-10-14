# Authentication, subscriptions and academic profile rollout

The profile presentation is restored from commit `3894be8`, with the existing current data services, section URLs and editor retained. Section navigation is restored explicitly. Manual percentages use the existing NSC achievement levels (1–7); they do not change the learner's qualification or grade. Verified transcript results remain protected.

Email-verification-required accounts receive `403 EMAIL_VERIFICATION_REQUIRED` on password login; refresh/session and protected backend access remain blocked. The verification page supports SES resend and preserves the intended local destination across tabs. Verification returns to sign-in because no authenticated session is issued by the verification endpoint. Successful sign-in uses the authenticated role and clears previous query data. Existing legacy account verification state is preserved.

Pending purchases redirect students to Subscription & Payment. Only confirmed payments activate purchased access. PayFast resumption submits its stored signed fields by POST; migration V75 retains resumable fields from older checkout metadata. A legacy pending payment without those fields can be cancelled and restarted. Failed, cancelled and completed purchases are excluded from the pending list.

Eligible trials resolve to Premium until the exclusive recorded start + 14-day boundary (or an earlier recorded expiry). Every backend entitlement check uses current server time, so expiry does not depend on a login or a scheduled job. Expired users keep free Basic access; active paid subscriptions take precedence. Original trial dates and saved profiles are preserved.

Pro testing uses an active `PLAN_PRO` subscription with provider `manual-testing`. Entitlement resolution recognises the grant without a paid expiry date; successful payment synchronization does not cancel it. Explicitly changing that grant's status to CANCELLED revokes it.

## Verified account status

On 2026-10-08, the configured **local** PostgreSQL database contained one exact active match for Arnold Tyvern Madamombe, UUID `90f679b2-69a6-4a00-8225-85bfa469fce1`. The grant script committed an active Pro testing subscription, confirmed by a subsequent SQL result.

Neither Siphelele Mgadla nor the user-confirmed email `s.mngaza07@gmail.com` exists in that database. No duplicate account was created. The subsequent [complimentary invitation migration](complimentary-pro-grant-rollout.md) now handles this recipient exclusively by verified email, including future registration. The original testing script handles only Arnold. Production grants and production email/payment operation have not been verified.

## Deployment

Run from the deployed checkout containing these changes, using the existing production `.env`. Configure `APP_EMAIL_VERIFICATION_AWS_REGION`, `APP_EMAIL_VERIFICATION_FROM`, and `APP_EMAIL_VERIFICATION_BASE_URL` for the actual site. The backend needs its existing SES IAM permissions and a verified sender. Keep the existing PayFast credentials and externally reachable notification URL.

```bash
docker compose build backend frontend
docker compose up -d postgres redis backend frontend
docker compose logs --tail=150 backend
curl --fail http://localhost:8080/actuator/health
curl --fail http://localhost:5173/actuator/health
```

Flyway must successfully apply V75 before the new backend serves payments. Do not manually mark the migration applied. Grant the existing production testing accounts **after deploying the updated entitlement code**:

```bash
docker compose exec -T postgres sh -c 'psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < scripts/grant-pro-testing.sql
```

Read the script's GRANTED/NOT GRANTED notices. A successful script exit alone does not establish that both accounts matched. It is safe to rerun without duplicating active testing grants.

## Reproduce checks

Final local results on 2026-10-08: frontend encoding, TypeScript, 115 tests and production build passed. Backend `mvn verify` passed with 347 tests run, zero failures/errors and 9 skipped (8 environment-gated datasource tests and 1 AI usage concurrency test). The PostgreSQL migration test was explicitly enabled and passed. Build warnings remain for outdated Browserslist data and a frontend bundle exceeding 500 kB. No production deployment or live SES/PayFast transaction was performed.

```bash
cd frontend
npm ci
npm run verify
cd ../backend
mvn verify
```

To enable the PostgreSQL migration regression, supply `EDURITE_TRIAL_TEST_DB_URL=jdbc:postgresql://127.0.0.1:5432/<test_database>`, `EDURITE_TRIAL_TEST_DB_USER` and `EDURITE_TRIAL_TEST_DB_PASSWORD`, then run `mvn -Dtest=TrialMigrationTest test`. It uses temporary tables and rolls its fixtures back. Never put database passwords into committed files or command history.

## Runtime verification after deployment

Sign in normally as each testing account, then use its access token with these requests (set `BASE_URL` to the site's HTTPS origin and `ACCESS_TOKEN` to that account's token):

```bash
curl --fail -H "Authorization: Bearer $ACCESS_TOKEN" "$BASE_URL/api/auth/me"
curl --fail -H "Authorization: Bearer $ACCESS_TOKEN" "$BASE_URL/api/subscriptions/entitlements"
curl --fail -H "Authorization: Bearer $ACCESS_TOKEN" "$BASE_URL/api/subscriptions/pending-payments"
```

Confirm `plan: PRO` for each testing account. Also check these flows in the deployed browser:

1. Sign in as an email-verification-required account: expect the verification page, working resend delivery, and denial of protected API access. Open the email link, verify and sign in; confirm the original role-authorized destination including query parameters.
2. Sign out of one role and sign into another; confirm the correct dashboard and no previous profile data, including after page refresh and in a second tab.
3. Create a sandbox pending purchase, leave checkout and sign in again. Confirm the payment warning and signed PayFast continuation. Verify failed/cancelled states do not grant the purchased tier; only a verified completed notification does.
4. On controlled staging fixtures, verify Premium just before trial expiry and Basic at/after expiry; an active paid subscription or testing grant must remain effective. Repeated login must not change trial dates.
5. Save manual marks at NSC boundaries, reload and confirm derived levels persist while qualification and grade remain unchanged. Exercise the restored profile sections, documents and saved versions.

SES delivery, a real provider checkout and these authenticated production flows still require runtime evidence; unit tests do not establish production success.
