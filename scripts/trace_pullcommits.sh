#!/usr/bin/env bash
set -euo pipefail

# Trace merge pull-request commit messages and remove the word "codex".
# Usage:
#   scripts/trace_pullcommits.sh                # prints sanitized messages
#   scripts/trace_pullcommits.sh --write FILE   # writes sanitized messages to FILE

write_file=""
if [[ "${1:-}" == "--write" ]]; then
  write_file="${2:-}"
  if [[ -z "$write_file" ]]; then
    echo "Missing output file path for --write" >&2
    exit 1
  fi
fi

sanitized=$(git log --grep='^Merge pull request' --pretty=format:'%H|%s' \
  | sed -E 's/[Cc]odex\/?//g; s/[[:space:]]+/ /g; s/[[:space:]]$//')

if [[ -n "$write_file" ]]; then
  printf '%s\n' "$sanitized" > "$write_file"
  echo "Wrote sanitized pull-commit trace to $write_file"
else
  printf '%s\n' "$sanitized"
fi
