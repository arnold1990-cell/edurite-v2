# EduRite career research implementation

For the latest working-tree, commit and deployment assessment, see the [repository audit](repository-audit.md). Test totals below describe the earlier implementation/verification pass.

This extends the existing Student Portal and catalogue; it does not introduce a second dashboard, authentication system, recommendation database, or production deployment. Existing uncommitted guidance UI changes were retained.

## Implemented flow

1. `POST /api/ai/career-research` (also `/api/v1/...`) starts a background job for the authenticated student. `GET` on the same endpoint reads status and the latest successful report. There is no client-supplied student ID.
2. The existing subscription entitlement is checked. The job service reserves one existing AI interaction after locking the student's profile and deduplicating active work. Polling is not chargeable. A UUID `requestKey` in the POST JSON body prevents completed request replays from creating another generation. Explicit regeneration uses a new key.
3. Academic evidence comes from the existing profile, selected transcript results, and transcript history. Levels are not invented percentages; averages and strength rankings do not mix different curricula or examination periods. Historical changes require matching subject, curriculum, scale, examination and known increasing years.
4. The existing recommendation engine ranks actual career catalogue IDs using existing profile, interest, skill, preference and assessment evidence. The artificial paid-plan limit of twelve and top-three study matching were removed. The existing Basic-plan restriction of three remains a subscription policy and is disclosed. No unrelated careers are inserted to meet a target count.
5. The programme matcher reads existing curated career requirements and paginated institution programme records, merges duplicate institution/programme identities and preserves canonical career references. Matching is lexical, explicitly disclosed, and is not proof of professional accreditation or qualification equivalence.
6. Missing/stale sources can trigger one institution refresh. Official pages pass registry, public-network address, redirect, robots and access-challenge checks. Existing page caches, programme tables and retrieval logs are reused. Failed new-institution attempts are recorded without inventing institution/programme data. Prior programme records and successful cached text are retained after failed refreshes.
7. Eligibility compares verified, fresh, compatible structured requirements with actual results. NSC institution APS rules reuse `CareerRoadmapService`; mixed examination periods, missing rules, prose conditions and incompatible curricula remain unresolved. Career suitability and current programme eligibility are separate.
8. Gemini interprets a bounded evidence bundle using the existing server-side provider orchestration. The narrative must satisfy a strict schema, cite only supplied programme sources, contain no new URLs, and introduce no unsupported percentages. The entire report is published only after successful narrative and report validation. Retrieved text is untrusted data, never instructions.
9. Funding comes from the existing local bursary provider and relevant fresh cached official funding pages. Known expired opportunities are excluded during generation and rendering. Unknown deadlines/open status are labelled. No private profile is sent to an internet search provider.
10. Successful reports and job metadata are persisted in the existing versioned student preferences JSON. The UI polls queued/running work, hides partial/failed results, labels saved reports and shares the same programme snapshot with Study Options. Save Career uses existing favourites and real catalogue IDs, so Career Explorer retains its existing saved-career functionality.

## Source providers and countries

- AI: existing configured Gemini service/provider orchestrator; credentials remain server-side. Its existing configured fallback behaviour remains unchanged.
- Discovery: approved institution registry, official seed pages, and official-page link discovery. No new general web-search provider or third-party search subscription was integrated.
- Access policy reference: [Robots Exclusion Protocol, RFC 9309](https://www.rfc-editor.org/rfc/rfc9309.html), including specific-agent groups and longest matching path rules.
- Extraction: shared Jsoup transport and existing PDFBox, with bounded HTML/PDF extraction. There is no CAPTCHA, login or TLS bypass.
- South Africa: the existing official university registry and structured requirement catalogue remain in use.
- Botswana: added official registry seeds for [University of Botswana](https://www.ub.bw/study) and [BIUST](https://www.biust.ac.bw/undergraduate-programmes/).
- Zimbabwe: added official registry seeds for [University of Zimbabwe](https://www.uz.ac.zw/index.php/admissions/university-programmes) and [NUST](https://www.nust.ac.zw/index.php/prospective-students.html).

Seed URLs identify sources to research; they are not prepopulated programme facts or a claim of completed programme verification. Sites can refuse automated access. Additional institutions/countries can be added through the existing registry.

## Admission and research boundaries

- Available structured rules currently support NSC percentage/level thresholds and the existing named institution APS rules. Automated BGCSE/ZIMSEC letter-grade equivalence and institution-specific international selection rules are **not implemented**. Those programmes display `Insufficient information`; no South African APS is assumed.
- Auto-extracted programme titles can be source-confirmed. Extracted prose admission conditions are not automatically promoted to verified structured requirements. They need review in the existing catalogue before deterministic eligibility can be confirmed.
- Published additional conditions, qualification completion, professional registration, selection and available places still require confirmation. No admission guarantee is presented.
- Duration and study mode are extracted only from explicit labels on a single-programme detail page. Tuition, reliable current application-open status, publication dates and complete programme-specific deadlines are not available consistently in the existing schema and are not fabricated.
- All supported career catalogue results within the student's plan are retained. Gemini narrative context is bounded to forty careers, sixty programme records and twenty funding records; this limit is disclosed in the report. Catalogue completeness and lexical programme matching limit discovery coverage.
- Preferences already used by the ranking service are preserved. There is no inference of undisclosed financial constraints or new country grading conversions.
- The report validates structure and grounded references; free-text AI interpretation still needs human judgement and is labelled separately from verified facts.

## Persistence, bounds and operations

No Flyway migration, database schema change, deployment workflow, Docker or AWS change was created. New report/job keys use existing `students.preferences_json`; programme refresh uses existing institution/programme/source tables. No production database was accessed or updated during implementation.

- One background report worker per application instance, queue capacity two. Capacity failures release the reservation. Cross-instance duplicate prevention uses the existing pessimistic profile lock and AI reservation transaction.
- Job lease: ten minutes from creation. Expired/interrupted jobs are marked failed and their reservation released on the next status/start request. Jobs are not automatically resumed after process restart; explicit retry is required.
- One stale institution per targeted generation, maximum four candidate page fetches, existing bounded retries and fetch deadlines. Existing nightly crawling is also bounded to two institutions per run.
- Source freshness: fourteen-day refresh interval; twelve-hour failure backoff; ninety-day admission/source assessment freshness; funding cache thirty days. Saved reports are hidden when evidence changes or after seven days.
- Per-host pacing and robots cache live in process. Targeted refresh additionally uses the existing Redis distributed host lock. Access fails closed when the distributed lock is unavailable.
- Optional scheduled catalogue refresh is **off by default**. `edurite.career-research.refresh-enabled=true` enables one institution per run, rotating SA/Botswana/Zimbabwe. `edurite.career-research.refresh-delay-ms` defaults to 7,200,000. Existing infrastructure configuration was not changed.
- Report JSON is limited to two million characters. Programme reads use pages of 200 with institution entity loading. The existing curated requirement query reads all curated requirements.

The preferences-JSON approach avoids an unapproved migration but is not a replacement for a dedicated durable queue/report store at high scale. A dedicated indexed job/report schema, automatic expired-job sweeper, cross-instance host-wide scheduling and large-catalogue semantic matching need a separately reviewed migration/design before high-volume operation. This implementation does not claim production load certification.

## UI and shared features

The existing orange/white portal header, navigation, academic/transcript/status illustration cards and routes are retained. Generated reports provide profile context, career assessment, progressive career cards, country/category/qualification/eligibility filters, programme filters, subject comparisons, actual strengths/progression, verified alternatives, funding, next steps and source limitations. Existing pathway, comparison, favourites, AI Tutor, study, profile and funding actions are reused.

Study Options adds an existing-page `section=research` view and reads the same saved report through GET. No duplicate recommendation rows are inserted. Existing catalogue recommendations continue to use the shared recommendation service.

## Files

Backend additions: `ai/research/{CareerResearchDtos,AcademicIntelligenceService,ProgrammeEligibilityService,ResearchCatalogueService,OfficialCatalogueResearchService,ResearchCatalogueScheduler,ResearchNarrativeService,CareerResearchReportBuilder,CareerResearchJobService,CareerResearchController}.java`, plus `ai/university/OfficialPageAccessPolicy.java`.

Backend integrations: `RecommendationService`, `GeminiService`, `StudentAccessPolicy`, `UniversityCrawlOrchestrator`, `MultiUniversityPageFetcherService`, `UniversityInfoService`, `UniversityProgrammeRepository`, and official seeds in `application.yml`.

Frontend additions/integrations: `CareerResearchReportView`, `SavedResearchStudyOptions`, `useCareerGuidance`, `types/careerResearch.ts`, `aiGuidanceService`, `StudentCareerMatchPage`, `StudentSectionPages`, `StudentUniversityInfoPages`, and the existing guidance stylesheet. Prior illustration/detail/comparison/model files and tests remain in the working tree.

Tests: new academic/eligibility, job, catalogue, official refresh/scheduling, narrative and public-source access tests; existing recommendation/crawl/programme tests updated for the changed semantics; frontend generation lifecycle and research report interactions extended.

## Validation and release checks

Final automated validation on 2026-10-19 (updated after the continuation; see [verification report](career-research-verification.md) for findings and evidence):

| Check | Result |
| --- | --- |
| `npm run lint` / TypeScript | Passed |
| `npm run validate:encoding` | Passed |
| `npm test` | 219 passed across 28 files |
| `npm run build` | Passed; bundle-size and Browserslist notices remain |
| Relevant Maven research/Gemini/catalogue/roadmap/subscription suites | 116 passed, zero failures/errors/skips; BUILD SUCCESS |
| `git diff --check` | Passed |

Validation logs are local under `build/career-research-*.log`. The entire backend suite was not run; the listed relevant suites were run with mocked external responses. Research controller transport tests verify JSON request-key submission, and the entitlement interceptor test verifies polling and job start are not automatically double reserved.

Automated checks use mocked AI/source responses and local HTTP fixtures, not live production university sites. Frontend checks include typechecking, encoding, all Vitest tests and production build. Backend checks include the new research tests and relevant existing Gemini, subscription, recommendation, roadmap, source and programme tests.

Before production approval: exercise the flow against a staging PostgreSQL/Redis stack and configured Gemini; test duplicate requests across two instances and process interruption; review institution access terms and source verification; populate/review country-specific rules; check desktop/mobile layouts in a browser; perform representative EC2 load measurements. No authenticated browser or live end-to-end source/Gemini/database verification was performed here, and no screenshots or production-readiness claim is supplied.

Phases A–C and F–G have implemented code and automated coverage; D–E and H have bounded source/caching/eligibility infrastructure with the country and durability limits above. Phase I has automated regression checks; live integration and production performance validation remain outstanding. Nothing was committed, pushed or deployed.
