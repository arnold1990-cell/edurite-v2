#!/usr/bin/env bash
set -u

failures=0
BACKEND_LOCAL_URL="${BACKEND_LOCAL_URL:-http://localhost:${BACKEND_HOST_PORT:-8080}}"
FRONTEND_LOCAL_URL="${FRONTEND_LOCAL_URL:-http://localhost:${FRONTEND_HOST_PORT:-5173}}"
PUBLIC_BASE_URL="${PUBLIC_BASE_URL:-http://edurite.net}"

pass() {
  echo "PASS $1"
}

fail() {
  echo "FAIL $1"
  failures=$((failures + 1))
}

status_code() {
  local url="$1"
  local follow="${2:-false}"
  if [[ "$follow" == "true" ]]; then
    curl -L -sS -o /dev/null -w "%{http_code}" --max-time 20 "$url"
  else
    curl -sS -o /dev/null -w "%{http_code}" --max-time 20 "$url"
  fi
}

head_status_code() {
  local url="$1"
  curl -I -sS -o /dev/null -w "%{http_code}" --max-time 20 "$url"
}

body_contains_html() {
  local url="$1"
  local follow="${2:-false}"
  if [[ "$follow" == "true" ]]; then
    curl -L -fsS --max-time 20 "$url" | grep -qi "<html"
  else
    curl -fsS --max-time 20 "$url" | grep -qi "<html"
  fi
}

check_head_status() {
  local label="$1"
  local url="$2"
  local expected="$3"
  local actual
  actual="$(head_status_code "$url" || true)"
  if [[ "$actual" == "$expected" ]]; then
    pass "$label returned $expected"
  else
    fail "$label expected $expected, got ${actual:-no response}"
  fi
}

check_status() {
  local label="$1"
  local url="$2"
  local expected="$3"
  local follow="${4:-false}"
  local actual
  actual="$(status_code "$url" "$follow" || true)"
  if [[ "$actual" == "$expected" ]]; then
    pass "$label returned $expected"
  else
    fail "$label expected $expected, got ${actual:-no response}"
  fi
}

check_status_any() {
  local label="$1"
  local url="$2"
  local follow="${3:-false}"
  shift 3 || true
  local actual
  actual="$(status_code "$url" "$follow" || true)"
  for expected in "$@"; do
    if [[ "$actual" == "$expected" ]]; then
      pass "$label returned $actual"
      return
    fi
  done
  fail "$label expected one of [$*], got ${actual:-no response}"
}

check_html() {
  local label="$1"
  local url="$2"
  local follow="${3:-false}"
  if body_contains_html "$url" "$follow"; then
    pass "$label returned frontend HTML"
  else
    fail "$label did not return frontend HTML"
  fi
}

check_health() {
  local label="$1"
  local url="$2"
  local response
  response="$(curl -fsS --max-time 20 "$url" || true)"
  if [[ "$response" == *'"status":"UP"'* || "$response" == *'"status": "UP"'* ]]; then
    pass "$label returned healthy"
  elif [[ -n "$response" ]]; then
    pass "$label was accessible"
  else
    fail "$label was not accessible"
  fi
}

check_health "Local backend health" "$BACKEND_LOCAL_URL/actuator/health"
check_status "Local protected API" "$BACKEND_LOCAL_URL/api/student/dashboard" "401"
check_head_status "Local Google OAuth route" "$BACKEND_LOCAL_URL/oauth2/authorization/google" "302"
check_html "Local frontend" "$FRONTEND_LOCAL_URL"

check_html "Domain frontend" "$PUBLIC_BASE_URL" "true"
check_status "Domain protected API" "$PUBLIC_BASE_URL/api/student/dashboard" "401" "true"

if [[ "$failures" -eq 0 ]]; then
  echo "PASS smoke test complete"
  exit 0
fi

echo "FAIL smoke test complete with $failures failure(s)"
exit 1
