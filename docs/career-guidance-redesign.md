# AI Career Guidance: portal styling and generation lifecycle

For the latest working-tree, commit and deployment assessment, see the [repository audit](repository-audit.md). Test totals below describe the earlier implementation/verification pass.

## UI and routes

The existing `/student/career-match` implementation remains the central page.
`/student/career-guidance` and the legacy AI route open that same implementation.
The assessment remains available at `?step=assessment`. The existing authenticated
`StudentAppShell`, navigation drawer, header, notifications, account menu, search,
and section navigation are unchanged.

The page now inherits the portal font and title hierarchy: Arial from the student
shell, its 24px desktop / 21px mobile page title, and standard card/body typography.
It uses the existing `--ed-orange`, `--student-border` and `--student-muted` tokens,
white cards and the shell's light-gray neutral backgrounds. The large decorative
brain, floating icons, peach gradients, brown headings and burnt-orange actions
were removed. Cards use the portal's 9px radius, restrained 12–16px spacing, and
ordinary orange primary actions. There is no second layout or navigation system.

The supplied messages describe reference screenshots but contain no attached
images. The existing portal source is therefore the visual authority; exact
screenshot comparison has not been performed.

## Generation behavior

1. **Initial:** only the compact header, three real-data summary cards, usage
   information and Generate AI Guidance action render. Catalogue suggestions are
   not displayed as AI guidance. There is no result count or results heading yet.
2. **Generating:** a synchronous in-flight guard prevents two submissions before
   React updates the disabled button. An explicit mutation calls the existing
   `/ai/career-advice/me` service. Automatic mutation retries are disabled. All
   generated content stays unmounted until a valid successful response arrives.
   Offline requests fail through the error UI instead of queueing a chargeable
   generation for a later reconnect.
3. **Success:** the complete response is checked for a nonempty career array,
   nonblank career names and reasons, and correctly typed improvement lists.
   The actual returned recommendation count is displayed. No ranking weights or
   AI percentages are shown. Results are labelled as generated in this session.
4. **Error:** network/provider, authentication, subscription, validation and rate
   limit failures show appropriate feedback with deliberate retry/navigation
   actions. Empty or malformed responses cannot appear as successful guidance.
   Failed regeneration hides the old response rather than presenting it as fresh.
5. **Refresh:** Refresh Recommendations invokes the same explicit guarded
   generation action. A background usage refresh does not clear valid results.
   Academic/profile changes invalidate the result's evidence scope. Account
   changes remount the generation owner, preventing a late response from a prior
   account appearing in the new account's page.

Generation requires a loaded profile, checked subscription access, remaining AI
usage, and usable saved evidence. Academic results, interests, skills, goals and
optional assessment evidence are supported. A missing transcript alone does not
block a learner with other usable evidence. Quotas, counts and dates come from
the backend; no allowances are hardcoded in production.

## Real data and existing features

- The academic card uses the authenticated profile's identity, grade, actual
  subject count, recorded percentage average and server completion percentage.
- Transcript status comes from the saved file and extraction/review history.
  Extraction messages, subject names and general APS are shown only when returned.
  No transcript displays “Transcript not uploaded”. The upload action opens the
  existing Documents route, preserving its PDF/DOC/DOCX validation and feedback.
- AI generation reuses `aiGuidanceService.getPersonalisedCareerAdvice`, the
  existing Spring controller/service, Gemini context, DTOs, entitlement policy,
  usage reservation and error normalization. This correction changes no backend
  API or authentication behavior.
- Only after generation succeeds does catalogue enrichment run. Exact title
  matches provide real catalogue descriptions and saved-career IDs. Unmatched
  AI suggestions link to the existing Career Explorer rather than inventing IDs.
- Result cards, filters, search, comparison and returned improvement actions use
  actual generated careers. No empty generated university, funding, pathway or
  learning-plan sections are mounted automatically.
- Explicitly opening a career still provides the existing detail component,
  programme requirement lookup, university logos, saved career controls, and
  links to Career Explorer, Study Options, study plans and the funding directory.
  Its on-demand tabs distinguish real records, saved roadmap snapshots and
  unavailable data. It does not present unavailable information as generated fact.

## Persistence boundary

Inspection found no endpoint for retrieving a student's persisted successful
career-advice response. `GET /ai/career-advice/me` generates guidance and can use
an AI interaction; it must not run automatically on mount as a saved-result fetch.
Saved career IDs and saved roadmaps are existing, separate features and remain
available through their existing APIs. Current-session guidance is not represented
as saved guidance. No tables, migrations or browser storage persistence were added.

## Tests and verification boundary

The generation tests now render React components with the real TanStack mutation
implementation and invoke button handlers. They cover initial visibility, pending
visibility, same-tick double clicks, valid and invalid responses, network/provider
and HTTP errors, retry, failed refresh, quota/data/access blocks, assessment-only
evidence, background usage updates, profile edits, cross-account late responses,
assessment routing and React console errors during tested generation interactions.
The AI transport and query inputs are test doubles; these tests do not assert live
Gemini model quality or live database persistence.

`react-test-renderer` and its types were added as development-only dependencies,
matching the existing React 18 version. Existing catalogue/model, saved-career,
programme comparison, detail tab, routing and student-shell tests are retained.
The standard lint, encoding validation, complete frontend suite and production
build were run, alongside focused backend evidence, contract, transcript and
entitlement checks. Results are recorded in the final response and ignored logs
under `build/career-guidance-corrections-*.log`.

Verification results: all 209 frontend tests passed across 25 files, including
26 generation-flow interaction tests. All 48 focused backend tests passed with
zero failures/errors/skips. Lint/TypeScript, UTF-8 validation and the production
build passed. Existing Browserslist-data and large-bundle warnings remain.

Computer-use inventory returned no browser or app surfaces. Therefore interactive
browser screenshots, visual mobile QA, drawer gestures and live authenticated
provider/persistence verification remain unverified in this session. Route and
shell consistency are covered by existing automated tests. No new claims of
persisted AI-guidance retrieval or exact screenshot matching are made.

## Files changed in the correction pass

- `frontend/src/pages/student/StudentCareerMatchPage.tsx`
- `frontend/src/pages/student/StudentCareerMatchPage.test.tsx`
- `frontend/src/components/student/guidance/useCareerGuidance.ts`
- `frontend/src/components/student/guidance/career-guidance.css`
- `frontend/package.json`
- `frontend/package-lock.json`
- `docs/career-guidance-redesign.md`

Earlier uncommitted guidance components, service additions, route compatibility
changes and the Gemini prompt improvements were preserved. No production data,
database schema, deployment workflow or AWS infrastructure was changed. No commit,
push or deployment was performed.
