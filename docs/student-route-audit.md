# Student route audit

Cause: DashboardLayout selected the new shell using a pathname allowlist. Every other student route fell through to the legacy header, horizontal navigation and white sidebar.

All routes below now belong to the authenticated, STUDENT-gated /student parent with StudentAppShell. Pages provide content only. Existing URLs are retained for bookmarks and payment callbacks. The current StudentDashboardPage is unchanged; its basic/premium detail components are still used.

| Route | Page / redirect before migration | Previous shell | Target section |
| --- | --- | --- | --- |
| /student/dashboard | `<StudentDashboardPage />` | New | Dashboard |
| /student/profile | `<StudentProfilePage />` | New | My Profile |
| /student/careers | `<Navigate to="/student/explore" replace />` | Old | Career Explorer |
| /student/recommendations/careers | `<Navigate to="/student/explore" replace />` | New | Career Explorer |
| /student/academic-profile | `<StudentAcademicProfilePage />` | New | My Profile |
| /student/documents | `<StudentDocumentsPage />` | New | My Profile |
| /student/qualifications | `<StudentQualificationsPage />` | New | My Profile |
| /student/experience | `<StudentExperiencePage />` | New | My Profile |
| /student/recommendations/bursaries | `<StudentBursaryRecommendationsPage />` | Old | Bursaries & Funding |
| /student/psychometric | `<StudentPsychometricPage />` | Old | Career Explorer |
| /student/cv-builder | `<StudentCvBuilderPage />` | Old | My Profile |
| /student/ai-tutor | `<StudentAiTutorPage />` | Old | Learning Resources |
| /student/learning-centre | `<StudentLearningCentrePage />` | Old | Learning Resources |
| /student/rewards | `<StudentRewardsPage />` | Old | My Progress |
| /student/careers/:id | `<StudentCareerDetailsPage />` | Old | Career Explorer |
| /student/ai-guidance | `<StudentCareerRecommendationsPage />` | New | Career Explorer |
| /student/explore | `<StudentCareerRoadmapsPage />` | New | Career Explorer |
| /student/career-roadmaps | `<StudentCareerRoadmapsPage />` | New | Career Explorer |
| /student/saved | `<Navigate to="/student/explore?category=Opportunities" replace />` | Old | Bursaries & Funding |
| /student/applications | `<StudentApplicationsPage />` | Old | Bursaries & Funding |
| /student/scholarships | `<StudentScholarshipAssistantPage />` | Old | Bursaries & Funding |
| /student/universities | `<Navigate to="/student/explore?category=Institutions" replace />` | New | Institutions |
| /student/universities/:slug/programmes | `<StudentUniversityProgrammesPage />` | Old | Institutions |
| /student/universities/:slug/admission-requirements | `<StudentUniversityAdmissionRequirementsPage />` | Old | Institutions |
| /student/colleges-tvets | `<StudentCollegesTvetsPage />` | Old | Institutions |
| /student/university-applications | `<StudentUniversityApplicationsPage />` | Old | Institutions |
| /student/notifications | `<StudentNotificationsPage />` | Old | Account |
| /student/subscription | `<StudentSubscriptionPage />` | Old | Account |
| /student/settings | `<StudentSettingsPage />` | Old | Account |
| /student/my-school | `<StudentMySchoolPage />` | Old | My Profile |

Additional canonical routes: /student/career-explorer, /student/study-options, /student/funding, /student/institutions, /student/learning, /student/progress, /student/goals, /student/messages, /student/profile/cv, /student/funding/saved.

Messages uses the existing delivered notification inbox; this migration does not introduce a direct-messaging backend.

The existing learning-centre link to /student/interview-prep previously had no route; it now opens the existing AI Tutor under Learning Resources. Career Explorer exposes the existing saved-career/roadmap dialog through its Saved section. My Progress reuses the progress-score API and links to the existing rewards feature.


## Canonical routes and compatibility

All legacy feature content is retained. Compatibility redirects preserve search parameters and hashes. Subscription, notifications and settings remain account routes within StudentAppShell. Password changes are available at `/student/settings/password`; the shared account URL redirects students there. Other roles retain their layouts.

| Canonical section | Content routes |
| --- | --- |
| Profile | profile, academic-profile, documents, qualifications, experience, my-school, profile/cv |
| Career Explorer | career-explorer, career-explorer/match, career-explorer/interests, careers/:id; saved roadmaps through ?view=saved |
| Study Options | study-options |
| Funding | funding, funding/matches, funding/saved, funding/scholarships, funding/applications |
| Institutions | institutions, institutions/universities, institutions/colleges-tvets, institutions/applications; existing university programme/admission detail URLs |
| Learning | learning, learning/tutor |
| Progress | progress, progress/rewards |
| Goals | goals (existing profile goals editor) |
| Messages | messages (existing notification inbox; no invented messaging backend) |
| Account | subscription, notifications, settings, settings/password |

The obsolete StudentDashboardLayout component and legacy student header/sidebar render branch are removed. Existing basic/premium dashboard detail components remain because the canonical dashboard still uses their functionality. Shared content styling is scoped to StudentAppShell; PayFast and feature service calls are unchanged.

Validation includes route ancestry and section link tests, rendered sidebar/header consistency at every registered route, TypeScript, lint, unit tests and production build. A live browser click-through requires an available browser connection; it was unavailable in this session.
