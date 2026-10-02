# Student dashboard reference reproduction

StudentAppShell owns all authenticated `/student/*` routes. Dashboard and feature pages render content only. See `docs/student-route-audit.md` for the route audit.

## Reference geometry

- Desktop sidebar: 17.5% (224 px on the 1280 px reference viewport).
- Header: 61 px; dashboard gutters: 10 px.
- Hero/progress: 2.73:1, four quick-access cards, then a 1.47:1.25:1 lower grid.
- Tablet: two lower columns; navigation becomes a drawer at 900 px.
- Phone: stacked sections, two quick links per row. Drawer supports Escape, focus containment and body scroll locking.

## Existing data used

| Section | Existing API |
| --- | --- |
| Identity, grade, subjects, interests | `/student/profile`, existing auth context |
| Goals, milestones, application counts, rewards, readiness | `/student/dashboard` |
| Career matches and course recommendations | `/recommendations/me` |
| Overall progress and detailed progress cards | `/student/progress-score` |
| Learning resources | `/student/learning-centre/recommended` |
| Upcoming dated bursaries | `/bursaries` |
| School access and status | Existing `schoolService.getMySchoolStatus` |
| Notification indicator | Existing `notificationService.unreadCount` in DashboardLayout |

Course recommendations exclude improvement/upgrade suggestions. Recommendation IDs are not assumed to be detail-route IDs: cards open existing guidance or learning pages. Bursary cards use the actual catalog IDs and only display valid current/future deadlines returned by the catalog. They do not claim to list every available opportunity. No client-side demonstration data is shipped.

Missing marks are not inferred from NSC levels. Missing scores show a dash. Goals use actual objectives and next milestones; milestone completion is not invented. Empty and failed queries retain the section layout and display an explicit state.

## Image/data replacement points

- **Hero:** `classroomPhoto` import in `StudentDashboard.tsx` uses the existing `src/assets/edurite-classroom-login-bg.png`. The exact reference portrait is unavailable. Replace this import with an approved local production portrait; `.ed-hero-photo` controls the right-side crop. No image generation was used.
- **Brand:** shared `EduRiteLogo` component using `src/assets/branding/edurite-logo.png`.
- **Quick links/course art:** existing local career/course/bursary photos are decorative category images, not claimed to depict a specific recommended course or institution.
- **Career thumbnails:** neutral icons until the recommendation API supplies real thumbnail URLs.
- **Avatar:** authenticated initials; the existing User/StudentProfile contract has no avatar URL.
- **Learning thumbnails:** backend thumbnail URL when supplied; an icon otherwise.
- **Goals:** no separate completion checklist API exists; milestones remain uncompleted indicators rather than fabricated checkmarks.

Existing Basic/Premium readiness details, improvements, school portal access, and account features remain available in expandable sections below the reference grid.

## Verification

Production TypeScript/Vite build, unit tests, encoding and distribution checks are run with the normal frontend commands. Headless-browser checks use isolated API fixtures outside the application under ignored `build/reference-dashboard-backup/`; fixtures never replace production services. Tested widths: 1280, 1024, 768, 390 and 320 px. Screenshots in that directory are QA previews, not real learner records.

Live public API and frontend proxy connectivity are checked separately. Authenticated production learner data requires a real user session; mocked browser authentication tests do not establish that live end-to-end behavior.
