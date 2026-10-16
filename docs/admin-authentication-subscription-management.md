# Admin authentication and subscription management

## Authentication investigation

Read-only inspection of the native local PostgreSQL database found
`admin@edurite.com`: ACTIVE, verified, not deleted, no outstanding email
verification requirement, ROLE_ADMIN, and a 60-character BCrypt hash. A private
BCrypt comparison confirmed that the configured EDURITE_ADMIN_PASSWORD matches
that hash. Neither the password nor hash was printed. This does not establish
what credential was entered into the reported failing portal, or the state of a
production account. Production was not accessed.

Live confirmation: the already-running local portal at http://localhost:5173
accepted the configured admin credential and returned ADMIN with an access token.
Neither was printed. That portal's Nginx currently routes to a different checkout,
C:\Users\user\edurite-ai-career-guidance-clean, using the Docker PostgreSQL
database at V66. The native edurite-v2 database is a separate database at V79.
Both local accounts have valid BCrypt credentials. This environment difference
must be kept in mind when reproducing a UI report; changing edurite-v2 source does
not replace another checkout's running containers.

The frontend sends the entered password unchanged to the existing shared auth
endpoint. AuthService uses PasswordEncoder.matches; SecurityConfig supplies
BCryptPasswordEncoder. No separate admin login controller or authentication
bypass was introduced.

Confirmed defects corrected:

- AdminSeedDataSeeder replaced an existing password whenever it differed from
  configuration. Startup provisioning now preserves existing credentials.
- The same seeder trimmed a newly configured password. Password whitespace is
  now preserved, while empty configuration is still rejected.
- Removed the built-in administrator password fallback from application.yml.
  Legacy auth seeding skips administrator provisioning without a configured
  secret and also preserves existing credentials.
- The development password-repair endpoint can no longer reset privileged
  administrator accounts. Recovery requires the normal verified recovery flow
  or the operator procedure below.
- SUPER_ADMIN resolves to the existing admin portal; no second dashboard exists.

These fixes prevent credential changes and role-routing defects. The particular
reported invalid-password event has not been attributed to a confirmed cause;
the current local configured credential is valid. No account password was reset
as part of implementation.

## Secure administrator recovery

Use the existing verified OTP recovery flow if the account has an appropriate
registered recovery phone. For accounts without that recovery channel, an
authorized operator can use scripts/ResetAdminPassword.java. Do not enable a
seeder to reset an existing account.

1. Obtain authorization for the specific account and environment. Production
   recovery must be explicitly authorized and recorded in the operations log.
2. Use a trusted interactive terminal. Supply database connection variables from
   the existing secret-management mechanism: SPRING_DATASOURCE_URL,
   SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD. Do not paste passwords
   into commands, application logs, tickets or Git. The script accepts only a
   loopback PostgreSQL URL, including an explicitly authorized SSH tunnel.
3. Generate a runtime classpath and run the source launcher from backend:

   ```powershell
   mvn -q dependency:build-classpath '-Dmdep.outputFile=target/admin-reset-classpath.txt'
   $resetClasspath = (Get-Content target/admin-reset-classpath.txt -Raw).Trim()
   java --source 21 -cp $resetClasspath ../scripts/ResetAdminPassword.java admin@edurite.com
   ```

4. Enter and confirm the new password at the hidden console prompts. There are
   no password command arguments or password environment variables. The script
   requires 12 characters minimum and 72 UTF-8 bytes maximum, BCrypt cost 12,
   exactly one active nondeleted ADMIN/SUPER_ADMIN, and a committed transaction.
   It does not activate an account or assign roles. It sets must_change_password
   so the existing change-password flow is required after login.
5. Verify login with the new credential, rotate it through the normal account
   flow, and record the recovery outcome without recording credentials. Existing
   sessions are not automatically revoked by this offline operation; use the
   incident/session controls appropriate to the environment if compromise is
   suspected.

The source launcher was compiled with --release 21 and exercised in a hidden
interactive console against a disposable recovery-fixture@example.test account.
The result was a 60-character BCrypt cost-12 hash, successful admin login, and
mustChangePassword=true. It was not used to reset a real administrator or run
against production.

## Subscription architecture

The existing subscriptions table remains the source of platform access. An
administrator assignment creates an ACTIVE subscription with provider
ADMIN_OVERRIDE and no payment reference, paid timestamp, renewal date or billing
agreement. The assignment remains active until replaced by another administrator
assignment. Only older admin overrides are cancelled during replacement.

StudentPlanAccessService selects an active admin override before paid plans,
testing grants, or a trial. This also applies to BASIC, so a downgrade cannot
silently fall back to a paid or trial entitlement. Existing Feature definitions
and AI allowances remain unchanged. User.planType is synchronized in the same
transaction. Existing auth/session and entitlement endpoints resolve the real
effective plan from persistence.

Payment processing continues to update the actual purchase and payment records.
Its activation loop skips admin overrides. It does not cancel or replace the
administrator's platform-access decision. Admin assignments never create a
payment, mark an unpaid purchase paid, or alter an existing billing agreement.

Source is exposed as ADMIN_OVERRIDE, PAYMENT or PROMOTION using the existing
provider metadata. Entitlements are checked server-side on requests. The learner
frontend polls entitlements every 30 seconds; the plan badge now uses that live
result, including Pro. A separate pending checkout cannot redirect an
admin-authorized learner away from granted access. The pending payment itself
remains available for legitimate billing actions.

## API, audit and UI

Both /api/admin/subscriptions and /api/v1/admin/subscriptions expose:

| Method | Path suffix | Purpose |
| --- | --- | --- |
| GET | / | Paginated registered users and effective subscriptions; search by full name, username or email |
| GET | /{userId} | Subscription details |
| GET | /{userId}/history | Paginated admin change history |
| POST | /{userId}/override | Validated BASIC/PREMIUM/PRO assignment and required reason |

SecurityConfig checks administrator authorities. The service independently loads
the active principal and verifies persisted ADMIN/SUPER_ADMIN roles, preventing a
forged or stale authority from authorizing an ordinary account. The actor is
derived from the principal, never the request. Request plans and reasons are
validated, pagination is bounded, deleted targets cannot receive assignments,
and a per-user database lock serializes changes with payment activation.

V80__admin_subscription_management.sql adds an immutable application audit entity,
admin_subscription_changes, with actor, target, previous/new plan, reason,
subscription and timestamps. Foreign keys and plan/reason checks validate the
stored history. A partial unique index permits one active override per account.
The existing audit_logs also receives ADMIN_SUBSCRIPTION_OVERRIDE entries, in the
same transaction. History, access and user-plan updates roll back together if a
write fails. No historical migration SQL was edited.

The existing /admin/subscriptions page replaces the unrelated company metrics
placeholder. The existing sidebar gains Subscription Management. The page shows
user, email, plan, source and status, with Change Plan, View Subscription and View
History. Its modal requires a reason, prevents submission while saving, displays
errors, reports success and refreshes data. Existing React Query, API client,
Button, Input and DataTable components are reused. Native dialog behavior supplies
modal focus and escape handling. No second admin portal was added.

## Files

- Authentication: AdminSeedDataSeeder.java, AuthDataSeeder.java, AuthService.java,
  CustomUserDetailsService.java, application.yml, frontend roleUtils.ts, and the
  operator recovery script.
- Backend: AdminSubscriptionController.java, AdminSubscriptionService.java,
  AdminSubscriptionChange.java, AdminSubscriptionChangeRepository.java,
  StudentPlanAccessService.java, SubscriptionService.java, EntitlementController.java,
  SecurityConfig.java, UserRepository.java, and V80.
- Frontend: ManageSubscriptionsPage.tsx, AdminPages.tsx (existing route export),
  adminSubscriptionService.ts, DashboardLayout.tsx and subscriptions/access.tsx.
- Tests: admin seed/auth tests, DevProfileDatasourceIntegrationTest,
  SubscriptionServiceTest, migration latest-version assertions, frontend role,
  API, page and pending-payment entitlement regression tests.
- The pre-existing uncommitted V77 reconciliation work was retained. Its
  regression test's latest-schema expectation advances to V80; V77 itself is
  unchanged.

## Validation commands

Backend checks use only the disposable admin_regression database at
127.0.0.1:55441. EDURITE_TEST_DB_*, EDURITE_TRIAL_TEST_DB_* and
EDURITE_ACADEMIC_TEST_DB_* point to that fixture database. Live AI tests remain
opt-in and are not required for these administrator features.

```text
backend: mvn -o verify
frontend: npm run typecheck
frontend: npm test
frontend: npm run build
python -m unittest discover -s scripts/tests
docker build -t edurite-admin-frontend:verification frontend
docker build --build-arg BUILD_MODE=prebuilt -t edurite-admin-backend:verification backend
```

The first test run found the unsupported SUPERSEDED status for retired overrides;
it was replaced with existing CANCELLED. A frontend regression fixture omitted
the real accessAllowed response field; the fixture was corrected. These initial
failures are retained in ignored verification logs, alongside later results.

### Final results

| Check | Result | Exit |
| --- | --- | --- |
| mvn -o verify | 424 discovered: 422 passed, 0 failures/errors, 2 opt-in live-AI tests skipped; packaged successfully | 0 |
| npm run typecheck | Passed; final production build also runs tsc -b | 0 |
| npm test | 150 passed across 20 files | 0 |
| npm run build | Passed | 0 |
| python -m unittest discover -s scripts/tests | 34 passed | 0 |
| Backend Docker build with BUILD_MODE=prebuilt | Passed using the verified Java 21 artifact | 0 |
| Frontend Docker build | Passed | 0 |
| Recovery source javac --release 21 | Passed | 0 |
| Hidden-console recovery on disposable admin | Successful reset and login; mandatory password change asserted | 0 |
| Docker runtime/restart HTTP check | Passed through the built frontend proxy | 0 |

The runtime check used only disposable synthetic accounts and PostgreSQL data.
It confirmed frontend HTTP 200, administrator login, a persisted PRO override,
100 AI interactions, HTTP 403 for a learner attempting an override, and the same
PRO access and seven history entries after restarting the backend and signing
in again. An existing synthetic paid billing agreement remained ACTIVE with the
same provider agreement identifier. The normal integration tests additionally
exercise every requested upgrade/downgrade, real BCrypt login rejection,
anonymous and forged-role denial, history/audit persistence, and complete rollback
after a deliberately failed audit write. Verified PayFast event tests protect
all three override plans from payment activation.

The first runtime wait used localhost for an IPv4-only Docker port mapping and
could not reach readiness. The check was rerun successfully using 127.0.0.1.
No browser provider was connected (browser inventory was empty), so interactive
browser clicks were unavailable; frontend rendering/API tests and real HTTP
checks must not be described as a completed browser workflow test.

Logs: backend/target/admin-final-verify.log,
frontend/admin-frontend-tests-final.log, frontend/admin-frontend-build-final.log,
backend/target/admin-docker-build.log, frontend/admin-docker-build.log,
build/admin-deployment-tests.log and build/admin-runtime-verification-final.log.
These are ignored local artifacts. Temporary verification containers/network are
removed after the checks; the existing user's containers remain running.

Existing warnings remain: large frontend chunks, stale Browserslist data, and a
Docker lint warning for the pre-existing VITE_YOUTUBE_API_KEY build argument.
No real YouTube key was supplied to the verification build.

## Deployment requirements

No commit, push or deployment was performed. Existing GitHub Actions workflows,
release/restore scripts, Compose configuration, OAuth build arguments and SES
configuration are preserved. The normal CI builds the tested Java 21 artifact
and frontend image, then publishes verified images for the existing deployment
workflow. Deployment concurrency and known-host SSH checks remain intact.

V80 makes this a DATABASE_MIGRATION release. Before authorizing production:

1. Review the changes and run CI on the reviewed commit. Confirm migration
   compatibility against an isolated production restore, not just synthetic data.
2. Obtain a recent genuine backup, successful isolated restore/migration and
   preservation evidence, and the protected production approval required by the
   existing release pipeline. Do not fabricate evidence or run repair/baseline.
3. Deploy through the existing digest-verified workflow, preserving production
   .env and existing billing/SES settings. Keep prod active and development
   seeders disabled. No production reset or manual grants are part of rollout.
4. Verify health, authorized admin login, role denials, and explicitly approved
   subscription changes with audit records. Observe learner entitlements after
   refresh/relogin. Billing agreements must remain separate.
5. Prepare a compatibility-reviewed rollback before enabling administrator
   overrides. Older application images do not understand override precedence and
   must not be blindly restored once overrides are active. Prefer a reviewed
   forward correction; the additive V80 schema itself need not be removed.

Production account diagnosis, production migration rehearsal, deployment and
browser interaction testing remain separate from local automated verification.

For local review, run the backend and frontend from edurite-v2. The backend's
normal Flyway startup applies V80 to its local database; use npm run dev with an
available frontend port (for example -- --port 5174 if 5173 belongs to the other
checkout). Its existing Vite API proxy targets 127.0.0.1:8080. Do not confuse that
frontend with the older running Docker portal.
