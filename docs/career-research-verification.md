# AI Career Guidance continuation and verification report

For the latest working-tree, commit and deployment assessment, see the [repository audit](repository-audit.md). Test totals below describe the earlier implementation/verification pass.

Date: 2026-10-19. This continues the existing uncommitted implementation. No production data, authentication configuration, infrastructure, deployment workflow or database schema was changed. No commits, pushes or deployments were made.

## What was found after the 101-test run

The 101-test run included the JSON request-key controller contract. Subsequent review identified stale cross-page caching: Study Options could briefly show an earlier report and did not follow a queued/running job. The existing fix added shared-result invalidation, read-only status polling and terminal-status usage refresh. A subscription-interceptor test confirmed GET polling and background-job POST are not automatically double reserved, bringing that earlier backend run to 102 passing tests. The job service remains responsible for one guarded reservation.

This continuation found and fixed these additional issues:

| Finding | Implemented correction |
| --- | --- |
| Duplicate programme records could overwrite associations to another recommended career. | Equivalent records merge all canonical career IDs; distinct admission variants retain their own records and source evidence. |
| Duplicate subject results could distort averages or cause eligibility to use the first recorded mark. | Ambiguous duplicates produce no combined average/strength ranking or confirmed eligibility. |
| Academic context comparison omitted grading scale; conflicting historical marks could produce an arbitrary progression. | Scale is included in the comparison context; conflicting latest historical marks produce no inferred change. |
| Jackson could coerce non-text AI fields into text or accept trailing output after a valid object. | Narrative validation now checks JSON node types and rejects trailing tokens. |
| Unsupported percentages written with a space before `%` could escape validation. | Percentage validation covers spaced values and normalises equivalent decimal representations. |
| Citation validation considered sources outside the bounded evidence actually sent to Gemini. | Allowed references are limited to the same programme/funding subset supplied in the prompt. |
| Targeted ingestion cached pages before institution allowlist validation. | Unapproved pages are filtered before caching or extracting programme data. |
| A redirect to another allowlisted university could be attributed to the original institution. | Redirects must remain within the original institution's approved domains; otherwise retrieval stops. |

## Student data and academic analysis

`CareerResearchEvidenceFlowTest` runs the real recommendation, academic-analysis, programme-matching, eligibility and narrative-validation services together. Student retrieval, repositories and Gemini are isolated test fixtures; no production student records are read.

The tests verify that:

- The report retains real catalogue IDs supplied by the repository, rather than inventing career records.
- Recorded subjects and marks feed the ranking rationale, strongest-subject analysis, improvement priorities and programme gaps.
- Changing the saved profile from an accounting interest with academic results to a technology interest changes both the recommended career and the AI evidence/report.
- Missing evidence produces no successful invented report.
- Subject levels are not converted into fabricated percentages, mixed academic contexts are not averaged, ambiguous results are not used for confirmed admission, and BGCSE/ZIMSEC results are not converted into NSC APS.
- Structured programme records retain their source URL and verification timestamp through the report.
- Private name, email and transcript storage URL are absent from the Gemini evidence bundle prepared by the research narrative service.
- Expired funding is excluded; current funding records and their sources reach the evidence bundle.

These tests verify application data flow with controlled records. They do not claim that a live student account or production database was exercised.

## Sources and programme verification

The application uses the existing official institution registry, permitted page discovery, public-address and robots checks, bounded retrieval and existing programme/source tables. Source URL, institution, verification time/status and eligibility caveats accompany programme records. An extracted title is distinguished from verified structured admission conditions.

During this continuation, the following official pages were checked read-only through web tooling:

| Country | Source checked | Result |
| --- | --- | --- |
| South Africa | [Wits Accounting programme](https://www.wits.ac.za/course-finder/undergraduate/clm/accounting/) | Accessible official programme page with programme and admissions sections. |
| Botswana | [UB Bachelor of Accountancy](https://www.ub.bw/programmes/business/accounting-and-finance/bachelor-accountancy) | Accessible official programme/curriculum page. |
| Zimbabwe | [UZ Accountancy programmes](https://www.uz.ac.zw/index.php/programes-accntcy) | Accessible official departmental programme page. |
| Botswana | [BIUST undergraduate programmes](https://www.biust.ac.bw/undergraduate-programmes/) | Timed out during this check; not reported as a successful retrieval. |

These checks establish that concrete public institutional sources exist and are traceable. They are **not** a live run of EduRite's crawler, a current intake eligibility certification, or a database catalogue update. No student information was included in the public web searches. No university marks, admission thresholds or programme records were inserted into production.

Extraction tests additionally check that a programme title, explicit duration/study-mode labels and its original source URL survive ingestion, while automatically extracted admissions remain `EXTRACTED_UNVERIFIED`. Unapproved sources and cross-institution redirects are rejected. Routine automated tests use mocked responses/local fixtures and do not depend on live university websites.

## Personalised AI summaries

The existing Gemini integration receives actual structured academic evidence, supported career explanations, programme conditions, funding metadata and explicit research limitations. No new AI provider or general web-search service was introduced.

The pipeline tests inspect the exact evidence payload and return a controlled AI response derived from it, then run the real response validator. They confirm that summaries follow the selected career and that marks/source references in the input are retained accurately. Separate tests reject unsupported references/percentages, missing sections, additional fields, wrong JSON types and trailing output.

Live Gemini interpretation quality remains a staging check. Schema/reference validation does not independently prove every free-text statement; the UI labels AI interpretation separately from verified programme facts.

## Shared feature integration

| Feature | Connection and verification |
| --- | --- |
| AI Guidance / Career Match | Existing guidance aliases resolve to the canonical Career Match page. Lifecycle tests cover initial, generating, successful, saved, stale and failed states; partial results remain hidden. |
| Recommendations / programme matching | The real service-chain test verifies canonical career IDs, programme IDs and their links, including merged programme associations. |
| Career Explorer | Guidance uses the existing save/unsave API and query cache. The new saved-career integration test mounts the real guidance save button and Explorer saved-career component together: save, refresh, open and remove all use the same canonical ID. |
| Study Options | Reads the same saved report via GET, with a student-scoped cache key. Existing tests cover queued/running status followed by completed programme rendering, without starting or charging a generation. |
| Pathways and learning | Existing pathway routes, career-name parameters, AI Tutor links and profile/document routes are retained and covered by route/component tests. |

The cross-component tests use mocked backend transports and real React Query behaviour. Live database persistence, two application instances and an authenticated browser have not been verified in this continuation.

## Final automated results

| Check | Final result |
| --- | --- |
| Relevant backend research, controller, eligibility, source, Gemini, roadmap and subscription suites | **116 passed**, zero failures/errors/skips; Maven exit code 0 and BUILD SUCCESS |
| `npm test` | **219 passed** across 28 test files; exit code 0 |
| `npm run lint` (TypeScript `tsc --noEmit`) | Passed; exit code 0 |
| `npm run validate:encoding` | Passed; exit code 0 |
| `npm run build` (TypeScript build and Vite production build) | Passed; exit code 0 |
| `git diff --check` | Passed |

Build warnings remain for bundle size and stale Browserslist metadata; they do not prevent a successful build. The entire backend suite was not run. Local logs are under `build/career-research-*.log`; `career-research-backend-validation.log` and `career-research-all-frontend-tests.log` contain the final suite totals. No staging URL or test-account sign-in method had been supplied at the time of this report, so live account/database/Gemini/browser verification remains unperformed.

## Remaining incomplete features and release checks

- Institution-specific BGCSE/ZIMSEC and international qualification equivalence rules require reviewed structured data and compatible student grading records. Unsupported assessments display `Insufficient information`.
- Programme discovery remains bounded and lexical. It does not claim complete coverage of every institution, semantic career/qualification equivalence or professional accreditation.
- The existing preferences JSON stores the latest report and job metadata. The bounded in-process queue does not automatically resume after restart; expired jobs are released on the next status/start request. A durable high-volume queue, automatic recovery sweeper and dedicated indexed report store require a separate schema/design review.
- Live PostgreSQL/Redis/Gemini, multi-instance duplicate prevention, desktop/mobile browser interactions and EC2 load validation remain outstanding unless a non-production test environment is supplied.
- Scheduled catalogue refresh remains off by default. Source access terms, refreshed catalogue accuracy and current intake requirements need review before enabling it for production.

No migrations or production updates were made to conceal these limitations.

## Files changed during this continuation

Production code:

- `backend/src/main/java/com/edurite/ai/research/ResearchCatalogueService.java`
- `backend/src/main/java/com/edurite/ai/research/AcademicIntelligenceService.java`
- `backend/src/main/java/com/edurite/ai/research/ProgrammeEligibilityService.java`
- `backend/src/main/java/com/edurite/ai/research/ResearchNarrativeService.java`
- `backend/src/main/java/com/edurite/ai/university/OfficialPageAccessPolicy.java`
- `backend/src/main/java/com/edurite/institution/universityinfo/service/UniversityInfoService.java`

Tests added/extended:

- `backend/src/test/java/com/edurite/ai/research/CareerResearchEvidenceFlowTest.java` (new)
- `backend/src/test/java/com/edurite/ai/research/ResearchCatalogueServiceTest.java`
- `backend/src/test/java/com/edurite/ai/research/AcademicAndEligibilityTest.java`
- `backend/src/test/java/com/edurite/ai/research/ResearchNarrativeServiceTest.java`
- `backend/src/test/java/com/edurite/ai/university/OfficialPageAccessPolicyTest.java`
- `backend/src/test/java/com/edurite/institution/universityinfo/service/UniversityInfoServiceTest.java`
- `frontend/src/components/student/guidance/CareerResearchSavedCareers.integration.test.tsx` (new)

Documentation: this report and `docs/career-research-engine.md`. The earlier frontend, backend and illustration changes remain in the working tree; see the implementation notes for that broader inventory.
