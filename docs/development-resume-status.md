# Development inspection — 15 October 2026

Baseline: clean working tree at `443965b207a841c24ec58bc648549028cec9e419`.
Remote HEAD/main was checked with `git ls-remote` and matches; no remote tags were returned.
GitHub release metadata and Actions results have not been authenticated/inspected.

| Area | Repository evidence and remaining work |
| --- | --- |
| Completed implementation | October 14 commits contain catalogue career selection, programme APS metadata, learner-owned saved roadmaps and study-plan history/progress, Career Match evidence and assessment history, academic persistence and profile associations. Preserve these services and routes. |
| Partially verified | AI tutor, study plans, admission evidence, Basic/Premium trial and complimentary PRO have service/controller tests; live provider and complete browser journey still require validation. |
| Broken safety controls | Startup unconditionally repairs Flyway history. Deployment resets production checkout, builds on EC2 and starts all services. CI omits frontend tests and silently skips explicitly gated database suites. |
| Backend integration gaps | Production SES changes/backups unavailable locally; production IAM, SES delivery and live AI remain unverified. Database tests need explicit isolated URLs. |
| Frontend integration gaps | Existing buttons/routes/selectors are implemented. Real browser-to-provider journey remains unverified; no duplicate dashboard is needed. |
| Deployment blockers | No tested production restore, no verified registry images, no production configuration/connectivity evidence, no reconciled production-only SES diff. |
| Required fixes | Remove automatic Flyway repair; enforce CI tests and skip reporting; publish immutable images; use manual protected deployment with backup restore evidence, ordered readiness checks and explicit application-only rollback. |

V75–V79 are already committed. V75 preserves metadata while extracting checkout payload;
V76 locks invitations and revokes only its linked manual-testing subscription; V77–V79
add nullable metadata/history fields. No recipient changes or extra grants are authorized.
Migration compatibility must be tested against both isolated fixtures and a restored V74
production copy before release. A fresh schema test alone cannot establish production preservation.
