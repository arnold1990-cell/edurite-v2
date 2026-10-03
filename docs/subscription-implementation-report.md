# EduRite subscription implementation

Audit and verification date: 2026-10-03.

## Existing architecture and implementation approach

EduRite already has `User`, persisted roles, JWT authentication, `SubscriptionRecord`, `PricingPlan`, `PaymentRecord`, `PaymentEvent`, a payment-provider factory, and PayFast/PayPal adapters. The Student Portal uses the existing authenticated `StudentAppShell`, consolidated section routes, and `StudentSubscriptionPage`.

The subscription implementation was already partly committed when this task resumed. This continuation reuses that architecture rather than creating another dashboard, subscription page, or billing system:

- `StudentPlanAccessService` resolves the effective plan from stored subscriptions, status, UTC start/end dates, and existing trial rules. Cached `User.planType`, request parameters, JWT plan claims and browser state do not grant entitlements.
- `Feature` is the server-owned feature catalogue. `EntitlementService` resolves grants. `StudentAccessPolicy` defines protected API aliases and browser section rules.
- `SubscriptionAccessInterceptor` enforces features before controller execution. Existing authentication and role checks still apply independently.
- `SubscriptionResponseAdvice` limits Basic catalogue results and removes Pro-only roadmap/progress fields before responses leave the backend. It does not modify saved records.
- `/api/subscriptions/entitlements` and `/api/v1/subscriptions/entitlements` provide plan, status, trial state, feature catalogue, granted entitlements, route rules, AI usage, billing interval, expiry, renewal and cancellation-at-period-end state. Existing `/subscriptions/me` and `/subscriptions/plans` remain available.

Existing Flyway migrations include the subscription/payment schema, trial columns (including V36), and V70 for entitlement pricing and usage. V70 was already present on resumption. No applied migration was edited and no additional migration was needed in this continuation.

## Exact feature catalogue

Premium inherits every Basic feature. Pro inherits every Premium feature.

| Plan | Price | Added entitlement IDs | Monthly AI allowance |
| --- | --- | --- | --- |
| Basic | R0/month | `PROFILE`, `CAREER_BASIC`, `APS_BASIC`, `INSTITUTION_SEARCH_LIMITED`, `BURSARIES_BASIC`, `PROGRESS_BASIC`, `LEARNING_RESOURCES_LIMITED`, `AI_SUPPORT` | 5 |
| Premium | R59/month or R599/year | `CAREER_FULL`, `CAREER_ASSESSMENT`, `PERSONALISED_CAREER_RECOMMENDATIONS`, `SUBJECT_CAREER_MATCHING`, `APS_FULL`, `INSTITUTION_SEARCH_FULL`, `BURSARIES_FULL`, `SAVE_OPPORTUNITIES`, `CAREER_ROADMAP_PERSONALISED`, `LEARNING_RESOURCES_FULL`, `STUDY_RECOMMENDATIONS`, `PROGRESS_FULL` | 30 |
| Pro | R119/month or R1,199/year | `ADVANCED_MATCHING`, `PRIORITY_OPPORTUNITY_MATCHING`, `CAREER_ROADMAP_ADVANCED`, `ACADEMIC_ANALYSIS`, `AI_OPPORTUNITY_ASSISTANT`, `APPLICATION_SUPPORT`, `PROGRESS_ADVANCED`, `EARLY_ACCESS`, `PARENT_GUARDIAN_INSIGHTS` | 100 |

Basic search previews return at most five results per response for careers, courses, institutions, university programmes/requirements, learning resources and the career roadmap catalogue. This is a per-search preview limit, not a lifetime limit on distinct careers viewed. Basic progress returns at most two cards. Manual/profile APS calculation remains available; personalised programme readiness is part of paid roadmap guidance. Premium roadmaps omit the advanced gap analysis and AI study plan. Detailed progress recommendations are Pro-only.

Parent/guardian functionality remains “when available”; the entitlement does not create a separate parent portal. Early access is a grant available for future features, not a new feature created by this task.

## Backend protection

Every path below is enforced for both `/api` and `/api/v1` aliases. An authenticated student lacking the feature receives HTTP 403 through the existing error handler.

| Endpoint family | Required access |
| --- | --- |
| `/student/psychometric/**`, legacy `/public/psychometric/**` | Premium assessment; legacy public alias requires authentication |
| `/student/career-roadmaps/generate`, `/saved`, `/save` | Premium personalised roadmap |
| `/student/career-roadmaps/requirements` | Premium study recommendations |
| `/recommendations/**`, `/ai/career-advice`, `/ai/career-advice/me` | Premium personalised recommendations |
| `/student/careers/*/save`, `/student/bursaries/*/save`, `/student/opportunities/*/*/save`, career/bursary saved and bookmark lists | Premium saving |
| `/student/learning-centre/recommended`, `/learning-centre/recommended` | Premium learning resources |
| `/student/university-applications/**`, `/student/scholarship-applications/**`, `/student/cv/**`, `/applications/**`, POST `/bursaries/*/applications` | Pro application support |
| POST `/student/scholarship-applications/*/motivation-letter`, GET `/student/cv/ai-suggestions` | Pro AI opportunity assistant |
| `/bursaries/recommendations/**`, `/ai/bursary-guidance/me` | Pro priority matching |
| POST `/ai/analyse-university-sources` | Pro advanced matching |
| GET `/ai/dashboard-summary` | Pro advanced insights |
| POST `/student/tutor/ask`, GET `/ai/gemini-health` | Basic AI access, subject to quota |

Ordinary career/APS/bursary browsing remains accessible. Anonymous public institution and learning catalogue responses cannot obtain the full result set simply by omitting the JWT. Non-student staff accounts, including mixed student/staff role accounts, are exempt from student billing restrictions; their existing role authorization is unchanged.

## Payments, plan changes and existing accounts

PayFast activates subscriptions only after signature verification, merchant matching, PayFast server-to-server validation, payment-provider/reference matching and expected-amount verification. The service updates the persisted payment and subscription records. A browser return/confirmation URL cannot establish PayFast payment success. The frontend polls payment status and refreshes entitlements after the backend confirms activation.

Checkout is serialized per account. Same active tier purchases and duplicate pending purchases of the same plan are rejected. An existing Premium trial can convert to paid Premium. Pending or failed upgrades do not replace active access. Activation cancels the superseded active subscription and sets the purchased monthly/yearly period. Basic downgrades take effect immediately; paid tier changes take effect after verified activation. No saved learner data is deleted.

Cancelled, expired, pending, failed and past-due paid records do not grant paid features. `cancelAtPeriodEnd` on an otherwise active subscription retains access until its end date. Unknown plan codes and future paid periods fail closed to Basic. Legacy trial dates cannot resurrect an expired paid Pro/Premium record.

Two pre-existing business exceptions are preserved explicitly:

- The existing one-time Premium trial remains supported for accounts initialized through that flow. Trials grant Premium, never Pro.
- The existing server-side owner email override remains a permanent Premium benefit. This continuation fixes it so a valid paid Pro subscription is not overwritten as Premium. Ordinary students cannot obtain this benefit through client-side plan manipulation.

## AI usage

V70 creates `student_ai_usage`, indexed by user, period and status. Allowances are 5/30/100 for the effective plan. Periods are UTC calendar months and reset by querying the current month, without a reset job. Upgrades retain usage already consumed in the same month.

Before counted controller execution, `AiUsageService.reserve` locks the authenticated user's database row and atomically checks used plus reserved slots. One SQL aggregate reads both counts from the same snapshot, avoiding a race with finishing requests. Exhaustion returns HTTP 429 before executing the AI operation. Successful responses commit the reservation; failed/non-2xx/unavailable responses release it. Completion updates are idempotent. Response headers and the entitlement endpoint expose updated usage.

Counted operations include tutor questions, career advice, roadmap generation, advanced university analysis, dashboard AI summaries, CV suggestions, motivation letters and the provider health request that invokes AI. Non-AI catalogue and recommendation reads are not charged. Reservations left by a process crash remain conservatively reserved for that month; they cannot cause quota overrun.

## Frontend

The existing subscription page has the feature overview and three orange/blue/purple plan cards, yearly prices, Premium “MOST POPULAR”, billing selection, active-plan state, duplicate-purchase disabling and downgrade information. The reference attachment was unavailable in this resumed conversation; implementation follows the written requirements, without a pixel-match claim.

`StudentRouteAccess`, `RequireEntitlement`, `FeatureLock` and `AccessBadge` consume the backend feature catalogue and route rules. Locked sections display View Plans and Not Now; entitlement loading/errors fail closed while account management remains reachable. Legacy routes continue through existing redirects into guarded sections.

Premium guidance uses `/ai/career-advice/me`; advanced university analysis remains Pro-only. Career explorer actions show upgrade prompts, avoid fetching inaccessible saved/assessment data, clear generated guidance on plan changes, and lock advanced study-plan content. Tutor and roadmap screens expose usage; tutor quota errors are visible. Backend enforcement remains authoritative if browser guards are bypassed.

## Files changed in this continuation

Backend, under `backend/src/main/java/com/edurite/subscription/`:

- `controller/EntitlementController.java`
- `entity/PlanType.java`
- `service/AiUsageService.java`
- `service/EntitlementService.java`
- `service/StudentPlanAccessService.java`
- `service/SubscriptionResponseAdvice.java`
- `service/SubscriptionService.java`

Backend tests, under `backend/src/test/java/com/edurite/subscription/`:

- `EntitlementSecurityTest.java`
- `StudentPlanAccessServiceTest.java`
- `SubscriptionResponseAdviceTest.java` (new)
- `SubscriptionServiceTest.java`: preserved the pre-existing uncommitted changes and added same-plan rejection, active-plan preservation and verified yearly Pro activation regressions.
- `AiUsageConcurrencyTest.java`: supports an explicitly configured test PostgreSQL database as well as Testcontainers, isolates each case in its own temporary schema, and checks all three quotas while requests finish concurrently.
- `../config/DevProfileDatasourceIntegrationTest.java`: verifies persisted subscription changes, cached-plan forgery rejection, actual protected controllers and quota denial through the full Spring application. Performance-test user seeding is disabled for this test context.

Frontend, under `frontend/src/`:

- `components/student/career/ExploreWorkspace.tsx`
- `features/subscriptions/access.tsx`
- `features/subscriptions/access.test.tsx` (new)
- `pages/student/StudentCareerRoadmapsExplorerPage.tsx`
- `pages/student/StudentFeaturePages.tsx`
- `pages/student/StudentPages.tsx`
- `pages/student/StudentSectionPages.test.tsx`
- `services/aiGuidanceService.ts`

The existing committed implementation additionally includes `Feature`, `StudentAccessPolicy`, `SubscriptionAccessInterceptor`, `SubscriptionWebConfig`, V70, subscription CSS, payment-provider hardening and `AiUsageConcurrencyTest`.

## Verification and operational follow-up

Security tests exercise the plan matrix, direct API denial with forged plan parameters, Premium-to-Pro denial, expiry/cancellation, missing subscriptions, quotas and UTC month boundaries. Payment tests cover valid/invalid ITNs, amount mismatch, same-plan purchase checks, verified activation and pending upgrades. New tests add invalid/future plan handling, stale trial rejection, mixed staff roles, owner Pro precedence, anonymous catalogue limits, saved-data preservation, Pro field redaction, unavailable AI responses and direct frontend URL locks.

Frontend: TypeScript, repository lint script, 94 tests across 9 files, and production build pass. The lint script currently delegates to TypeScript; there is no separate ESLint run. Build warnings concern stale Browserslist metadata and the existing large application bundle.

The earlier `mvn verify` passed with eight environment-dependent skips. Continuation verification removed those skips by initializing an isolated PostgreSQL 17 cluster under ignored `build/subscription-test-pg`, listening only on `127.0.0.1:55439`, and creating a dedicated `edurite_subscription_test` database. All 70 migrations, including V70, applied successfully to that fresh database.

Database-backed verification uses the existing `EDURITE_TEST_DB_URL`, `EDURITE_TEST_DB_USERNAME` and `EDURITE_TEST_DB_PASSWORD` variables. Point these only at a disposable test database; the full application integration test applies migrations and inserts fixtures. `AiUsageConcurrencyTest` now supports the same variables and removes only its own randomly named temporary schemas after each case. Docker remains supported when these variables are absent.

Final database-backed `mvn verify`: **BUILD SUCCESS; 312 tests passed, zero failures/errors, zero skipped**. Results are recorded in `backend/subscription-database-verify.log`. The database integration test exercises Basic, Premium, Pro and expired access using persisted records and the real Spring Security/interceptor/controller stack. The quota tests cover all three allowances, failure release, UTC month transition, and concurrent completion/admission. No production database or existing local application database was used. The isolated test server was stopped after verification; its files remain under ignored `build/`.

Deployment follow-up: apply the existing V70 migration through normal Flyway deployment if it is not already applied; retain real PayFast merchant credentials/passphrase and the existing reachable HTTPS `.co.za` ITN URL; perform a PayFast sandbox payment/ITN smoke test. No live payment was initiated, no production database was changed, and deployment/domain/OAuth/Nginx/Docker networking configuration was not modified. Browser visual comparison against the missing attachment remains unverified.
