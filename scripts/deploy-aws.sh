#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

print_failure_diagnostics() {
  echo "==> Deployment failure diagnostics"
  echo "pwd: $(pwd)"
  docker compose ps || true
  echo "==> Backend logs (last 200 lines)"
  docker compose logs backend --tail=200 || true
  echo "==> Frontend logs (last 100 lines)"
  docker compose logs frontend --tail=100 || true
}

trap 'print_failure_diagnostics' ERR

cd "$REPO_ROOT" || {
  echo "ERROR: Failed to enter repository root resolved from script path: $REPO_ROOT"
  exit 1
}

echo "==> Deployment diagnostics"
echo "pwd: $(pwd)"
ls -la
echo "whoami: $(whoami)"
echo "hostname: $(hostname)"
docker compose ps || true

if [[ ! -d ".git" ]]; then
  echo "ERROR: Expected a git repository at $REPO_ROOT but .git was not found."
  exit 1
fi

if [[ ! -f "docker-compose.yml" ]]; then
  echo "ERROR: Expected docker-compose.yml at $REPO_ROOT but it was not found."
  exit 1
fi

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
docker compose logs backend --tail=200

echo "==> Waiting for backend health"
for attempt in {1..30}; do
  if curl -fsS "$BACKEND_LOCAL_URL/actuator/health" >/dev/null; then
    echo "PASS backend health"
    break
  fi

  if [[ "$attempt" == "30" ]]; then
    echo "FAIL backend health"
    docker compose logs backend --tail=200
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
