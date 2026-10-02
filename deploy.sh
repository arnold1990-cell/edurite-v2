#!/bin/bash
set -euo pipefail

PROJECT_DIR="/opt/edurite-v2"

echo "=== Deploying EduRite V2 ==="

if [ ! -d "$PROJECT_DIR/.git" ]; then
    echo "ERROR: EduRite V2 repository not found at $PROJECT_DIR"
    exit 1
fi

cd "$PROJECT_DIR"

echo "=== Updating repository ==="
git fetch origin
git checkout main
git pull --ff-only origin main

if [ ! -f ".env" ]; then
    echo "ERROR: Production .env file not found."
    exit 1
fi

echo "=== Validating Docker Compose ==="
docker compose config > /dev/null

echo "=== Building containers ==="
docker compose build backend frontend

echo "=== Starting EduRite ==="
docker compose up -d

echo "=== Container status ==="
docker compose ps

echo "=== Deployment complete ==="
