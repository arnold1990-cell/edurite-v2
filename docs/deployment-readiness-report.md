# EduRite development and deployment readiness — 15 October 2026

Repository-side corrections and local verification are implemented. Production release
is **blocked on external evidence and configuration checks** listed below. No production
deployment, production migration, registry publication, payment change or invitation
change was performed. Changes remain uncommitted for review.

## A. Completed development

### Resumed baseline and existing features

The working tree was clean before work. Local HEAD and remote main matched
`443965b207a841c24ec58bc648549028cec9e419`; remote branch/tag inspection found main
and no release tags. GitHub's public releases API returned no published releases.
Private drafts and authenticated Actions history were unavailable. Thus there was
no visible separate release candidate to compare; main already contains the October
14 fixes and migrations V75–V79.

The October 14 commits already implemented the main student features. They were
retained, rather than rebuilt:

- Career catalogue search/selection, unsupported-career rejection, saved learner
  choices and roadmaps, structured programme/institution records, programme-specific
  APS comparisons and explicit unavailable/unverified evidence.
- Career Match based on saved academics, interests/preferences and assessment
  evidence, explanations and learner-owned assessment history.
- Persisted study-plan generations, request-key deduplication, history and completion
  progress, using the existing roadmap storage and AI provider orchestrator.
- Tutor subject/grade context, provider responses, conversation persistence,
  authenticated ownership checks, entitlement and usage enforcement.
- Academic profile/results persistence, APS calculation, saved profile associations
  and school relationships, and cross-user access checks.
- Free Basic after Premium trial expiry, paid-access preservation and pending
  complimentary PRO invitation activation/revocation.
- SES API email verification using default AWS credentials, hashed expiring single-use
  tokens, resend throttling and safe provider-failure handling.

### New corrections

1. Removed unconditional startup `Flyway.repair()`. Normal migration validation now
   fails on checksum/history conflicts instead of rewriting history. A regression
   test verifies that migration failure cannot trigger repair.
2. Study-plan generation now requires a saved grade and at least one usable subject
   mark/achievement level. Missing information produces an actionable error before
   any provider call or persistence. Previously saved plans remain reopenable.
3. Added an Academic Results link next to study-plan prerequisites.
4. Career programme selection now handles legacy null/blank/JSON-null preferences;
   invalid non-object preferences still fail without overwriting saved data.
5. Added the deployed `.co.za` origins to existing CORS defaults in application and
   Compose configuration. Existing environment overrides continue to apply.
6. Added HTTP/database journey coverage for registration, SES acceptance, blocked
   unverified login, hashed token storage, resend throttling, verification/reuse
   rejection, login, academic updates/relogin, Career Match, catalogue selection,
   roadmap generation/save, study-plan retry/progress/history and tutor messages.
7. CI now runs frontend tests/encoding checks, supplies all isolated database-test
   URLs and rejects missing/failed/unexpectedly skipped backend suites. Only the
   explicitly opt-in live-provider suite may skip, with visible reporting.
8. CI builds both application images, publishes main images to GHCR with commit
   tags and revision labels, and records immutable digests in a release artifact.
   The backend image packages the JAR from successful Maven verification.
9. Replaced automatic `workflow_run` deployment, destructive checkout reset and EC2
   builds with a manual production-environment workflow. It verifies main ancestry,
   exact CI success/provenance and matching release artifacts before SSH staging.
10. Added a production Compose override, persistent upload storage requirement,
    configuration/SES/domain checks, exact image revision checks, backup evidence
    gates, sequential backend/frontend readiness, PostgreSQL SQL and Redis checks,
    protected-file hashes and deployment result records.
11. Added explicit application-image rollback from recorded immutable image IDs,
    requiring schema compatibility review and unchanged environment/Compose hashes.
    No automatic database/schema rollback exists.
12. Added a separate restore validator that creates a new private cluster, restores
    a V74 dump, migrates to V79, checks repeat migration is a no-op and compares
    pre-existing public-table rows. Archive creation time, dump hash, release commit
    and restore evidence are checked before production deployment.

## B. Modified and added files

Paths are relative to the repository root.

| File | Purpose |
| --- | --- |
| `.github/workflows/ci.yml` | Required frontend/backend/database verification, report gate, verified JAR artifact and immutable GHCR image publication. |
| `.github/workflows/deploy.yml` | Manual protected release, SHA/CI provenance, pinned SSH host trust and non-destructive staging. |
| `.gitignore` | Ignore Python bytecode created by verification tools. |
| `backend/.dockerignore` | Permit the verified JAR while excluding other target files. |
| `backend/Dockerfile` | Retain local source builds; add prebuilt verified-JAR mode. |
| `backend/src/main/java/com/edurite/config/FlywayRuntimeConfig.java` | Remove startup repair; migrate/validate safely. |
| `backend/src/main/java/com/edurite/roadmap/service/CareerSelectionService.java` | Parse empty legacy preferences safely. |
| `backend/src/main/java/com/edurite/roadmap/service/StudyPlanService.java` | Reject insufficient saved academic context before generation. |
| `backend/src/main/resources/application.yml` | Include deployed domain in default CORS origins. |
| `backend/src/main/resources/application-prod.yml` | Include deployed domain in production CORS defaults. |
| `backend/src/test/java/com/edurite/config/DevProfileDatasourceIntegrationTest.java` | HTTP learner journey with real persistence and mocked SES/AI boundaries. |
| `backend/src/test/java/com/edurite/config/FlywayRuntimeConfigTest.java` | Reject repair on migration/validation failure. |
| `backend/src/test/java/com/edurite/roadmap/CareerEvidenceTest.java` | Legacy empty-preference programme regression. |
| `backend/src/test/java/com/edurite/roadmap/StudyPlanServiceTest.java` | Missing grade/results and invalid marks must not generate a plan. |
| `backend/src/test/java/com/edurite/roadmap/StudyPlanPersistenceIntegrationTest.java` | Persisted academic prerequisites in isolated plan fixture. |
| `deploy.sh` | Delegate to controlled release tool instead of pulling/building on production. |
| `docker-compose.yml` | Add deployed CORS origins while retaining developer builds and existing data services. |
| `docker-compose.production.yml` | Require immutable application images and pre-existing persistent document bind storage. |
| `frontend/src/components/student/career/StudyPlans.tsx` | Explain saved academic prerequisites and link to Academic Results. |
| `scripts/check-test-reports.py` | Fail on missing required suites, failures and unexpected skips. |
| `scripts/release.py` | Deploy/rollback prebuilt images with data/configuration and readiness gates. |
| `scripts/RestoreValidation.java` | Flyway upgrade/repeatability and existing-row comparisons on restored copy. |
| `scripts/validate-restore.py` | Private cluster lifecycle, restoration, archive freshness and evidence generation. |
| `scripts/tests/test_release.py` | Deployment, rollback, backup, uploads and CI-report safety tests. |
| `docs/development-resume-status.md` | Initial inspection/status findings. |
| `docs/production-release-runbook.md` | Exact preparation, backup/restore, deployment and rollback procedure. |
| `docs/deployment-readiness-report.md` | This report. |

No V75–V79 SQL, invitation recipient, production secret, payment record, existing
uncommitted file, production checkout or production volume was overwritten. The
generated tracked TypeScript build-info change was restored to its original bytes.

## C. Executed verification

| Verification | Result and limits |
| --- | --- |
| Backend Maven verification with isolated PostgreSQL | 408 reported tests: **406 passed, 0 failures, 0 errors, 2 skipped**. Final report gate accepts only the two opt-in live AI skips. |
| Backend coverage | Authentication/security, SES service and HTTP flow, subscriptions/trials/entitlements, complimentary invitation triggers/concurrency, academic profile persistence, roadmap evidence, study-plan persistence/deduplication, tutor behavior and cross-user isolation. |
| Frontend `npm run verify` | **139 tests passed across 17 files**; UTF-8 validation, TypeScript and production build passed. Existing bundle-size and Browserslist warnings remain. |
| Deployment/report tooling | **9 Python tests passed**, including failed-backend ordering, image rollback, backup freshness/hash/provenance, missing/skipped test gates and unmounted document protection. |
| Migration tests | V1–V79 applied and validated in isolated schemas; V75 payment JSONB extraction and V76 activation/revocation/concurrency fixtures passed. |
| Restore validator | Real custom-format dump/restore of a **synthetic V74 fixture**, V75–V79 upgrade, repeat migrate no-op and row-preservation comparison passed. This is not a production-data restore. |
| Docker frontend | Image built; Nginx served 2 checked routes and 8 assets successfully. |
| Docker backend | Prebuilt verified-JAR image built; Java 21 and curl runtime checks passed. The initial source-build attempt failed on this machine's container JVM trust of Maven Central; no TLS checks were disabled. |
| Isolated container integration | PostgreSQL **16.13** reached V79; backend readiness returned UP, Nginx-to-backend readiness returned UP, Redis returned PONG. Synthetic configuration only, with outbound access blocked by an internal Docker network. |
| Compose/workflows | YAML parsed; production Compose resolved against synthetic configuration and retained service names/PostgreSQL volume while selecting immutable images and explicit upload storage. GitHub Actions execution itself was not performed. |

The HTTP journey mocks external SES/AI responses and uses synthetic accounts and an
isolated subscription fixture. It does not establish actual email delivery, model
quality, public TLS behavior or a complete interactive browser journey. Existing
assessment history/isolation tests cover the assessment branch separately.

Earlier test runs exposed new validation-order and legacy JSON-null regression
failures; these were corrected before the successful final suite. An initial
synthetic container startup correctly refused missing Gemini configuration; the
isolated smoke configuration supplied a nonfunctional synthetic key and made no
live provider request. Production preflight now checks that requirement first.

## D. Database safety

| Migration | Inspection and validation |
| --- | --- |
| V75 | Changes Basic plan display/interval to free; adds checkout payload and extracts JSONB response without removing metadata/payments. Historical trial timestamps are untouched. JSONB fixture and migration application passed. |
| V76 | Adds normalized unique invitations, user/subscription references, status/audit constraints, activation row locks and audit/revocation triggers. Activation requires an eligible verified active unique account. Repeated activation is prevented; revocation only cancels the linked `manual-testing` subscription. The existing authorized recipient is unchanged. |
| V77 | Adds nullable APS rule/qualification metadata; no invented admissions data. |
| V78 | Adds study-plan history/progress metadata and partial unique learner/request-key index; permits unknown learner APS to remain null. Existing roadmap rows retained. |
| V79 | Adds nullable programme admission context and a bounded nullable admission-year constraint. |

The released migrations use normal ordered Flyway execution. They are not manually
repeatable SQL scripts; Flyway's second migration invocation is the supported no-op.
Startup repair has been removed, so checksum/schema-history disagreements stop release.
Restore testing and migration fixtures validate syntax/constraints on synthetic data.

**Actual production backup freshness, restoration and existing-data preservation have
not been verified.** No production dump or access was supplied. Compatibility with
the production V74 history, legacy rows and any drift remains a release gate. A
successful fresh schema or synthetic restore cannot prove those properties.

The original Compose setup also lacks persistent document storage. Current production
mounts and uploaded files must be inspected; unmounted documents require a verified
maintenance migration before application replacement. PostgreSQL and Redis are not
recreated by the release tool.

## E. AWS and release blockers

| Area | Current state |
| --- | --- |
| Local images | Backend and frontend built locally; registry-ready workflow implemented. |
| Registry | No images published by this session. GHCR package permission and EC2 pull authentication remain to be verified. |
| GitHub Actions | Workflow code/Compose parsed locally; new workflow has not run on GitHub. Production environment reviewer/branch settings require repository administration. |
| AWS connectivity | No authenticated EC2/IAM/SES checks or deployment were performed. |
| SES configuration | Repository retains default-provider/EC2-role architecture and specified region, sender and verification base URL. Tests mock SES. Actual sender verification, sandbox status, IAM/IMDS access and delivery remain unverified. |
| Production SES reconciliation | The locally modified production files/backups were unavailable; semantic comparison remains outstanding. The release refuses an unreviewed production file hash. |
| Admission records | Existing structured evidence is retained. Missing/unverified institutional requirements remain labeled unavailable; no thresholds/eligibility were invented. Actual production catalogue completeness is unverified. |
| Live learner journey | HTTP/storage path tested with provider doubles. Isolated live SES/AI and authenticated browser validation remain outstanding. |

The remaining release gates are: reviewed production SES differences; a fresh actual
production dump successfully restored/migrated in isolation; persistent uploaded
documents; successful GitHub CI/image publication for the committed changes; protected
production environment and SSH host trust; registry access; and live AWS/domain/provider
checks. No claim of production readiness or deployment is made until these pass.

## F. Production release procedure

Use [the production release runbook](production-release-runbook.md) for the complete
commands and verification order. After all gates pass, the controlled entry point is:

```bash
gh workflow run deploy.yml --ref main \
  -f release_sha="$RELEASE_SHA" -f ci_run_id="$CI_RUN_ID"
```

The selected SHA must contain these reviewed changes and have a successful publishing
CI run. The runbook supplies actual backup/restore-validation commands, production
file-review evidence, manual host equivalent, public-domain checks and application-only
rollback. It does not authorize an untested production migration or database rollback.
