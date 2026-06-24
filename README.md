# EduRite

EduRite is an AI-powered career guidance platform.

- `backend/`: Spring Boot 3, Java 21 API
- `frontend/`: React, Vite, TypeScript app
- `docker-compose.yml`: production-style Postgres, Redis, backend, and Nginx-served frontend
- `backend/src/main/resources/db/migration/`: Flyway migrations

## Local Development

Create an environment file:

```bash
cp .env.example .env
```

For local Maven/Vite development, use localhost service URLs in `.env`:

```dotenv
SPRING_PROFILES_ACTIVE=dev
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/edurite
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
APP_FRONTEND_URL=http://localhost:5173
APP_BASE_URL=http://localhost:5173
BACKEND_BASE_URL=http://localhost:8080
VITE_API_BASE_URL=/api
```

Start database services:

```bash
docker compose up -d postgres redis
```

Run the backend:

```bash
cd backend
mvn spring-boot:run
```

Run the frontend:

```bash
cd frontend
npm install
npm run dev
```

Default URLs:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`

## Production Docker Compose

The root `docker-compose.yml` builds immutable backend/frontend images from the current checkout. It does not bind mount backend or frontend source into containers, so a redeploy uses the code from the pulled Git commit.

Services:

- `postgres`: PostgreSQL 16 with persistent `postgres_data`
- `redis`: Redis 7
- `backend`: Java 21 Spring Boot app on port `8080`
- `frontend`: Nginx serving the latest Vite `dist` build on host port `5173`

The frontend image builds with `npm run build` and Nginx proxies:

- `/api/**` to `backend:8080/api/**`
- `/api/v1/**` to `backend:8080/api/v1/**`
- `/oauth2/**` to `backend:8080`
- `/login/oauth2/**` to `backend:8080`
- `/actuator/health` to `backend:8080`

## Required Environment

Copy `.env.example` to `.env` on the server and replace placeholders. Do not commit `.env`.

Required production values:

- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `GEMINI_API_KEY` if live Gemini guidance is enabled
- `VITE_GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_ID` when Google sign-in is enabled
- `GOOGLE_CLIENT_SECRET` only if you later enable a server-side OAuth code flow
- Twilio values if `OTP_ENABLED=true`
- PayFast values if `PAYMENT_PROVIDER=payfast`

For production on `edurite.net`, keep:

```dotenv
APP_BASE_URL=https://edurite.net
APP_FRONTEND_URL=https://edurite.net
BACKEND_BASE_URL=https://edurite.net
VITE_API_BASE_URL=/api
APP_CORS_ALLOWED_ORIGINS=https://edurite.net,https://www.edurite.net,http://edurite.net,http://www.edurite.net
GOOGLE_REDIRECT_URI=https://edurite.net/login/oauth2/code/google
```

Google Sign-In in the React app reads the browser client ID only from `VITE_GOOGLE_CLIENT_ID`.

## AWS Deployment

On EC2:

```bash
cd ~/ai-career-guidance
git pull origin main
chmod +x scripts/deploy-aws.sh scripts/smoke-test.sh
./scripts/deploy-aws.sh
./scripts/smoke-test.sh
```

The deploy script pulls `main`, stops the old stack, prunes unused Docker resources and anonymous volumes, removes the old `edurite-backend:latest` and `edurite-frontend:latest` images, rebuilds with `--no-cache`, starts, prints status, shows the last 50 backend log lines, and tests backend/frontend health.

## Verify Deployed Commit

```bash
git rev-parse --short HEAD
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
```

Expected signs of a clean deployment:

- `docker compose ps` shows `edurite-postgres`, `edurite-redis`, `edurite-backend`, and `edurite-frontend` as running/healthy.
- `curl http://localhost:8080/actuator/health` returns JSON with `"status":"UP"`.
- `curl -I http://localhost:5173` returns `HTTP/1.1 200 OK`.
- `curl -I http://localhost:8080/api/student/dashboard` returns `401`, proving protected routes are secured and routed.

## Useful Checks

Backend:

```bash
cd backend
mvn test
```

Frontend:

```bash
cd frontend
npm run lint
npm run build
```

Compose:

```bash
docker compose config
docker compose build --no-cache
docker compose up -d
```
