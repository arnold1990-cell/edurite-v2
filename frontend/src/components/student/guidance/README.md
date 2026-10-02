# Personalised AI Guidance

`/student/recommendations/careers` renders `AiGuidance` inside the existing `StudentAppShell`. The content component has no application navigation, sidebar or header. All authenticated student routes share StudentAppShell; DashboardLayout is used by other portals.

The existing page retains Fast/Deep request handling, subscription checks, Basic career limits, loading/errors and full source-backed results. `AiGuidance` organises those results into insights, careers, qualification readiness and actual recommended next steps. The existing qualification evaluator is reused unchanged; matching parsed subjects is explicitly distinguished from confirmed admission eligibility.

Readiness uses the backend profile-completion percentage and profile fields. Assessment status uses `psychometricService.latestStudent`. Missing information is never converted into invented scores, recommendations or action steps. Programme details remain Premium-gated. Full requirements, warnings and source diagnostics remain available in expandable guidance details.

`RecommendedCareerCard` is shared with the exploration component. `ProgressRing` and the Student shell are reused from the Dashboard. CSS is scoped to guidance content; there is no separate mobile navigation.

Validation: production build, TypeScript, UTF-8 validation and 18 unit tests. Browser fixtures check one sidebar/header, active AI Guidance navigation, empty state, completion percentage, Fast/Deep payloads, Premium restrictions, careers/universities, parsed subject checks, manual-verification states, failures, mobile drawer and guest redirects. Layout widths: 1280, 1024, 768, 390 and 320 pixels.

QA screenshots in ignored `build/ai-guidance-backup/` use isolated test data. Live authenticated AI responses and backend persistence were not exercised by these fixture checks. No backend or API contracts were changed.
