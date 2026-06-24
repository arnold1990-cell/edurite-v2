# Platform Architecture

## System Overview
EduRite is implemented as a modular monolith platform with a React frontend, Spring Boot backend, PostgreSQL primary datastore, Redis cache/ephemeral data service, and containerized deployment via Docker Compose.

Core runtime components found in codebase:
- `frontend/`: Single-page app (React + TypeScript + Vite) served by Nginx.
- `backend/`: Java 21 Spring Boot API with domain modules (auth, student, company, school, subscription, AI, compliance, notifications, etc.).
- `postgres`: Relational system-of-record database with Flyway schema migrations.
- `redis`: Cache/data service configured through Spring Data Redis.
- `ai-service/`: Not currently found in codebase (placeholder only).

## Frontend Architecture
- Framework: React 18 + TypeScript + Vite.
- Routing: `react-router-dom` with guarded routes (`RequireAuth`, `RequireRole`, `RequireCompanyApproval`).
- State/session: Auth tokens and user profile persisted in browser `localStorage` / `sessionStorage` (`authStore.ts`).
- Data-fetching: Axios client (`apiClient.ts`) + React Query.
- Validation/forms: `react-hook-form` + `zod`.
- Delivery: Built static assets served by Nginx (`frontend/nginx.conf`), with reverse proxy to backend for `/api`, `/api/v1`, OAuth, and health paths.

## Backend Architecture
- Framework: Spring Boot 3.5.x, Java 21.
- Architectural style: Modular monolith (single deployable jar with module packages).
- Key modules present:
- Authentication and identity (`auth`, `security`, `user`).
- Student profile and recommendations (`student`, `recommendation`, `psychometric`, `roadmap`, `progress`).
- Institution/career/course/bursary domains.
- Company and admin workflows.
- Payments/subscriptions (`subscription`, PayFast/PayPal providers).
- Notifications and SSE stream endpoints.
- AI orchestration (`ai`) across Gemini/OpenAI/OpenRouter abstractions.
- Compliance (`compliance`) with consent recording.

## Database Architecture
- Primary DB: PostgreSQL 16 (`docker-compose.yml`).
- Access layer: Spring Data JPA repositories.
- Schema management: Flyway (`backend/src/main/resources/db/migration`).
- Entity baseline: UUID primary keys and audit timestamps (`BaseEntity`).
- Secondary data service: Redis 7 configured for application use.

## Authentication and Authorization Flow
1. User registers or logs in through `/api/v1/auth` or `/api/auth`.
2. Backend validates credentials/OTP/eligibility and issues JWT access + refresh tokens.
3. Frontend stores tokens in local or session storage based on remember-me behavior.
4. Axios request interceptor attaches `Authorization: Bearer <token>`.
5. `JwtAuthenticationFilter` validates token and sets Spring Security context.
6. `SecurityConfig` enforces route access by role/authority and endpoint classification.
7. Token refresh is handled via `/auth/refresh` when access token expires.

## API Structure
- Versioning pattern: Dual mapping supports both `/api/v1/...` and `/api/...`.
- API style: REST endpoints across controller modules.
- Documentation: OpenAPI/Swagger enabled (`/v3/api-docs`, `/swagger-ui.html`).
- Health endpoint: `/actuator/health`.

## Deployment Flow
1. GitHub Actions CI runs backend build/tests using ephemeral Postgres + Redis.
2. On `main` push, deploy workflow SSHs to AWS EC2 and runs Docker Compose build/up.
3. On host, `docker-compose.yml` builds backend/frontend images and starts all runtime services.
4. Nginx frontend container serves SPA and proxies backend routes.

## Text-Based Architecture Diagram
```text
[Browser]
   |
   v
[Nginx Frontend Container :80]
   |-- serves React static assets
   |-- proxies /api, /api/v1, /oauth2, /login/oauth2, /actuator/health
   v
[Spring Boot Backend :8080]
   |-- Auth/JWT/Security
   |-- Domain Modules (Student, Company, Admin, AI, Payments, etc.)
   |-- Flyway migrations
   |
   |--> [PostgreSQL 16]
   |
   |--> [Redis 7]
   |
   |--> [External Services: Gemini/OpenAI/OpenRouter, Twilio, PayFast/PayPal, Adzuna, YouTube, WhatsApp]
```
