# South African university intelligence extension

Development baseline: `e1f7dbd` (`fix: restore career guidance and AI research engine`). All changes extend the existing services, repositories, research jobs and Career Guidance components. Nothing was committed, pushed or deployed.

## Delivered behaviour

- The government-derived public registry includes all 26 institutions, adding the previously missing University of the Western Cape. A dated resource snapshot provides startup availability; scheduled government-directory discovery and persisted directory records provide updates beyond the former YAML-only list. The original institution-specific seeds and operator crawl restrictions are retained.
- Registry metadata supports institution type, province, official/admission/faculty/catalogue/application websites, registration and accreditation status, registration number, provenance URL and verification date. Unknown endpoints/statuses remain unverified; a public institution listing does not certify every programme's accreditation. The snapshot has 12 traditional universities, 8 comprehensive universities and 6 universities of technology.
- Private institutions are a separate category. A configured brand is not treated as proof of registration. Administrator-only verification requires an exact institution/registration-number/website match in a recent register PDF linked from DHET's current register directory. Cancelled, provisional, stale, inaccessible or ambiguous evidence fails closed. Registration evidence expires for research eligibility after 90 days. This does not claim a complete current private institution register.
- The existing scheduled crawler visits every active eligible institution rather than stopping after two. Each institution has its own configured page budget and Redis distributed lock. A bounded persistent frontier follows official faculty, undergraduate/postgraduate, programme, admission, fee and funding links across update runs. A fresh homepage no longer suppresses other unvisited programmes.
- Page-level freshness (14 days), failure backoff (12 hours), existing bounded fetch retries, SHA-256 content-change detection, HTTP ETag/Last-Modified conditional requests and redirect-alias freshness avoid unnecessary fetching and re-extraction. The current in-memory HTTP validator cache is bounded and resets on restart; persisted crawl freshness/frontier/history survive restart. Frontier storage is bounded to 2,000 URLs per institution.
- The existing public-address checks, institution-domain attribution, robots rules, crawl delay, honest user agent, authentication/CAPTCHA/challenge rejection and explicit website prohibition checks remain active. `crawling-permitted=false` disables an institution when an operator identifies restrictive terms. Operators must review site-specific terms; robot permission is not asserted to be a legal permission audit of all sites. Access restrictions are never bypassed.
- The existing `university_programmes` and `university_admission_requirements` tables store programmes and requirements. Programme-specific published evidence adds APS, an explicitly stated supported APS rule, compulsory subject thresholds, intake year, labelled dates/fees/campus/faculty, application links and funding links. Broad listing-page numbers are not assigned to an individual programme. Ambiguous alternatives, conflicting APS values and unpublished costs remain unknown. Existing source URLs, verification times and retrieval logs are retained; a new attempt history also records failures before an institution has any successful programme retrieval.
- Existing career ranking, academic strengths/weaknesses, programme matching, funding research, report generation, saved results and subscription accounting remain in place. Published programme evidence now feeds the eligibility evaluator. Saved preferred study locations influence ordering; an optional saved annual tuition budget influences ordering only for explicit annual ZAR tuition. Missing funding or programme evidence is not invented, and no programme is discarded merely because its fee is unknown.
- Existing verified minimum comparisons retain their API status values and add one of three display categories: **Meets published minimum requirements**, **Does not meet published minimum requirements**, or **Eligibility cannot be determined**. A partial automated extract cannot certify completeness of all published conditions. Admission is never guaranteed. NSC thresholds are not applied to incompatible BGCSE/ZIMSEC results. Existing verified alternative diploma/certificate/TVET catalogue pathways remain supported; no new TVET programme is invented where evidence is unavailable.
- Career Guidance adds an on-demand university coverage panel and published intake/application dates/fees/subject/funding evidence in programme details, using the existing panel/table/link styles. Unknown extracted facts display **Information not yet verified.**

## APIs and preferences

Existing career research start/status routes and `career-research-v1` reports remain compatible. New fields are additive; saved report round trips are tested.

| Route | Access and behaviour |
| --- | --- |
| `GET /api/ai/university-intelligence/coverage` | Authenticated, read-only registry/programme/queue/failure coverage. Also available under `/api/v1/ai/`. No AI credit is reserved. |
| `POST /api/ai/university-intelligence/refresh-directory` | ADMIN/SUPER_ADMIN only; refresh government public directory evidence. |
| `POST /api/ai/university-intelligence/private-registration` | ADMIN/SUPER_ADMIN only; verify a named private institution against current linked DHET PDF evidence. Input: `institutionName`, `registrationNumber`, `officialWebsite`, `registerPdfUrl`. |

The existing student preference store supplies `preferredLocations`. Optional `extra.annualTuitionBudgetZar` (a positive JSON number, maximum 10,000,000) and `extra.fundingRequired` (boolean) supply explicit financial considerations. Missing/invalid values cause no affordability inference. Existing preference APIs remain unchanged. An annual tuition amount is a subtotal, not total cost of study or a funding award. The research summary receives these limitations; no private identity/contact/document information is added to external discovery requests.

Public directory refresh runs weekly Monday at 01:00 via `edurite.university.directory-cron`; the existing university crawl schedule remains daily at 02:00. The existing optional targeted country refresh scheduler remains off by default. All singleton crawling uses the existing distributed lock service and fails closed if the lock service is unavailable.

## Sources, coverage and limits

See [the 26-institution coverage report](south-african-university-coverage.md) for every institution, official government-listed domain, category, province and unavailable/unverified-source notes. The authoritative baseline was obtained from the [Department of Basic Education university directory](https://www.education.gov.za/FurtherStudies/Universities.aspx). DHET's [private register directory](https://www.dhet.gov.za/SitePages/DocRegisters.aspx) remains the private registration authority.

The registry is complete for the 26 public universities requested. **Full programme coverage is not verified.** No production backfill was run. External websites in automated tests are mocked. Catalogue linking is lexical relevance, not proof of a professional accreditation pathway. Pages requiring JavaScript, blocked pages, unlabelled/ambiguous requirements and scanned PDFs may remain unverified. Current retrieval budgets include an 8,000-character content window, 8 headings, 100 outgoing links per page and up to 30 PDF pages; these are explicit partial coverage limits. No application dates, APS rules, fees or programmes are manufactured to fill gaps.

Newly observed faculty/catalogue links are retained in the persistent crawl frontier. The richer in-memory registry link annotations can be rediscovered after restart; the authoritative government registration snapshots are persisted separately. No assertion is made that all optional registry endpoint fields are already verified.

## Database and deployment safety

New, additive Flyway migration: `V83__university_intelligence_evidence.sql`. It adds `university_directory_records`, `university_crawl_attempts` and nullable `university_programmes.published_evidence_json`. Existing programme/admission/crawl tables and data are reused. No V1–V82 migration is edited. The migration upgrade test inserts a synthetic V82 programme, compares full pre-existing row hashes after upgrading to V83, validates history and confirms repeat migration is a no-op. An initial test caught a redundant index declaration already present in V63; it was removed from the new, unapplied V83 file only.

This candidate is **DATABASE_MIGRATION** and must follow the existing manual protected migration deployment procedure: successful exact-commit CI/artifact verification, unchanged applied migration checksums, approved backup/restore evidence, schema review and production environment approval. See [deployment readiness procedure](deployment-readiness-report.md). No production migration or production data write was performed during this work.

The release classifier's blanket treatment of `application.yml` was narrowed only for changes confined to three reviewed schema-inert property subtrees: `edurite.university.registry`, `edurite.university.crawl`, and `edurite.career-research`. Other contents must remain byte-equivalent. YAML aliases/tags/documents, ambiguous indentation and schema/runtime-sensitive settings fail closed. Datasource/Flyway/Hibernate/SQL configuration, entity changes and pending migrations retain the migration gate. `application.yml` itself was not modified. Tests prove that the exception cannot hide pending migrations or schema edits. CI workflows and deployment protections were not bypassed.

## Verification results

- Backend JDK 21 `mvn -B clean verify`: **BUILD SUCCESS**; 524 tests reported, **522 passed**, zero failures/errors, two optional live-provider tests skipped under the existing policy. Executable JAR packaging passed. The final run used fresh `sa_university_complete` and `recovery_qa_sauniversitycomplete` databases in the isolated local cluster.
- `python scripts/check-test-reports.py`: passed, including the newly mandatory `UniversityIntelligenceMigrationTest`. All required database suites executed. The migration test also passed separately with CI's `localhost` hostname variant.

- Frontend: encoding and TypeScript validation, all **220 tests in 29 files**, and production build passed. Built asset validation passed (8 references); locally served build verification passed (2 routes, 8 assets). This verifies asset serving, not an authenticated browser journey.
- Deployment-script tests: **47 passed**, including schema-safe registry configuration and migration-gate regressions.
- Backend automated tests cover public/private discovery, source restrictions, extraction, academic matching, source evidence, duplicate/unchanged-page handling, redirect freshness, complete-registry scheduling, failure backoff, saved-report compatibility, authorization and additive migration preservation. All external websites are mocked.
- Local backend uses JDK 21 and an isolated PostgreSQL 17.7 cluster on loopback port 55439. CI uses PostgreSQL 16/Ubuntu; Docker/Nginx validation was unavailable locally.
- Non-blocking frontend notices: outdated Browserslist metadata and bundle-size warning.
- Git validation: all feature source files are tracked/staged; 641 local frontend imports resolve with exact-case Git paths, Java package/public filenames and Git path case collisions passed, and staged whitespace checks passed. Only new migration V83 is staged; V1–V82 remain unchanged.

Logs are retained under ignored `build/sa-*`. The pre-existing user change to `frontend/tsconfig.tsbuildinfo` is preserved separately from this feature's source changes.

## Changed files

45 feature files are staged for review. The pre-existing `frontend/tsconfig.tsbuildinfo` change remains unstaged and byte-for-byte preserved.

### Backend production and database

- [`backend/src/main/java/com/edurite/ai/research/CareerResearchDtos.java`](../backend/src/main/java/com/edurite/ai/research/CareerResearchDtos.java)
- [`backend/src/main/java/com/edurite/ai/research/CareerResearchReportBuilder.java`](../backend/src/main/java/com/edurite/ai/research/CareerResearchReportBuilder.java)
- [`backend/src/main/java/com/edurite/ai/research/OfficialCatalogueResearchService.java`](../backend/src/main/java/com/edurite/ai/research/OfficialCatalogueResearchService.java)
- [`backend/src/main/java/com/edurite/ai/research/ProgrammeEligibilityService.java`](../backend/src/main/java/com/edurite/ai/research/ProgrammeEligibilityService.java)
- [`backend/src/main/java/com/edurite/ai/research/ProgrammePreferenceOrdering.java`](../backend/src/main/java/com/edurite/ai/research/ProgrammePreferenceOrdering.java)
- [`backend/src/main/java/com/edurite/ai/research/PublishedProgrammeEvidence.java`](../backend/src/main/java/com/edurite/ai/research/PublishedProgrammeEvidence.java)
- [`backend/src/main/java/com/edurite/ai/research/PublishedProgrammeEvidenceExtractor.java`](../backend/src/main/java/com/edurite/ai/research/PublishedProgrammeEvidenceExtractor.java)
- [`backend/src/main/java/com/edurite/ai/research/ResearchCatalogueService.java`](../backend/src/main/java/com/edurite/ai/research/ResearchCatalogueService.java)
- [`backend/src/main/java/com/edurite/ai/university/CrawledUniversityPageRepository.java`](../backend/src/main/java/com/edurite/ai/university/CrawledUniversityPageRepository.java)
- [`backend/src/main/java/com/edurite/ai/university/GovernmentUniversityDirectoryService.java`](../backend/src/main/java/com/edurite/ai/university/GovernmentUniversityDirectoryService.java)
- [`backend/src/main/java/com/edurite/ai/university/MultiUniversityPageFetcherService.java`](../backend/src/main/java/com/edurite/ai/university/MultiUniversityPageFetcherService.java)
- [`backend/src/main/java/com/edurite/ai/university/OfficialPageAccessPolicy.java`](../backend/src/main/java/com/edurite/ai/university/OfficialPageAccessPolicy.java)
- [`backend/src/main/java/com/edurite/ai/university/UniversityCrawlAttemptRepository.java`](../backend/src/main/java/com/edurite/ai/university/UniversityCrawlAttemptRepository.java)
- [`backend/src/main/java/com/edurite/ai/university/UniversityCrawlOrchestrator.java`](../backend/src/main/java/com/edurite/ai/university/UniversityCrawlOrchestrator.java)
- [`backend/src/main/java/com/edurite/ai/university/UniversityDirectoryRepository.java`](../backend/src/main/java/com/edurite/ai/university/UniversityDirectoryRepository.java)
- [`backend/src/main/java/com/edurite/ai/university/UniversityIntelligenceController.java`](../backend/src/main/java/com/edurite/ai/university/UniversityIntelligenceController.java)
- [`backend/src/main/java/com/edurite/ai/university/UniversityRegistryProperties.java`](../backend/src/main/java/com/edurite/ai/university/UniversityRegistryProperties.java)
- [`backend/src/main/java/com/edurite/ai/university/UniversitySourcePageResult.java`](../backend/src/main/java/com/edurite/ai/university/UniversitySourcePageResult.java)
- [`backend/src/main/java/com/edurite/ai/university/UniversitySourceRegistryService.java`](../backend/src/main/java/com/edurite/ai/university/UniversitySourceRegistryService.java)
- [`backend/src/main/java/com/edurite/ai/university/entity/UniversityCrawlAttempt.java`](../backend/src/main/java/com/edurite/ai/university/entity/UniversityCrawlAttempt.java)
- [`backend/src/main/java/com/edurite/ai/university/entity/UniversityDirectoryRecord.java`](../backend/src/main/java/com/edurite/ai/university/entity/UniversityDirectoryRecord.java)
- [`backend/src/main/java/com/edurite/institution/universityinfo/entity/UniversityProgramme.java`](../backend/src/main/java/com/edurite/institution/universityinfo/entity/UniversityProgramme.java)
- [`backend/src/main/java/com/edurite/institution/universityinfo/repository/UniversityProgrammeRepository.java`](../backend/src/main/java/com/edurite/institution/universityinfo/repository/UniversityProgrammeRepository.java)
- [`backend/src/main/java/com/edurite/institution/universityinfo/service/UniversityInfoService.java`](../backend/src/main/java/com/edurite/institution/universityinfo/service/UniversityInfoService.java)
- [`backend/src/main/java/com/edurite/security/config/SecurityConfig.java`](../backend/src/main/java/com/edurite/security/config/SecurityConfig.java)
- [`backend/src/main/resources/db/migration/V83__university_intelligence_evidence.sql`](../backend/src/main/resources/db/migration/V83__university_intelligence_evidence.sql)
- [`backend/src/main/resources/university-registry/sa-public-universities.json`](../backend/src/main/resources/university-registry/sa-public-universities.json)

### Frontend

- [`frontend/src/components/student/guidance/CareerResearchReportView.tsx`](../frontend/src/components/student/guidance/CareerResearchReportView.tsx)
- [`frontend/src/components/student/guidance/UniversityCoveragePanel.tsx`](../frontend/src/components/student/guidance/UniversityCoveragePanel.tsx)
- [`frontend/src/types/careerResearch.ts`](../frontend/src/types/careerResearch.ts)

### Tests and release tooling

- [`backend/src/test/java/com/edurite/ai/research/CareerResearchEvidenceFlowTest.java`](../backend/src/test/java/com/edurite/ai/research/CareerResearchEvidenceFlowTest.java)
- [`backend/src/test/java/com/edurite/ai/research/OfficialCatalogueResearchServiceTest.java`](../backend/src/test/java/com/edurite/ai/research/OfficialCatalogueResearchServiceTest.java)
- [`backend/src/test/java/com/edurite/ai/research/PublishedProgrammeEvidenceTest.java`](../backend/src/test/java/com/edurite/ai/research/PublishedProgrammeEvidenceTest.java)
- [`backend/src/test/java/com/edurite/ai/research/UniversityEvidenceIngestionTest.java`](../backend/src/test/java/com/edurite/ai/research/UniversityEvidenceIngestionTest.java)
- [`backend/src/test/java/com/edurite/ai/university/GovernmentUniversityDirectoryServiceTest.java`](../backend/src/test/java/com/edurite/ai/university/GovernmentUniversityDirectoryServiceTest.java)
- [`backend/src/test/java/com/edurite/ai/university/OfficialPageAccessPolicyTest.java`](../backend/src/test/java/com/edurite/ai/university/OfficialPageAccessPolicyTest.java)
- [`backend/src/test/java/com/edurite/ai/university/UniversityCrawlOrchestratorTest.java`](../backend/src/test/java/com/edurite/ai/university/UniversityCrawlOrchestratorTest.java)
- [`backend/src/test/java/com/edurite/ai/university/UniversityIntelligenceMigrationTest.java`](../backend/src/test/java/com/edurite/ai/university/UniversityIntelligenceMigrationTest.java)
- [`backend/src/test/java/com/edurite/security/config/AuthAndAiSecurityWebMvcTest.java`](../backend/src/test/java/com/edurite/security/config/AuthAndAiSecurityWebMvcTest.java)
- [`frontend/src/components/student/guidance/UniversityCoveragePanel.test.tsx`](../frontend/src/components/student/guidance/UniversityCoveragePanel.test.tsx)
- [`scripts/check-test-reports.py`](../scripts/check-test-reports.py)
- [`scripts/classify_release.py`](../scripts/classify_release.py)
- [`scripts/tests/test_classified_release.py`](../scripts/tests/test_classified_release.py)

### Documentation

- [`docs/south-african-university-coverage.md`](../docs/south-african-university-coverage.md)
- [`docs/south-african-university-intelligence.md`](../docs/south-african-university-intelligence.md)
