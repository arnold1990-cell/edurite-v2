# Student profile reference layout

`StudentProfileView.tsx` supplies the reference-style hero, six tabs, readable overview cards, completion ring and quick actions. `student-profile.css` extends the dashboard's scoped shell and palette. `StudentPages.tsx` retains the existing profile business logic and provides the editor, readiness, guidance, document and saved-version sections. Other role dashboards are unchanged by this profile work.

## Data and editing

- Identity comes from the existing authenticated context and `studentService.getMe`; school information uses `schoolService.getMySchoolStatus`.
- Profile saves retain `studentService.updateMe` and its existing payload. FET/senior-phase subjects, NSC levels 1–7, readiness formulas, Premium gating and qualification evaluation remain in the existing page.
- The completion ring uses the backend's `profileCompleteness`. Checklist items indicate whether their stored fields are present; they do not recompute the backend percentage.
- Career guidance retains `/ai/analyse-university-sources` and the recommendation fallback. Guidance includes unmet requirements, manual-verification states and alternatives.
- CV/transcript uploads retain the multipart `file` APIs. Saved versions retain list/detail/create/apply/delete services.
- Unsaved edits survive tab changes and background query refreshes. Successful saves/applications update the profile cache and invalidate existing derived data.
- Legacy academic, document, qualification and experience URLs select the relevant tab. No duplicate routes were introduced.

## Assets and missing data

The exact reference portrait is unavailable. The hero's `banner` import uses existing `src/assets/images/careers.jpeg`; replace this import with an approved local production banner. No generated artwork or fake student photos were added. The profile contract currently has no avatar/upload API, so the avatar uses initials.

Interests and skills come from saved data with decorative icons. Academic cards show stored percentages only when supplied, otherwise the actual NSC level. Empty fields display incomplete states. There is no fabricated biography, location, goal, mark or completion score.

## Verification

Production build, TypeScript, encoding checks and all 18 unit tests pass. Stateful browser checks in ignored `build/profile-reference-backup/` exercise personal saves, subject add/remove/save/reload, guidance/readiness, cross-tab drafts, failed-save feedback, multipart uploads, version create/apply/delete and legacy URLs. Overview widths tested: 1280, 1024, 768, 390 and 320 px; all six tabs checked at 390 px.

Browser screenshots and mutation tests use isolated API fixtures, not real learner accounts. They verify frontend behavior and contract usage, not authenticated end-to-end persistence against the live backend. Public backend/proxy connectivity is checked separately.
