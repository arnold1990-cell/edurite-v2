# Hosting and Infrastructure

## Hosting Provider Assumptions
- AWS EC2 is the current deployment target assumption based on:
- `.github/workflows/deploy.yml` (SSH deployment using AWS host/user/SSH key secrets).
- `scripts/deploy-aws.sh`.

## Server Setup
- Host-level Docker + Docker Compose expected.
- Deployment pattern:
1. Pull latest `main`.
2. Build backend/frontend images (`--no-cache`).
3. Start stack with `docker compose up -d`.
4. Health-check backend and frontend.

## Docker Services
Defined in `docker-compose.yml`:
- `postgres` (`postgres:16`) with persistent volume `postgres_data`.
- `redis` (`redis:7`).
- `backend` (built from `backend/Dockerfile`, port 8080).
- `frontend` (built from `frontend/Dockerfile`, Nginx on port 80 exposed to host port 5173 default).

## Database Service
- PostgreSQL 16 container with configurable DB/user/password environment variables.
- Persistent storage via named volume.
- Backend Flyway migrations enabled by default.

## Reverse Proxy / Nginx
Implemented in `frontend/nginx.conf`:
- Serves SPA static content.
- Proxies `/api`, `/api/v1`, `/oauth2`, `/login/oauth2`, `/actuator/health` to backend container.
- Supports SPA fallback routing (`try_files ... /index.html`).

## SSL / HTTPS Setup
- Not currently found in codebase (Nginx listens on port 80 only; no TLS certificate directives).

Recommended addition:
- Add TLS termination (Nginx + Let’s Encrypt/ACM + load balancer) and HTTP-to-HTTPS redirect.

## Environment Variables
- Environment is loaded from root `.env` for compose runtime.
- Sensitive categories present include DB credentials, JWT secret, OAuth secrets, AI API keys, OTP keys, payment keys, and external API keys.

Security note:
- Do not store production secrets in plaintext files on host repositories; migrate to managed secret stores.

## Backup Recommendations
Recommended controls (not currently found in codebase):
- Scheduled PostgreSQL logical backups + point-in-time recovery strategy.
- Encrypted off-site backups (e.g., S3 with retention lifecycle).
- Backup restore drills and documented RTO/RPO.
- Optional Redis persistence strategy based on criticality.

## Estimated Infrastructure Components (Placeholder)
| Component | Example Sizing | Cost Placeholder (Monthly) |
|---|---|---|
| EC2 application host | 2-4 vCPU, 8-16 GB RAM | TBD |
| Block storage (EBS) | 100-300 GB SSD | TBD |
| Backup storage | S3 or equivalent | TBD |
| Domain + DNS | Managed DNS zone | TBD |
| TLS certificates | Let’s Encrypt or managed certs | TBD |
| Monitoring/logging | Cloud-native or third-party | TBD |

## Operational Gaps Before Production Audit
- No explicit IaC found (`infra/README.md` is TODO).
- No explicit HTTPS enforcement config in repo.
- No explicit backup automation scripts/policies in repo.
- No explicit centralized observability stack configuration in repo.
