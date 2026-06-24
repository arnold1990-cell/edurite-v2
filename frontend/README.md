# EduRite Frontend

React + TypeScript + Vite frontend for the EduRite AI-powered Student Career Guidance Platform.

## Stack
- React, Vite, TypeScript
- Tailwind CSS
- React Router
- React Hook Form + Zod
- TanStack Query
- Axios
- Lucide React icons

## Setup
```bash
npm install
npm run dev
```

Default API base URL is `/api`. In development, Vite proxies `/api/**` to `http://localhost:8080/api/v1/**`. In production, the frontend Nginx container proxies `/api/**` to the backend container's `/api/v1/**` routes.

## Google Sign-In (Local)
- Use a Google OAuth 2.0 `Web application` client ID.
- Set `VITE_GOOGLE_SIGNIN_ENABLED=true` and `VITE_GOOGLE_CLIENT_ID=<your-web-client-id>`.
- The browser client ID is read only from `VITE_GOOGLE_CLIENT_ID`.
- If needed, set `VITE_DEV_SERVER_PORT` (default `5173`) before running `npm run dev`.
- In Google Cloud Console, add your local frontend origin to `Authorized JavaScript origins`:
  - `http://localhost:5173` (or your configured `VITE_DEV_SERVER_PORT`)
  - `http://127.0.0.1:5173` (optional fallback)
  - `http://localhost:5173` and `http://127.0.0.1:5173` if another local app already occupies `5173`
- If Google popup shows `Error 401: invalid_client` with `no registered origin`, that exact origin is missing from the OAuth client.

## Architecture Highlights
- `src/app`: root app composition and providers
- `src/routes`: route guards (`RequireAuth`, `RequireRole`)
- `src/components`: shared UI, layout, feedback, table, and card components
- `src/pages`: public + role-based portal pages
- `src/features/auth`: auth context/store and JWT persistence
- `src/services`: typed API service modules mapped to Spring Boot endpoints
- `src/types`: shared models for API/domain contracts

## Role-based portals
- `STUDENT`: recommendations, profile, applications, subscription
- `COMPANY`: bursary management, applicants, talent search
- `ADMIN`: users, roles, moderation, analytics, audit logs

## Notes
- Axios interceptors include access token injection and refresh flow structure (`/auth/refresh`).
- Forms use React Hook Form and Zod validation.
- Layout is fully responsive with mobile sidebar toggle.
