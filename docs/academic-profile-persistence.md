# Academic profile persistence phase

Implemented in the existing student profile API and students JSONB columns. No new academic database or migration is required. Existing unrelated working-tree changes were retained. AGENTS.md was not present in the repository or its immediate parent. AdminController.java was inspected; academic writes belong to StudentController/StudentService, so AdminController was left unchanged.

## Failures traced and corrected

- Transcript upload immediately merged extracted results into the live profile and could replace unverified manual marks without learner confirmation. Upload now stores the document and extraction history only. Learners explicitly select extracted results, correct them, and save the complete reviewed profile.
- Full subject-list saves had no stale-draft comparison, and duplicate canonical names could overwrite each other inside a map. The frontend now submits its loaded academic baseline; a changed baseline returns HTTP 409. Equivalent names deduplicate, conflicting marks/context are rejected, and protected verified results cannot be silently changed.
- Academic context was absent from the DTO and was lost when normalizing or saving results. Curriculum, grading scale, academic year and examination now survive JSON serialization, manual edits, extraction review, saved profile payloads and guidance context.
- Invalid persisted subject JSON was treated as an empty list. It now produces an actionable error rather than hiding stored data and allowing its replacement.
- Career Explorer manual marks were preview-only. Its editor now loads the latest saved results, presents old/new values, and confirms a real profile API save. Other saved subjects and their context are retained.

## User flow

My Profile -> Academic Details shows the persisted subject list, an extraction review, and editable marks/context. Save Profile uses PUT /api/v1/student/profile. Document upload uses POST /api/v1/student/profile/transcript; GET /api/v1/student/transcripts reloads extraction history even after a refresh. Select each desired extraction result, resolve replacements explicitly, correct fields, and save. Uploading while a manual draft exists does not overwrite the draft.

Career Explorer -> Enter marks manually -> Review and save marks -> Confirm and save to profile uses the same PUT endpoint and stale-data check. Guidance and APS continue reading the authenticated learner's students.subject_achievements_json; query invalidation refreshes their profile-derived results.

The existing authentication layer resolves the learner from the authenticated principal, not a supplied student ID. Existing session cache clearing and in-flight response protection were preserved.

## Files changed by this phase

Backend:

- backend/src/main/java/com/edurite/student/dto/StudentSubjectAchievementDto.java
- backend/src/main/java/com/edurite/student/dto/StudentProfileUpsertRequest.java
- backend/src/main/java/com/edurite/student/service/StudentService.java
- backend/src/main/java/com/edurite/student/service/StudentSubjectCatalogue.java
- backend/src/main/java/com/edurite/student/service/TranscriptExtractionService.java
- backend/src/main/java/com/edurite/student/service/ProfileGuidanceContext.java
- backend/src/main/java/com/edurite/common/exception/ApiExceptionHandler.java (added concurrent-save conflict handling; retained existing edits)

Frontend:

- frontend/src/pages/student/StudentPages.tsx (retained existing edits)
- frontend/src/pages/student/StudentCareerRoadmapsExplorerPage.tsx
- frontend/src/pages/student/academicResults.ts
- frontend/src/components/student/career/SubjectEditor.tsx
- frontend/src/services/studentService.ts
- frontend/src/types/index.ts (retained existing edits)

Tests and evidence:

- backend/src/test/java/com/edurite/student/StudentServiceTest.java (added regression tests; retained existing edits)
- backend/src/test/java/com/edurite/student/TranscriptExtractionServiceTest.java
- backend/src/test/java/com/edurite/student/AcademicProfilePersistenceIntegrationTest.java
- frontend/src/pages/student/academicResults.test.ts
- docs/academic-profile-persistence.md

## Verification

40 backend tests passed with zero failures/errors/skips across StudentServiceTest, TranscriptExtractionServiceTest, AcademicProfilePersistenceIntegrationTest, StudentAiGuidanceServiceTest, CareerRoadmapServiceTest and DocumentStorageTest.

The integration test actually ran against local PostgreSQL in a generated, isolated schema that was dropped afterward. It uses HTTP controller binding, the real student service, Hibernate's StudentProfile mapping, real JSONB writes, real PDF extraction and temporary document storage. Other application services and authenticated principals are test fixtures. It verified:

1. Manual Mathematics 70% saved and reloaded with a fresh persistence session.
2. A real PDF containing Physical Sciences 75% uploaded without changing the saved subject list.
3. Learner correction to 76%, combined with Mathematics, persisted through PUT and a fresh GET.
4. Academic year and examination context survived the database round trip.
5. A second learner saw an empty academic profile.
6. A stale update received HTTP 409.
7. Direct SQL confirmed subject_achievements_json is a JSON array containing two results.

Other regression tests cover percentage/level boundaries, equivalent names, duplicate conflicts, invalid marks, foreign document provenance, forged verification flags, actual DOCX table extraction, unreadable files, and persisted guidance context.

Frontend production build (tsc -b + Vite), UTF-8 validation, and 50 focused tests passed. Focused suites: academicResults, StudentProfileView, apiClient.session, roadmapAps.utils, StudentSectionPages. git diff --check passed. Build warnings concern existing bundle size and Browserslist data age.

To repeat the database integration test, set EDURITE_ACADEMIC_TEST_DB_URL to an explicitly local jdbc:postgresql://127.0.0.1:PORT/DATABASE URL, with EDURITE_ACADEMIC_TEST_DB_USER and EDURITE_ACADEMIC_TEST_DB_PASSWORD, then run Maven with -Dtest=AcademicProfilePersistenceIntegrationTest test. Without the local test URL this opt-in test skips. Credentials were not added to the repository.

## Runtime limits

- Full browser logout/login against the rebuilt application was not exercised. The database test uses fresh principals and persistence sessions; the existing frontend session isolation tests passed. This is not a claim of a complete JWT/browser login test.
- Live AI-provider requests were not executed; persisted guidance input and guidance/roadmap service regressions were checked.
- Automatic extraction supports text-based PDFs and DOCX. Scanned PDFs needing OCR, legacy binary DOC, unsupported layouts and ambiguous multi-column results return review-required guidance and retain the uploaded document; learners can enter/correct marks manually. No OCR provider is configured in the existing extraction architecture.
- The existing NSC 1-7 percentage conversion remains in use. Context fields are preserved, but arbitrary grading-system conversion was not added.
