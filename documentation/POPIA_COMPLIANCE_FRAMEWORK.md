# POPIA Compliance Framework (South Africa)

## Purpose
This document maps current platform capabilities to POPIA-aligned controls based on code evidence. It is not legal advice and should be validated by legal counsel before external use.

## Personal Data Collected (Observed)
- Identity data: name, email, phone number.
- Account/security data: password hash, verification state, login timestamps.
- Profile data: student profile fields (interests, location, DOB, qualification details, etc.).
- Company profile data: registration and contact details.
- Consent data: consent type/version/accepted timestamp (`consent_records`).
- Potentially uploaded documents: CV/transcripts/company documents.
- Payment-related records: payment/subscription entities.
- Notification and interaction data across platform modules.

## Purpose of Data Processing
- Account creation, authentication, and access control.
- Delivery of career guidance, recommendations, and education/work opportunity features.
- Compliance logging (consent recording).
- Service communication (notifications/OTP/password reset).
- Payment/subscription processing and status tracking.

## User Consent
Implemented:
- POPIA consent record is stored at registration via `ConsentService.recordPopiaConsent(...)`.
- Consent versioning supported via configurable `app.popi.consent-version`.

Not currently found in codebase:
- Evidence of consent withdrawal workflow/API.
- Evidence of granular consent preferences by processing purpose.

Recommended additions:
- Add consent withdrawal and preference management endpoints.
- Store and evidence collection context (IP/user-agent are fields in entity but not clearly populated in service).

## Data Retention
Not currently found in codebase:
- Explicit retention schedules per data category.
- Automatic purge/anonymization jobs.

Recommended additions:
- Define retention matrix (e.g., account, payment, consent, logs, uploads).
- Implement scheduled archival/deletion jobs and legal-hold exceptions.

## Access Control
Implemented:
- JWT auth with role-based route enforcement in `SecurityConfig`.
- Segmented authorities for student/school/teacher/company/admin flows.

Recommended additions:
- Formal least-privilege review and periodic access recertification process.
- Admin activity monitoring policy tied to audit retention.

## Data Security
Implemented:
- BCrypt password hashing.
- JWT signed tokens with configurable secret and expirations.
- CORS restrictions configured through allowlist.
- Input validation using Bean Validation (`@Valid`) and centralized exception handling.

Risk/gap:
- Secrets currently appear to be managed in `.env` file in workspace.

Recommended additions:
- Use managed secrets store (AWS Secrets Manager/SSM/Vault).
- Mandatory secret rotation policy and incident response runbook.

## User Rights (POPIA)
Partially present:
- Account deletion pathway exists (`AccountController` + account deletion audit repository).

Not currently found in codebase:
- Dedicated APIs/workflows for access requests, rectification requests, portability exports, objection/restriction processing timelines.

Recommended additions:
- Implement Data Subject Request (DSR) module with SLA tracking and audit trail.

## Data Sharing with Third Parties
Observed third-party processors/integrations:
- AI providers (Gemini/OpenAI/OpenRouter)
- OTP/telecom (Twilio)
- Payments (PayFast/PayPal)
- Jobs/content APIs (Adzuna/YouTube)

Recommended governance:
- Maintain processor inventory and lawful basis per integration.
- Enforce Data Processing Agreements (DPAs) and cross-border transfer safeguards.

## Privacy Policy Requirements
Should explicitly cover:
- Categories of personal data collected.
- Purpose and lawful basis for processing.
- Data sharing and cross-border transfer details.
- Retention periods.
- User rights and request channels.
- Security measures.
- Contact details of information officer.

Not currently found in codebase:
- Enforced linkage between policy version and accepted consent artifact beyond `consentVersion` string.

## Data Processing Agreement (DPA) Notes
For enterprise/partner readiness:
- Define controller vs operator roles.
- Include subprocessors list with notification obligations.
- Add breach notification timelines and assistance obligations.
- Include deletion/return of data on termination.

## Compliance Readiness Summary
- Foundational controls exist (consent capture, RBAC, auth, hashing, validation).
- Enterprise-grade POPIA program elements are not yet complete (retention, DSR operations, formal processor governance, documented transfer controls).
