# Security Controls Evidence

## Password Hashing
Implemented:
- BCrypt password hashing via `PasswordEncoder` bean (`BCryptPasswordEncoder`) in `SecurityConfig`.
- Password changes/resets use encoder before persistence in `AuthService`.

## JWT / Session Handling
Implemented:
- Stateless authentication (`SessionCreationPolicy.STATELESS`).
- Access + refresh token model in `JwtService`.
- JWT signature validation and claim extraction in `JwtAuthenticationFilter`.
- Frontend auto-refresh flow in `apiClient.ts`.

Security caveat:
- Tokens are stored in browser storage (`localStorage`/`sessionStorage`), which is vulnerable to XSS token theft if frontend compromise occurs.

Recommended enhancement:
- Evaluate secure HttpOnly cookie approach with CSRF strategy for higher assurance.

## Role-Based Access Control (RBAC)
Implemented:
- Route-level authority checks in `SecurityConfig` for student, company, admin, school admin, teacher, school student, and authenticated-only endpoints.
- Frontend route guards (`RequireAuth`, `RequireRole`) align UX with backend policy.

## CORS
Implemented:
- Explicit CORS allowlist in `SecurityConfig` with configurable origins.
- Methods/headers/exposed headers constrained.

## HTTPS / SSL Assumptions
- Application supports forwarded headers (`server.forward-headers-strategy=framework`).
- TLS termination config is not present in repo (Nginx HTTP only).

Status:
- Not currently found in codebase: enforced HTTPS listener/cert config.

## Input Validation
Implemented:
- Bean Validation for request DTOs (`@Valid` in controllers).
- Centralized validation and error formatting in `ApiExceptionHandler`.
- Additional domain-level validation in services (e.g., phone normalization, role checks, business rules).

## API Protection
Implemented:
- JWT filter integration before username/password filter.
- Unauthorized and access-denied handlers configured.
- Security test coverage exists under `backend/src/test/java/com/edurite/security/...`.

## Database Protection
Implemented:
- PostgreSQL credentials via environment variables.
- JPA + Flyway schema governance.

Not currently found in codebase:
- At-rest encryption controls specification.
- Row-level security policies.
- Database activity monitoring policy docs.

## Environment Secret Handling
Partially implemented:
- Secrets externalized to environment variables in Spring/Compose configuration.

Risk evidence:
- Root `.env` file in workspace contains populated secrets.

Recommended controls:
- Immediate credential rotation.
- Move secrets to managed vault service.
- Prevent secret leakage with pre-commit and CI scanners.

## Audit / Logging
Implemented:
- Auth logging and error logging present.
- Admin audit log entities/repositories/controllers present.
- Consent records stored with version/timestamp.

Not currently found in codebase:
- Centralized SIEM/log retention policy.
- Tamper-evident audit pipeline documentation.

## Overall Security Posture (Code Evidence Summary)
- Strong baseline exists for application-layer security (hashing, JWT, RBAC, validation, controlled error responses).
- Most significant gaps for external audit readiness are secret governance, TLS hardening evidence, and formalized operations controls (backup/monitoring/audit retention).
