# Repository audit and commit-readiness report

Audit date: 2026-10-19. Baseline HEAD: `da1c63d` (`fixing career guidance page`).

## Decision

**The current repository is not ready for production deployment.** Local compilation, relevant tests, frontend assets and backend packaging pass, but the latest commit is an incomplete source snapshot and live integration/release checks remain outstanding.

No commit, push, deployment, staging operation, production application startup or production database operation was performed. No database schema, migration, AWS, Docker or workflow changes were made. Existing working files were retained.

## Git and conflict findings

- At audit start: zero tracked modifications, zero staged changes, and 37 untracked files. Local Git reported `main` one commit ahead of its existing `origin/main` tracking reference; no remote fetch or push was performed.
- `git ls-files -u` and `git diff --name-only --diff-filter=U` returned no unresolved files. No conflict markers were found in source, documentation or workflows. No `.rej`/`.orig` patch leftovers were found outside generated/dependency directories.
- **Commit completeness blocker:** tracked code already imports untracked dependencies. Examples: `StudentCareerMatchPage.tsx` imports the untracked report view, illustrations, model, hook and stylesheet; `aiGuidanceService.ts` imports untracked `types/careerResearch.ts`; `MultiUniversityPageFetcherService.java` depends on untracked `OfficialPageAccessPolicy.java`.
- A clean checkout of HEAD therefore lacks required sources even though this local working directory builds. Include the complete feature dependency/test set together when manually preparing a commit. Do not commit only selected tests, omit source dependencies, or deploy HEAD as it stands.
- This audit adds one report, bringing the untracked total to 38. It does not stage them. Generated `frontend/tsconfig.tsbuildinfo` was restored to its clean baseline after validation; build outputs remain generated artifacts.

## Correction made during audit

`AcademicIntelligenceService` accepted blank curriculum, grading-scale and examination metadata when comparing historical marks. Blank metadata does not establish comparable academic contexts. The comparison now requires nonblank subject/context fields. A focused regression test was added in `AcademicAndEligibilityTest`; valid existing comparisons remain supported.

No conflict resolution or removal of working code was necessary. No compilation failures were found in the audited local source set.

## Validation

| Check | Result |
| --- | --- |
| Frontend lint / TypeScript (`npm run lint`, which runs `tsc --noEmit`) | Passed, exit 0 |
| Frontend UTF-8 validation | Passed, exit 0 |
| All frontend tests | **219 passed across 28 files**, exit 0 |
| Frontend production build (`tsc -b` and Vite) | Passed, exit 0 |
| Static distribution asset check | Passed; 8 referenced assets verified |
| Locally served build check | Passed; 2 script-defined routes and 8 assets |
| Additional local career route checks | HTTP 200 application shell for Career Guidance, Career Match, Career Explorer and Study Options research view |
| Relevant backend suites | **117 passed**, zero failures/errors/skips; Maven exit 0, BUILD SUCCESS |
| Backend executable JAR packaging | Passed, exit 0, BUILD SUCCESS |
| Git whitespace check and explicit scan of all 38 untracked files | Passed; no trailing whitespace or missing final newlines |

Backend suites covered research evidence flow, controller JSON request keys, academic/eligibility rules, job deduplication/persistence, catalogue matching, refresh/scheduling, narrative validation, official-source access, existing crawlers/programme ingestion, recommendations, Gemini contracts, roadmap behaviour and entitlement enforcement. The full backend suite and database-dependent integration tests were not run. Selected suites use mocks/local fixtures, not production data. Packaging was performed after the successful test run with duplicate test execution disabled; it does not replace the reported test run.

Static HTTP checks only verify asset serving and application-shell fallback. They do not establish authentication, JavaScript rendering, responsive browser behaviour or live API correctness. The temporary loopback preview server was stopped after testing.

Non-blocking build notices remain for bundle size and outdated Browserslist metadata. JDK/Mockito warnings appear in Maven logs; the tests and package commands returned exit 0. UTF-8 reading and a limited private-key/AWS/Google-key pattern scan of all 37 initial untracked files found no matching credentials. This is not an exhaustive security certification.

Logs: `build/repository-audit-{lint,encoding,frontend-tests,frontend-build,backend-tests,backend-package,dist,served-build,career-static-routes}.log`.

## Safe to include in a reviewed commit

The source/test/doc files listed below are eligible for a coherent feature commit with the already-tracked integrations. This means local compilation/testing and conflict checks passed; it does **not** mean production deployment is approved. The docs record current limitations, and the tests are development fixtures rather than hardcoded production recommendations. No important file should be deleted to make the status clean.

### Backend production sources (11)

- `backend/src/main/java/com/edurite/ai/research/AcademicIntelligenceService.java`
- `backend/src/main/java/com/edurite/ai/research/CareerResearchController.java`
- `backend/src/main/java/com/edurite/ai/research/CareerResearchDtos.java`
- `backend/src/main/java/com/edurite/ai/research/CareerResearchJobService.java`
- `backend/src/main/java/com/edurite/ai/research/CareerResearchReportBuilder.java`
- `backend/src/main/java/com/edurite/ai/research/OfficialCatalogueResearchService.java`
- `backend/src/main/java/com/edurite/ai/research/ProgrammeEligibilityService.java`
- `backend/src/main/java/com/edurite/ai/research/ResearchCatalogueScheduler.java`
- `backend/src/main/java/com/edurite/ai/research/ResearchCatalogueService.java`
- `backend/src/main/java/com/edurite/ai/research/ResearchNarrativeService.java`
- `backend/src/main/java/com/edurite/ai/university/OfficialPageAccessPolicy.java`

### Backend tests (8)

- `backend/src/test/java/com/edurite/ai/research/AcademicAndEligibilityTest.java`
- `backend/src/test/java/com/edurite/ai/research/CareerResearchControllerTest.java`
- `backend/src/test/java/com/edurite/ai/research/CareerResearchEvidenceFlowTest.java`
- `backend/src/test/java/com/edurite/ai/research/CareerResearchJobServiceTest.java`
- `backend/src/test/java/com/edurite/ai/research/OfficialCatalogueResearchServiceTest.java`
- `backend/src/test/java/com/edurite/ai/research/ResearchCatalogueServiceTest.java`
- `backend/src/test/java/com/edurite/ai/research/ResearchNarrativeServiceTest.java`
- `backend/src/test/java/com/edurite/ai/university/OfficialPageAccessPolicyTest.java`

### Frontend production sources (9)

- `frontend/src/components/student/guidance/CareerComparison.tsx`
- `frontend/src/components/student/guidance/CareerGuidanceDetails.tsx`
- `frontend/src/components/student/guidance/CareerResearchReportView.tsx`
- `frontend/src/components/student/guidance/SavedResearchStudyOptions.tsx`
- `frontend/src/components/student/guidance/SummaryCardIllustration.tsx`
- `frontend/src/components/student/guidance/career-guidance.css`
- `frontend/src/components/student/guidance/careerGuidanceModel.ts`
- `frontend/src/components/student/guidance/useCareerGuidance.ts`
- `frontend/src/types/careerResearch.ts`

### Frontend tests (6)

- `frontend/src/components/student/guidance/CareerComparison.test.tsx`
- `frontend/src/components/student/guidance/CareerGuidanceDetails.test.tsx`
- `frontend/src/components/student/guidance/CareerResearchReportView.test.tsx`
- `frontend/src/components/student/guidance/CareerResearchSavedCareers.integration.test.tsx`
- `frontend/src/components/student/guidance/SavedResearchStudyOptions.test.tsx`
- `frontend/src/components/student/guidance/careerGuidanceModel.test.ts`

### Documentation (3)

- `docs/career-guidance-redesign.md`
- `docs/career-research-engine.md`
- `docs/career-research-verification.md`

### This audit report

- `docs/repository-audit.md` (new): include with the other documentation if committing the audit outcome.

## Files requiring further work before the full production scope is complete

These are intentional implementation limits, not unresolved merge conflicts or current compiler failures. They can be committed with the documented restrictions, but must not be represented as fully completed production capabilities.

| Area / files | Remaining work |
| --- | --- |
| `CareerResearchJobService.java` | A bounded in-process worker and profile JSON provide current persistence. There is no automatic durable queue replay after restart; expiry recovery occurs on status/start access. High-volume durability, automatic recovery, indexed report storage and multi-instance behaviour need design and staging verification before broad rollout. |
| `ProgrammeEligibilityService.java` | Reviewed BGCSE/ZIMSEC/international qualification rules and compatible recorded grading data are not implemented. Unsupported conditions correctly remain `Insufficient information`. |
| `ResearchCatalogueService.java`, `OfficialCatalogueResearchService.java`, `ResearchCatalogueScheduler.java` | Matching is lexical and research is bounded, not exhaustive. Programme coverage, published admission facts, current intake details and official-site access policies need catalogue review. Optional scheduled refresh remains off by default. |
| Frontend report/shared pages and source/AI services | Live PostgreSQL/Redis/Gemini requests, authenticated desktop/mobile browser behaviour, process interruption, two-instance duplicate prevention and representative EC2 load have not been verified. |
| Commit contents | The 37 original untracked dependencies/tests/docs must be reviewed and included with their integrations; the current HEAD alone is incomplete. Git tracking remains a manual user action. |

No migration or production data modification was introduced to conceal or bypass these gaps. Follow the repository's protected migration process if a later durable storage change is approved. Keep production deployment blocked until the complete candidate commit and applicable live validation gates pass.
