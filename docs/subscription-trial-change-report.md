# Fourteen-day student free trial ? local change report

## Behaviour and calculation

The existing SubscriptionService, StudentPlanAccessService, EntitlementService, API interceptor and Student Portal are reused. No additional subscription system, dashboard or subscription page was created.

New student trials start at the persisted account creation timestamp, following the existing registration lifecycle (including Google student registration). The existing subscription record stores trial_start_date and trial_end_date. Expiry is start + 14 days (336 hours). Access is active only when start <= server UTC time < expiry. At the exact expiry instant access is denied. Resolution also caps any stored expiry at start + 14 days and honours an earlier recorded expiry. No browser date, plan claim or stored browser state is used for enforcement.

Trial users receive Basic features, not Premium. The existing portal shows Free Trial, server-calculated days remaining, the end date and Upgrade; the warning becomes amber at three days remaining. Expired protected routes show the end-of-trial message and View Plans. Profile/account management, settings, subscription management, notifications and logout remain accessible. Protected authenticated API requests are also denied after expiry. Existing anonymous public catalogue previews remain public under the existing architecture.

Valid active paid Premium/Pro records take precedence over expired trials and pending purchases. Expired/cancelled records and pending/failed payments cannot grant paid access. Existing payment verification is unchanged. The legacy email-only Premium override was removed. Learner information and subscription/payment history are not deleted. Basic checkout is rejected, preventing repeat free activation.

## Migration and existing users

Added V71__fourteen_day_basic_trial.sql; no previously applied migrations were edited and no new tables or columns were needed.

Existing free records are anchored to the earlier of account creation and earliest recorded trial start. Expiry is capped at that start + 336 hours; an earlier recorded expiry is preserved. Accounts older than 14 days normally become expired immediately when this migration is eventually applied. Eligible historical student accounts without any subscription receive a record anchored to their historical creation date, never migration time. Paid subscription and payment records are preserved. Initialization locks the account and reuses an existing subscription, and account creation remains the fallback anchor if a record is absent. Login/profile changes do not create a fresh window. The Basic price entry is relabelled Free Trial and is not purchasable as recurring free access.

The migration has been exercised only against a disposable local PostgreSQL database. Production has not been changed.

## Tests and verification

Added/updated tests cover exactly fourteen days, day 13, the instant before expiry, the exclusive expiry boundary, day 15, forged dates/plan claims, repeated initialization after profile/login metadata changes, Basic checkout rejection, Premium/Pro precedence, pending/failed/cancelled/expired paid records, direct Basic API denial, frontend upgrade states and management access. The PostgreSQL regression test executes the migration twice and checks historical backfill, earlier-expiry preservation, paid records, payments and saved learner data.

- Backend `mvn test -q`: passed after correcting an existing local-date assertion to use the UTC date used by payment activation.
- Final `mvn -f backend/pom.xml verify`: BUILD SUCCESS; 323 tests reported, 314 passed, 9 skipped, zero failures/errors.
- The nine existing conditional skips were eight DevProfileDatasourceIntegrationTest cases and one AiUsageConcurrencyTest. The full application/database integration and AI concurrency suites were not exercised.
- TrialMigrationTest: ran and passed against isolated PostgreSQL on localhost, not skipped. The test server was stopped afterward.
- Frontend `npm run verify`: passed encoding validation, TypeScript typecheck, 96 tests across 9 files, and production build.
- `git diff --check`: passed.
- Non-failing frontend warnings remain for outdated Browserslist data and a bundle larger than 500 kB.

## Files changed

Backend production:
- backend/src/main/java/com/edurite/subscription/controller/EntitlementController.java
- backend/src/main/java/com/edurite/subscription/entity/PlanType.java
- backend/src/main/java/com/edurite/subscription/service/EntitlementService.java
- backend/src/main/java/com/edurite/subscription/service/StudentAccessPolicy.java
- backend/src/main/java/com/edurite/subscription/service/StudentPlanAccessService.java
- backend/src/main/java/com/edurite/subscription/service/SubscriptionAccessInterceptor.java
- backend/src/main/java/com/edurite/subscription/service/SubscriptionService.java
- backend/src/main/resources/db/migration/V71__fourteen_day_basic_trial.sql

Backend tests:
- backend/src/test/java/com/edurite/subscription/EntitlementSecurityTest.java
- backend/src/test/java/com/edurite/subscription/StudentPlanAccessServiceTest.java
- backend/src/test/java/com/edurite/subscription/SubscriptionServiceTest.java
- backend/src/test/java/com/edurite/subscription/TrialMigrationTest.java

Frontend:
- frontend/src/features/subscriptions/access.tsx
- frontend/src/features/subscriptions/access.test.tsx
- frontend/src/pages/student/StudentPages.tsx

Documentation:
- docs/subscription-trial-change-report.md

No commit, push, deployment, AWS changes, production database changes or PayFast credential changes were performed.
