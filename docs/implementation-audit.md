# EduRite Implementation Audit (Pre-change)

## Existing modules found
- **Auth/Security/User**: JWT auth, role-based guards, refresh flow, role seeding.
- **Student**: profile, saved careers/bursaries, documents, dashboard, settings.
- **Career**: searchable careers module (backend API + frontend pages).
- **Bursary**: listing/search + recommendations and company bursary management.
- **Recommendations/AI Guidance**: recommendation endpoint + Gemini-based university source analysis.
- **Notifications**: in-app notification storage, scheduler/events, student/company pages.
- **Applications/Subscriptions/Admin/Company**: core CRUD/workflows present.

## Fully implemented modules
- Authentication and role enforcement.
- Student profile and basic recommendation flows.
- Bursary workflows (student + company + moderation).
- Notification read/list lifecycle.

## Partially implemented modules
- **Institutions/Universities**:
  - DB table exists (`institutions`) but no backend JPA/controller/repository module.
  - Frontend service and public page exist, but page is table-only and lacks official website links and SA public university completeness guarantees.
- **AI career guidance extensions**:
  - AI guidance exists, but advanced modules (personality tests, roadmap generator, CV analysis, labour insights, internship/jobs) are not fully represented as dedicated domain modules.

## Missing modules (from requested scope)
- Dedicated backend universities/institutions API module.
- Seed/update logic ensuring all 26 SA public universities with official website URLs.
- Student-facing universities page with featured carousel/cards and external official links.

## Existing university-related model/API/frontend components
- `institutions` table in baseline schema with `name` + `location` only.
- Frontend `institutionService` calling `/institutions` and `/institutions/:id`.
- Public `InstitutionsPage` currently renders a basic table.
- No backend `/api/v1/institutions` endpoint currently implemented.

## Planned changes
1. Add backend institution module (entity/repository/controller/DTO) mapped to existing `institutions` table.
2. Add Flyway migration to extend institutions schema with website + metadata fields and seed/update all 26 SA public universities without duplicates.
3. Add student universities page with:
   - featured carousel-style section,
   - full universities grid,
   - clickable external links (`target="_blank"`),
   - graceful unavailable state.
4. Wire student route + sidebar navigation, and update shared frontend types as needed.
