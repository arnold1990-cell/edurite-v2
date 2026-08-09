# Baseline Scalability Audit

Audit date: 2026-08-08

## Stack

- Frontend: React 18, TypeScript, Vite 8, Tailwind, React Router, TanStack Query, Nginx container for static delivery.
- Backend: Java 21, Spring Boot 3.5.13, Spring MVC, Spring Security, Spring Data JPA, Flyway, Actuator.
- Database: PostgreSQL 16 in Docker Compose, Flyway migrations in `backend/src/main/resources/db/migration`.
- Redis: Redis 7 in Docker Compose. Before changes, Redis was configured but app caching used local `simple` cache.
- Authentication: stateless JWT through `JwtAuthenticationFilter`; no server HTTP session dependency for API auth.
- Live updates: SSE notification stream at `/api/*/notifications/stream`; no STOMP WebSocket broker found.
- External APIs: Gemini, OpenAI, OpenRouter, Google OAuth/tokeninfo, Twilio Verify, Adzuna, PayFast/PayPal, OpenLibrary, Google Books, Open Trivia DB, YouTube.
- File storage: `StorageService` returns S3-style object URIs, but curriculum assets store binary content in PostgreSQL byte arrays.
- Deployment: Docker Compose with backend, frontend, PostgreSQL, Redis. No Kubernetes manifests existed before this pass.
- CI/CD: `.github` exists; no full production scaling pipeline was verified in this audit.

## Existing Test Baseline

- Backend: `mvn test` from `backend` passed. Result: 236 tests, 0 failures, 0 errors, 7 skipped. Duration: 42.350 s.
- Frontend: `npm test -- --run` from `frontend` passed. Result: 3 files, 14 tests, 0 failures. Duration: 570 ms.
- Frontend typecheck: `npm run typecheck` passed.

## Current Architecture

The application is a modular monolith backend with a separate SPA frontend. Backend instances can be made horizontally scalable for normal HTTP APIs because JWT auth is stateless when every replica shares the same `JWT_SECRET`. PostgreSQL is the durable source of truth. Redis is available for distributed rate limiting, distributed cache, and temporary coordination, but was underused before this work.

## Bottlenecks And Risks

- Database: many service paths call `findAll()` and filter in memory, especially admin, district, curriculum, and school analytics flows. These will not scale to large datasets.
- JPA loading: `User.roles` is EAGER. This is acceptable for auth reads but risky when loading many users.
- Pagination: some public APIs use pageable queries; many dashboard/admin/school flows return lists without hard limits.
- Indexes: many important indexes exist, but large-table access paths still need query-specific `EXPLAIN ANALYZE` against production-like data.
- Connection pool: Hikari values were defaulted, not environment-tuned.
- Threading: Tomcat, async, and scheduler pools were not explicitly bounded/configured for production capacity planning.
- Scheduled jobs: scheduled jobs run in every backend replica unless externally coordinated or disabled on non-worker replicas.
- Cache: local in-JVM cache in `LearningCentreService` prevents cross-node consistency and can grow per replica.
- SSE: emitters are stored in local JVM memory. Multi-node delivery requires sticky routing or an external event distribution design.
- Uploads: curriculum binary content in PostgreSQL increases DB size, backup cost, memory pressure, and network I/O.
- External APIs: AI/payment/search integrations need strict timeout, retry, and circuit-breaker policy per provider.
- Docker: backend previously ran as root and health checked only generic `/actuator/health`.
- Observability: health existed, but Prometheus metrics, request IDs, p95/p99 histograms, and custom SSE/background metrics were missing.

## Single Points Of Failure

- One PostgreSQL primary.
- One Redis instance in Compose.
- One backend and one frontend container in Compose.
- Scheduler execution tied to API nodes.
- SSE ownership tied to one JVM.
- Local development infrastructure cannot verify 500,000 active users.

## 500K Readiness Assessment

The original repository was not verified for 500,000 simultaneous active users. It had a feasible stateless auth foundation, but database access patterns, local SSE state, missing distributed rate limiting, missing production observability, and lack of distributed load generation blocked any credible 500K claim.
