#!/usr/bin/env bash
set -euo pipefail

echo "==> Current directory: $(pwd)"

BACKEND_LOCAL_URL="${BACKEND_LOCAL_URL:-http://localhost:${BACKEND_HOST_PORT:-8080}}"
FRONTEND_LOCAL_URL="${FRONTEND_LOCAL_URL:-http://localhost:${FRONTEND_HOST_PORT:-5173}}"

echo "==> Pulling latest code from origin/main"
git pull origin main
echo "==> Deploying commit $(git rev-parse --short HEAD)"

echo "==> Stopping existing containers"
docker compose down

echo "==> Building fresh images without cache"
docker compose build --no-cache

echo "==> Starting containers"
docker compose up -d

echo "==> Container status"
docker compose ps

echo "==> Backend logs (last 100 lines)"
docker compose logs backend --tail=100

echo "==> Waiting for backend health"
for attempt in {1..30}; do
  if curl -fsS "$BACKEND_LOCAL_URL/actuator/health" >/dev/null; then
    echo "PASS backend health"
    break
  fi

  if [[ "$attempt" == "30" ]]; then
    echo "FAIL backend health"
    docker compose logs backend --tail=100
    exit 1
  fi

  sleep 3
done

echo "==> Testing frontend"
if curl -fsS "$FRONTEND_LOCAL_URL/" | grep -qi "<html"; then
  echo "PASS frontend HTML"
else
  echo "FAIL frontend HTML"
  docker compose logs frontend --tail=100
  exit 1
fi

echo "==> Final curl checks"
printf "Backend health: "
curl -fsS "$BACKEND_LOCAL_URL/actuator/health"
printf "\nProtected API status: "
curl -sS -o /dev/null -w "%{http_code}\n" "$BACKEND_LOCAL_URL/api/student/dashboard"
printf "OAuth route status: "
curl -I -sS -o /dev/null -w "%{http_code}\n" "$BACKEND_LOCAL_URL/oauth2/authorization/google"
printf "Frontend status: "
curl -sS -o /dev/null -w "%{http_code}\n" "$FRONTEND_LOCAL_URL/"

echo "==> Deploy complete"
