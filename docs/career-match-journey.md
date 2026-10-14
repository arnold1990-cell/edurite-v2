# Career Match journey

Implemented 13 October 2026 in the existing student shell and styles.

## Navigation and UI

`/student/career-match` is the central recommendation page. The assessment lives at
`/student/career-match?step=assessment`, with questions, saved results and attempt history.
Career Guidance links to Career Match and Career Explorer; Explorer now browses the
catalogue and builds roadmaps instead of showing a second personalised recommendation list.
The previous AI recommendation screens have been replaced by one optional **EduRite AI
Guidance** action inside Career Match. AI and catalogue suggestions occupy the same result
area. Funding Match no longer accidentally renders career recommendations.

Compatibility routes remain for `/student/ai-guidance`, `/student/recommendations/careers`,
`/student/psychometric`, `/student/psychometric-test`, `/student/career-explorer/match`, and
the old Explorer `career-match`, `interests` and `guidance` sections. Redirects replace the
old history entry. The existing document/account/profile work is retained.

The page shows inputs used, missing information, reasons for each suggestion, links to edit
academic results, interests and career goals, and an explicit refresh action. Saving an
assessment invalidates recommendation queries; returning to Career Match fetches current
saved inputs. AI output is cleared when recommendation evidence refreshes.

## Data and matching

The existing `RecommendationService` and authenticated `/recommendations/me` endpoint now
return `inputsUsed`, `missingInformation` and `limitations` alongside existing fields.
Evidence includes saved subjects and marks/levels, interests, skills, career goals,
preferred industries, saved course/career associations and available assessment dimensions.
Preferred locations are disclosed as context, with no claim of verified local availability.

Catalogue ranking remains an explainable text-correlation heuristic. Related recorded marks
contribute bounded internal ordering weights. These weights are not validated probabilities
or suitability percentages; the UI and dashboard no longer present them as percentages.
Only candidates with supporting correlations appear. With no supported evidence, the page
offers profile/assessment actions and catalogue browsing rather than invented matches.
Missing or temporarily unavailable assessment data does not block other matching inputs.

The optional AI action reuses `StudentAiGuidanceService`, `ProfileGuidanceContext`, Gemini
and the existing provider orchestrator and subscription/usage controls. Its context includes
the saved evidence and assessment availability. The prompt requests reasons, missing inputs
and no diagnostic or eligibility claims. Generated match percentages are discarded. Malformed
career JSON fails explicitly rather than being converted to fabricated fallback careers.
Catalogue recommendations remain available when AI fails.

## Assessment preservation and ownership

No assessment tables, existing records or scoring history are deleted or migrated.
`GET /api[/v1]/student/psychometric/history` returns the authenticated learner's question-based
attempts across assessment versions, including inactive/archived definitions. The prior
per-assessment history and latest legacy-result endpoints remain available. The UI retains
the legacy latest-result fallback when no question-based attempts exist.

Both history lookups derive the profile from the authenticated principal; clients cannot
select an owner. Returned attempt and result ownership are checked. Viewing an old result
does not replace the latest assessment input. Questions have no preselected or fabricated
answers; the browser requires all answers and the server rejects missing or duplicate question
IDs. Assessment scores are described as recorded response averages, not diagnoses or outcomes.
Missing submissions are distinguished from corrupt/unavailable stored results in the API client.

## Verification

Final result: 134 frontend tests and 86 focused backend tests passed, with no failures or
skips. UTF-8 validation, TypeScript checks, production build and `git diff --check` passed.

- Frontend: `npm.cmd run verify` (UTF-8, TypeScript, Vitest and production build).
- Backend focused suites: `RecommendationServiceTest`, `CareerMatchEvidenceTest`,
  `PsychometricServiceTest`, `PsychometricHistoryIsolationTest`, `StudentServiceTest`,
  `CriticalApiRouteWebMvcTest`, `CareerMatchAiEvidenceTest`, `StudentAiGuidanceServiceTest`,
  `GeminiServiceResponseContractTest`, `EntitlementSecurityTest`.
- Academic-only, assessment-only, combined and neither cases are covered in catalogue-service,
  AI-context and frontend rendering tests. Missing categories and used evidence are asserted;
  no-evidence catalogue results are empty. Assessment outage and preference refresh are covered.
- HTTP controller/service tests verify that forged learner IDs cannot expose another learner's
  latest result or attempt history, on both API prefixes. Security-filter tests reject anonymous
  history requests. Archived ownership and inconsistent result-owner protection are covered.
- Valid assessment save/history reload, incomplete/duplicate rejection, malformed AI output,
  generated-score removal, legacy redirects and router back/forward are covered.

These are automated service, controller, security-filter, router and server-render tests.
Repositories/providers use test doubles in the focused matching tests; they do not establish
live database persistence or live model quality. No authenticated browser session or paid live
AI request was used. Existing bundle-size/Browserslist/JDK warnings remain non-blocking.
