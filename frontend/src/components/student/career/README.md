# Unified Career Explorer

## Entry point and existing work

`/student/explore` renders `StudentCareerRoadmapsExplorerPage` inside the existing dark-sidebar, white-header `StudentAppShell`. This migration preserves the previous Career Explorer components and unrelated working-tree changes. The disconnected roadmap layout has been replaced, and its state, endpoint calls and APS utilities are reused.

The active components are `ExploreWorkspace`, `ExploreSearch`, `ExploreResources`, `ExploreUi`, `RecommendedCareerCard`, `SavedCareers`, `SubjectEditor`, and `CareerRoadmapDetails`. `career-explorer.css` supplies the existing visual language; `explore-workspace.css` adds responsive layout, search, dialogs and print styles. Earlier components in `student/explore/` remain preserved; the active route uses `student/career/`.

## Functionality and API mapping

- Discovery and grouped search: `careerService.list`, `courseService.list`, `institutionService.list`, `bursaryService.search`, and `studentService.searchOpportunities`.
- Personalisation: `studentService.getMe`, `recommendationService.mine`, and `psychometricService.latestStudent`. Missing recommendations fall back to a labelled catalogue; no match percentage is fabricated.
- Subjects and APS: `featureModulesService.apsProfile` / `calculateAps` and existing `roadmapAps.utils`. The subject dialog uses compact responsive rows. Manual edits are a preview; permanent editing uses My Profile.
- Roadmaps: existing generate/save/saved methods from `featureModulesService`. The selected career drives the timeline, requirements and institutional readiness. Full guidance includes university/professional pathways, alternative pathways, improvement suggestions and AI study plans. Print/PDF includes all guidance sections.
- Saved careers: existing `studentService.savedCareers`, `saveCareer`, and `careerService.details`. Careers can be saved before generating a roadmap. Saved opportunity save/unsave uses existing methods and invalidates the shared query cache.
- Institutions and courses: existing institution and course services, plus `universityInfoService.programmes` and `admissionRequirements`. Generated university matches retain backend eligibility states and verification labels, phrased without guaranteeing admission. Directory, qualification, province, institution type, APS and programme study-mode filters use available fields.
- Funding and opportunities: existing bursary recommendations/search and opportunities APIs, plus live `jobsService.search`. Aggregated funding IDs are not treated as internal bursary IDs. Provider links retain the existing application destination. Missing eligibility/deadline/status data is not invented.
- Recent career selections are kept in component state, can be reopened and cleared, and are not persisted across reloads.

## Routes

- `/student/explore`: unified experience.
- `/student/career-roadmaps`: compatible entry into the same experience.
- `/student/careers` and `/student/recommendations/careers`: redirect to Explore.
- `/student/universities`: redirects to Explore, Institutions category.
- `/student/saved`: redirects to Explore, Opportunities category.
- `/student/ai-guidance`: preserves the existing Fast/Deep AI guidance tools.
- Existing profile, application, scholarship, university-detail and authentication routes remain available. Student role guards are unchanged.

## Assets and verification limits

Existing local EduRite imagery is reused as illustrative artwork. The exact reference portrait and career-specific photographs are not available as project assets. No reference student name, APS value, university match or percentage is hard-coded.

TypeScript, production build, UTF-8 validation and the 18 existing frontend tests have passed during this migration. The build retains a large-bundle warning. The final build/test commands should be rerun after any subsequent edits.

The current session has no connected browser and no in-app browser surface. Responsive CSS and route/API contracts were inspected in code; live visual comparison, browser console/overflow checks and authenticated generation/save round trips could not be verified. No claim is made that browser fixtures from the previous standalone design work verify this unified migration.
