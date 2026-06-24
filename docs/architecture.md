# EduRite Architecture (Phase 0)

EduRite starts as a **modular monolith** to optimize developer velocity while preserving module boundaries for future extraction.

## Architectural style

- Single Spring Boot deployable backend.
- Domain-oriented packages as internal modules.
- Shared infrastructure managed centrally (`config`, `security`, `common`).
- External dependencies (PostgreSQL, Redis) provisioned with Docker Compose.

## Why modular monolith first

- Faster iteration for early product discovery.
- Lower operational complexity than microservices.
- Clear migration path to services once domains and load patterns stabilize.

## Phase 0 scope

- Project scaffolding and module boundaries.
- Environment-specific Spring configuration.
- Initial database migration placeholders with Flyway.
- Containerized local infrastructure.

## Next phases (TODO)

- Define explicit module APIs and dependency rules.
- Add authentication and authorization flows.
- Introduce observability and health checks.
- Add CI pipelines and quality gates.
