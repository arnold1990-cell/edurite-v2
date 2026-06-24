# Technology Stack

## Programming Languages
- Java 21: Backend service implementation, domain logic, API layer.
- TypeScript: Frontend application logic and type-safe APIs.
- SQL: Flyway migrations for schema and seed evolution.
- YAML: Spring and CI/CD configuration.
- JavaScript (tooling/runtime): Frontend build toolchain and package ecosystem.
- Bash/Shell: Deployment and smoke-test scripts.

## Frameworks and Core Libraries
- Spring Boot: Backend application framework, dependency injection, REST hosting.
- Spring Security: JWT-based authentication/authorization and route security.
- Spring Data JPA: ORM and repository abstraction for PostgreSQL.
- Flyway: Controlled schema migration/versioning.
- Spring Validation: DTO validation and API input guarding.
- Springdoc OpenAPI: API docs and Swagger UI generation.
- React: Frontend UI framework.
- React Router: SPA routing and route protection.
- TanStack React Query: Query/mutation lifecycle and caching.
- Axios: HTTP client for API communication.
- React Hook Form + Zod: Structured forms and validation.
- Tailwind CSS + PostCSS + Autoprefixer: Utility-first styling and CSS pipeline.
- Framer Motion: UI motion/animation.

## Databases and Data Services
- PostgreSQL 16: Primary relational datastore for application records.
- Redis 7: Cache/fast data access and potentially transient shared state.

## Build and Packaging Tools
- Maven: Backend build, dependency management, tests.
- Vite: Frontend development server and production build.
- TypeScript Compiler (`tsc`): Static typing and build-time checks.
- npm: Frontend dependency management and scripts.

## Docker and Container Tools
- Docker Engine + Docker Compose: Multi-service runtime orchestration.
- Backend image: Multi-stage Maven build to Temurin JRE runtime.
- Frontend image: Node build stage + Nginx runtime stage.
- Official images used: `postgres:16`, `redis:7`, `nginx:1.27-alpine`, `node:20-alpine`, `maven:3.9.9-eclipse-temurin-21`, `eclipse-temurin:21-jre`.

## Cloud and Hosting Tools
- AWS EC2 (assumed from deploy scripts/workflow): Host for Docker Compose deployment.
- GitHub Actions: CI and deployment orchestration.
- SSH-based deployment action (`appleboy/ssh-action`): Remote command execution on target host.

## Authentication and Identity Tools
- JWT (`jjwt`): Stateless access and refresh token issuance/validation.
- BCrypt: Password hashing strategy.
- Google OAuth/OIDC integration: Social sign-in path support.
- Twilio Verify: OTP verification workflow.

## External Integrations (APIs/Services)
- Gemini API: Primary AI provider.
- OpenAI API: AI fallback/alternate provider.
- OpenRouter API: Additional AI provider routing.
- PayFast: Payment checkout and callback flow.
- PayPal: Alternative payment provider implementation.
- Adzuna API: Jobs aggregation/search.
- YouTube Data API: Learning/video search support.
- WhatsApp webhook integration: Inbound message handling.

## Why This Stack Is Used
- Fast full-stack delivery: React + Spring Boot + Docker Compose keeps development and deployment straightforward.
- Strong backend governance: Spring ecosystem provides mature security, validation, data access, and observability primitives.
- Reliable data operations: PostgreSQL + Flyway supports transactional integrity and schema lifecycle control.
- Modular growth path: Current modular monolith layout allows gradual extraction to services if scale/ownership requires it.
- Multi-provider resilience: AI and payments abstractions reduce single-vendor dependency risk.
