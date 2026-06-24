#!/bin/bash
set -e

cd ~/edurite-ai-career-guidance-clean

git pull origin main

docker compose build --no-cache backend frontend
docker compose up -d

docker compose ps
