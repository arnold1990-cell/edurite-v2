#!/usr/bin/env bash
set -euo pipefail
# Arguments name a tested staged release, backup and separate restore evidence.
exec python3 "$(dirname "$0")/scripts/release.py" deploy "$@"
