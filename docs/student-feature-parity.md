# Student feature parity inventory

Audit sources: working diff, HEAD (4a64c52), pre-redesign router (675a24c), all student page exports and service calls. No existing work reset.

- `/student/dashboard`: `StudentDashboardPage` (content-only; no application shell).
- `/student/profile`: `StudentProfilePage` (content-only; no application shell).
- `/student/academic-profile`: `StudentAcademicProfilePage` (content-only; no application shell).
- `/student/documents`: `StudentDocumentsPage` (content-only; no application shell).
- `/student/qualifications`: `StudentQualificationsPage` (content-only; no application shell).
- `/student/experience`: `StudentExperiencePage` (content-only; no application shell).
- `/student/recommendations/careers`: `StudentCareerRecommendationsPage` (content-only; no application shell).
- `/student/recommendations/bursaries`: `StudentBursaryRecommendationsPage` (content-only; no application shell).
- `/student/psychometric`: `StudentPsychometricPage` (content-only; no application shell).
- `/student/cv-builder`: `StudentCvBuilderPage` (content-only; no application shell).
- `/student/ai-tutor`: `StudentAiTutorPage` (content-only; no application shell).
- `/student/learning-centre`: `StudentLearningCentrePage` (content-only; no application shell).
- `/student/rewards`: `StudentRewardsPage` (content-only; no application shell).
- `/student/careers/:id`: `StudentCareerDetailsPage` (content-only; no application shell).
- `/student/career-roadmaps`: `StudentCareerRoadmapsPage` (content-only; no application shell).
- `/student/saved`: `StudentSavedPage` (content-only; no application shell).
- `/student/applications`: `StudentApplicationsPage` (content-only; no application shell).
- `/student/scholarships`: `StudentScholarshipAssistantPage` (content-only; no application shell).
- `/student/universities`: `StudentUniversitiesPage` (content-only; no application shell).
- `/student/universities/:slug/programmes`: `StudentUniversityProgrammesPage` (content-only; no application shell).
- `/student/universities/:slug/admission-requirements`: `StudentUniversityAdmissionRequirementsPage` (content-only; no application shell).
- `/student/colleges-tvets`: `StudentCollegesTvetsPage` (content-only; no application shell).
- `/student/university-applications`: `StudentUniversityApplicationsPage` (content-only; no application shell).
- `/student/notifications`: `StudentNotificationsPage` (content-only; no application shell).
- `/student/subscription`: `StudentSubscriptionPage` (content-only; no application shell).
- `/student/settings`: `StudentSettingsPage` (content-only; no application shell).
- `/student/my-school`: `StudentMySchoolPage` (content-only; no application shell).

## Compatibility map

| Old route | New parent / section | Accessible | Functional / API |
|---|---|---|---|
| /student/academic-profile | /student/profile?section=academic | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/documents | /student/profile?section=documents | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/qualifications | /student/profile?section=career | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/experience | /student/profile?section=career | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/my-school | /student/profile?section=school | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/cv-builder | /student/profile?section=cv | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/profile/cv | /student/profile?section=cv | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/ai-guidance | /student/learning?section=guidance | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/recommendations/careers | /student/learning?section=guidance | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/ai-tutor | /student/learning?section=tutor | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/learning/tutor | /student/learning?section=tutor | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/interview-prep | /student/learning?section=tutor | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/learning-centre | /student/learning?section=centre | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/psychometric | /student/career-explorer?section=career-match | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/psychometric-test | /student/career-explorer?section=career-match | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/career-explorer/interests | /student/career-explorer?section=interests | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/career-explorer/match | /student/career-explorer?section=guidance | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/career-guidance | /student/career-explorer?section=guidance | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/career-roadmaps | /student/career-explorer?section=career-path | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/saved | /student/funding?section=opportunities | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/opportunities | /student/funding?section=opportunities | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/applications | /student/funding?section=bursaries | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/bursaries | /student/funding?section=bursaries | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/scholarships | /student/funding?section=scholarships | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/scholarship-assistant | /student/funding?section=scholarships | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/recommendations/bursaries | /student/funding?section=matches | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/universities | /student/institutions?section=universities | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/colleges-tvets | /student/institutions?section=colleges-tvets | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/university-applications | /student/institutions?section=applications | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/rewards | /student/progress?section=rewards | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/progress/rewards | /student/progress?section=rewards | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/funding/saved | /student/funding?section=saved | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/funding/applications | /student/funding?section=applications | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/funding/scholarships | /student/funding?section=scholarships | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/funding/matches | /student/funding?section=matches | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/institutions/universities | /student/institutions?section=universities | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/institutions/colleges-tvets | /student/institutions?section=colleges-tvets | Routed + section navigation + tool search | Existing component and service retained; live verification pending |
| /student/institutions/applications | /student/institutions?section=applications | Routed + section navigation + tool search | Existing component and service retained; live verification pending |

Dashboard, notifications, subscription, settings, career details, university programmes/admission requirements retain their routes under StudentAppShell. Goals reuse profile goal editing; Messages reuse notifications (no separate legacy messaging API).

Preserved APIs: studentService (profile, saved profiles, uploads, opportunities, saved bursaries); schoolService (school directory/join); aiGuidanceService + recommendationService; psychometricService (assessments, attempts, history); featureModulesService (CV/suggestions/save, tutor sessions/ask, scholarships/motivation, university applications CRUD, roadmaps/APS/progress); learningService; gamificationService; applicationService; bursaryService; institutionService; universityInfoService; notificationService; subscriptionService (checkout, confirm, cancel, PayFast initiate/status); settingsService; accountService. All retain useAppQuery/useMutation integration.


## Verified source inventory (2026-10-01)

The following table is generated from every `/student/*` route in the pre-redesign router at `675a24c`, then checked against the working router, page exports and service imports. `4a64c52` and the pre-existing working diff were also inspected. Each retained page below is a content component: its `Section` wrapper contains a heading/content, not an application header/sidebar. The common `StudentAppShell` owns the application navigation.

Accessible = present in the router and the parent navigation/search (account pages use the avatar menu). Functional = original feature code/API retained and typechecked; **authenticated end-to-end operation is not verified**. The section tests check which retained component is mounted, not external service availability.

| Old feature / component | Old route | Existing API/hook | New parent | Section / access | Accessible? | Functional? | API preserved? |
|---|---|---|---|---|---|---|---|
| `StudentDashboardPage` | `/student/dashboard` | useStudentDashboard / studentService.dashboard; existing query/mutation hooks | Dashboard | Overview | Yes, routed | Retained; live check pending | Yes |
| `StudentProfilePage` | `/student/profile` | studentService.getMe/updateMe; saved profile and upload APIs; existing query/mutation hooks | My Profile | Overview | Yes, routed | Retained; live check pending | Yes |
| `StudentAcademicProfilePage` | `/student/academic-profile` | studentService; featureModulesService APS; report uploads; existing query/mutation hooks | My Profile | Academic Profile | Yes, routed | Retained; live check pending | Yes |
| `StudentDocumentsPage` | `/student/documents` | studentService uploads and saved profiles; existing query/mutation hooks | My Profile | Documents | Yes, routed | Retained; live check pending | Yes |
| `StudentQualificationsPage` | `/student/qualifications` | studentService.updateMe (qualifications); existing query/mutation hooks | My Profile | Career Aspirations | Yes, routed | Retained; live check pending | Yes |
| `StudentExperiencePage` | `/student/experience` | studentService.updateMe (experience); existing query/mutation hooks | My Profile | Career Aspirations | Yes, routed | Retained; live check pending | Yes |
| `StudentCareerRecommendationsPage` | `/student/recommendations/careers` | aiGuidanceService; recommendationService; subscriptionService; existing query/mutation hooks | Learning Resources | AI Guidance; also Career Explorer / Career Guidance | Yes, routed | Retained; live check pending | Yes |
| `StudentBursaryRecommendationsPage` | `/student/recommendations/bursaries` | aiGuidanceService; recommendationService; existing query/mutation hooks | Bursaries & Funding | Funding Match | Yes, routed | Retained; live check pending | Yes |
| `StudentPsychometricPage` | `/student/psychometric` | psychometricService assessment, attempts, latest and history; existing query/mutation hooks | Career Explorer | Career Match; Psychometric / Interests | Yes, routed | Retained; live check pending | Yes |
| `StudentCvBuilderPage` | `/student/cv-builder` | featureModulesService.getCv/cvSuggestions/saveCv; existing query/mutation hooks | My Profile | CV Builder | Yes, routed | Retained; live check pending | Yes |
| `StudentAiTutorPage` | `/student/ai-tutor` | featureModulesService.tutorSessions/tutorSession/askTutor; existing query/mutation hooks | Learning Resources | EduRite Tutor | Yes, routed | Retained; live check pending | Yes |
| `StudentLearningCentrePage` | `/student/learning-centre` | learningService; YouTube/resource search and filters; existing query/mutation hooks | Learning Resources | Learning Centre; Resources | Yes, routed | Retained; live check pending | Yes |
| `StudentRewardsPage` | `/student/rewards` | gamificationService points, ledger, rewards and claim; existing query/mutation hooks | My Progress | Points & Rewards; Achievements | Yes, routed | Retained; live check pending | Yes |
| `StudentCareerDetailsPage` | `/student/careers/:id` | careerService.details; studentService saved careers; existing query/mutation hooks | Career Explorer | Career details (retained detail route) | Yes, routed | Retained; live check pending | Yes |
| `StudentCareerRoadmapsPage` | `/student/career-roadmaps` | featureModulesService APS, generate/save/list roadmaps; studentService saved careers; existing query/mutation hooks | Career Explorer | Career Path; Learning Path; Academic Readiness; Saved Careers | Yes, routed | Retained; live check pending | Yes |
| `StudentSavedPage` | `/student/saved` | studentService.searchOpportunities/saveOpportunity/unsaveOpportunity; jobsService; existing query/mutation hooks | Bursaries & Funding | Opportunities | Yes, routed | Retained; live check pending | Yes |
| `StudentApplicationsPage` | `/student/applications` | applicationService.listMine/submit; bursaryService.search/recommended; studentService saved bursaries; existing query/mutation hooks | Bursaries & Funding | Applications; Bursaries | Yes, routed | Retained; live check pending | Yes |
| `StudentScholarshipAssistantPage` | `/student/scholarships` | featureModulesService scholarships, create/update, upcoming, motivationLetter; existing query/mutation hooks | Bursaries & Funding | Scholarships | Yes, routed | Retained; live check pending | Yes |
| `StudentUniversitiesPage` | `/student/universities` | institutionService; universityInfoService; existing query/mutation hooks | Institutions | Universities | Yes, routed | Retained; live check pending | Yes |
| `StudentUniversityProgrammesPage` | `/student/universities/:slug/programmes` | universityInfoService; existing query/mutation hooks | Institutions | Programmes detail (retained route) | Yes, routed | Retained; live check pending | Yes |
| `StudentUniversityAdmissionRequirementsPage` | `/student/universities/:slug/admission-requirements` | universityInfoService; existing query/mutation hooks | Institutions | Admission requirements detail (retained route) | Yes, routed | Retained; live check pending | Yes |
| `StudentCollegesTvetsPage` | `/student/colleges-tvets` | institutionService; existing query/mutation hooks | Institutions | Colleges / TVET | Yes, routed | Retained; live check pending | Yes |
| `StudentUniversityApplicationsPage` | `/student/university-applications` | featureModulesService universityApplications/create/update/delete; existing query/mutation hooks | Institutions | Applications; Saved drafts | Yes, routed | Retained; live check pending | Yes |
| `StudentNotificationsPage` | `/student/notifications` | notificationService list, unread and read actions; existing query/mutation hooks | Account menu | Notifications | Yes, routed | Retained; live check pending | Yes |
| `StudentSubscriptionPage` | `/student/subscription` | subscriptionService current/plans/checkout/confirm/cancel; PayFast initiate/status; existing query/mutation hooks | Account menu | Subscription | Yes, routed | Retained; live check pending | Yes |
| `StudentSettingsPage` | `/student/settings` | settingsService; accountService; existing query/mutation hooks | Account menu | Settings | Yes, routed | Retained; live check pending | Yes |
| `StudentMySchoolPage` | `/student/my-school` | schoolService school directory, membership and requests; existing query/mutation hooks | My Profile | School | Yes, routed | Retained; live check pending | Yes |

### Additional destinations from the current migration

| Feature | New location | Reused content / data | Status |
|---|---|---|---|
| Discover / career recommendations | Career Explorer / Discover | ExploreWorkspace, recommendationService and careerService catalogue | Mounted |
| Career Guidance | Career Explorer / Career Guidance | StudentCareerRecommendationsPage + AiGuidance | Mounted |
| Interests assessment | Career Explorer / Psychometric / Interests | Full StudentPsychometricPage, assessment submission/history retained | Mounted |
| Learning Path / Study Plan | Career Explorer / Learning Path; Learning Resources / Study Plan | CareerRoadmapDetails AI Study Plan, generation and saved snapshots | Mounted; opening saved snapshots now preserves the requested detail tab |
| APS readiness | Career Explorer / Academic Readiness | SubjectEditor and authoritative APS calculations | Mounted |
| Saved careers / roadmaps | Career Explorer / Saved Careers | SavedCareers and existing career/roadmap services | Mounted |
| Study Options | Study Options | ExploreStudyOptions, institution/course/subject queries | Mounted |
| Saved funding | Bursaries & Funding / Saved | Saved opportunity queries and saved provider bursaries | Mounted |
| Saved institutions | Institutions / Saved | Existing saved DRAFT/READY university applications | Mounted; no independent legacy institution-bookmark API was found |
| Programmes | Institutions / Programmes | ExploreCourses plus retained university detail routes | Mounted |
| Academic progress | My Progress / Academic Progress | Full academic profile editor/readiness content | Mounted |
| Learning progress | My Progress / Learning Progress | featureModulesService.progressScore learning card | Mounted |
| Goals | My Goals | Existing profile careerGoals editor/save action | Mounted |
| Messages | Messages | Existing notifications component/service | Mounted; no separate legacy student chat service was found |
| Password | Account menu / Settings / password | AccountChangePasswordPage; accountService | Retained inside student shell, including /account/change-password relay |
| Logout | Avatar menu / Log out | Existing useAuth.logout | Wired; no live session available to exercise |

### Changes made during this correction

- Kept the existing consolidated shell, section navigation, search and redirects; no reset or deletion of prior work.
- Reused the real CV editor, suggestions, preview, save and service calls. Tutor sessions, AI guidance, psychometric attempts/history, learning catalogue, rewards, scholarship AI and university application CRUD remain in their original content components.
- Removed the profile's duplicate internal tab strip. Profile edit/quick actions now navigate to canonical sections; Goals opens My Goals. Explicit Overview takes precedence over a stale legacy `tab` parameter.
- Added Qualifications/Experience and Saved Profiles/Documents search aliases.
- Preserved the requested AI Study Plan / APS Readiness tab when selecting a saved roadmap or a career.
- Split Funding Applications from Bursary Finder using the existing component's `applicationsOnly` option. Applications now render actual returned records/statuses.
- Corrected Bursary Finder's `id`/`externalId` mismatch. Provider bursaries use existing save/apply endpoints; external bursaries use their provider links. Application submission now invalidates the list and exposes success/error feedback.
- Preserved horizontally scrolling section links, existing responsive content grids, mobile navigation focus handling and the single account menu.
- Removed Vite's existing environment-value console dump from validation output.
- No useful legacy feature components were removed. No backend API/payment logic was recreated. Existing unrelated backend changes were left alone.

### Validation and remaining limits

- Source/route checks: one authenticated StudentAppShell, every section resolves, compatibility redirects preserve query strings and hashes, and no legacy shell is embedded in student feature pages.
- Rendered section checks: retained feature components are selected for CV, school, profile editors, career guidance, assessments, roadmaps, tutor, resources, funding, institutions and rewards.
- Browser/manual validation is blocked: browser inventory returned no browsers and opening `iab` returned `Browser is not available: iab`. Consequently responsive screenshots, actual clicks/logout, authenticated AI/API operations and PayFast transactions are **not claimed as tested**.
- No old useful feature has been intentionally omitted. Live operational parity still requires an available authenticated browser/backend and, for billing, the payment test environment.

Legacy `/student/applications` intentionally redirects to Bursaries because its actual old component was Bursary Finder (including an application count). `/student/funding/applications` opens the dedicated Applications section. This preserves old dashboard/profile Explore Bursaries links.


### Final command results

| Check | Result |
|---|---|
| `npm run typecheck` (frontend) | Passed |
| `npm run lint` (frontend) | Passed; configured as `npm run typecheck`, not ESLint |
| `npm test` (frontend) | Passed: 8 files, 89 tests |
| `npm run build` (frontend) | Passed: TypeScript project build and Vite production bundle |
| `npm run validate:encoding` | Passed |
| `git diff --check` | Passed |
| Browser/manual navigation and responsive screenshots | Blocked: no browser available |
| Authenticated API mutations, AI generation and PayFast end-to-end | Not exercised; require a live authenticated test environment |

Build warnings: the main application chunk exceeds 500 kB and Browserslist data is outdated. No backend code was changed by this correction, so backend tests were not rerun. A service-call comparison against the pre-redesign StudentPages, StudentFeaturePages and StudentUniversityInfoPages found no removed integrations: applicationService.submit moved from a direct click callback to the existing mutation pattern.
