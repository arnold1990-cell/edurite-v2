"""Release classification against actual deployed revisions; unknown history fails closed."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import zlib

MIGRATIONS = "backend/src/main/resources/db/migration/"
CLASSES = {"FRONTEND_ONLY", "BACKEND_NO_SCHEMA_CHANGE", "DATABASE_MIGRATION"}
PROTECTED = ("backend/pom.xml", "backend/src/main/java/com/edurite/auth/service/EmailVerificationService.java")


def git(root, *args):
    result = subprocess.run(["git", "-C", str(root), *args], capture_output=True, check=True)
    return result.stdout


def revision(root, sha):
    if not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise ValueError("A verified full deployed revision is required")
    if git(root, "rev-parse", sha + "^{commit}").decode().strip() != sha:
        raise ValueError("Unknown deployed revision")


def inventory(root, sha):
    files = git(root, "ls-tree", "-r", "--name-only", sha, "--", MIGRATIONS).decode().splitlines()
    result = []
    seen = set()
    for path in files:
        name = Path(path).name
        match = re.fullmatch(r"V([0-9]+(?:[._][0-9]+)*)__.+\.sql", name)
        if not match:
            raise ValueError("Unrecognized/repeatable migration requires explicit review")
        version = ".".join(str(int(part)) for part in re.split(r"[._]", match[1]))
        if version in seen:
            raise ValueError("Duplicate migration version")
        seen.add(version)
        data = git(root, "show", sha + ":" + path)
        # Flyway CRC32 ignores line endings and the leading UTF-8 BOM.
        checksum = 0
        for line in data.decode("utf-8-sig").splitlines():
            checksum = zlib.crc32(line.encode("utf8"), checksum)
        if checksum >= 2**31:
            checksum -= 2**32
        result.append(dict(version=version, script=name, checksum=checksum,
                           sha256=hashlib.sha256(data).hexdigest()))
    if not result:
        raise ValueError("Migration inventory missing")
    return sorted(result, key=lambda row: tuple(map(int, row["version"].split("."))))


def schema_sensitive(paths):
    return any(p.startswith(("database/", MIGRATIONS)) or
        (p.startswith("backend/") and ("/entity/" in p or "Flyway" in p or p.endswith((".yml", ".yaml", ".properties", ".sql"))))
        for p in paths)


def classify(root, commit, backend_base, frontend_base):
    for sha in (commit, backend_base, frontend_base):
        revision(root, sha)
    for base in (backend_base, frontend_base):
        subprocess.run(["git", "-C", str(root), "merge-base", "--is-ancestor", base, commit], check=True)
    old, new = inventory(root, backend_base), inventory(root, commit)
    by_name = {row["script"]: row for row in new}
    if any(by_name.get(row["script"]) != row for row in old):
        raise ValueError("Applied migration files were edited, renamed or removed")
    added = [row for row in new if row["script"] not in {r["script"] for r in old}]
    if added and min(tuple(map(int, r["version"].split("."))) for r in added) <= tuple(map(int, old[-1]["version"].split("."))):
        raise ValueError("Out-of-order migration requires manual reconciliation")
    backend_changes = git(root, "diff", "--name-only", backend_base, commit).decode().splitlines()
    frontend_changes = git(root, "diff", "--name-only", frontend_base, commit, "--", "frontend/").decode().splitlines()
    changes = sorted(set([p for p in backend_changes if not p.startswith("frontend/")] + frontend_changes))
    allowed_roots = ("frontend/", "backend/", "scripts/", ".github/", "docs/", "documentation/", "deploy/", "infra/", "database/", "ai-service/", "performance/")
    allowed_files = {"docker-compose.yml", "docker-compose.production.yml", "deploy.sh", "README.md", ".gitignore", ".gitattributes", ".editorconfig", "EDURITE_PRODUCT_ARCHITECTURE_FOR_LOVABLE.md"}
    if any(not p.startswith(allowed_roots) and p not in allowed_files for p in changes):
        raise ValueError("Unclassified path; review classification policy before deployment")
    diff = git(root, 'diff', '-U0', backend_base, commit, '--', 'backend/src/main').decode()
    schema_edits = bool(re.search(r'(?im)^[+-](?![+-]).*(?:\b(?:CREATE|ALTER|DROP|TRUNCATE)\s+(?:TABLE|INDEX|SCHEMA|TYPE|SEQUENCE)|@(?:Entity|Table|Column|JoinColumn)\b|ddl-auto|hibernate\.hbm2ddl)', diff))
    schema_change = schema_sensitive(changes) or schema_edits
    kind = "DATABASE_MIGRATION" if added or schema_change else (
        "FRONTEND_ONLY" if changes and all(p.startswith("frontend/") for p in changes)
        else "BACKEND_NO_SCHEMA_CHANGE")
    protected = {p: hashlib.sha256(git(root, "show", commit + ":" + p)).hexdigest() for p in PROTECTED}
    return dict(commit=commit, backend_base=backend_base, frontend_base=frontend_base,
                classification=kind, changes=changes, schema_edits=schema_edits, baseline_migrations=old, migrations=new, protected_files=protected)


def validate_history(rows, migrations, allow_pending=False):
    """Compare real Flyway history to the tested inventory without changing the DB."""
    if not rows or any(row.get("success") is not True or row.get("type") != "SQL" for row in rows):
        raise ValueError("Missing, failed or unsupported Flyway history")
    expected = {row["version"]: row for row in migrations}
    versions = []
    for row in rows:
        version = ".".join(str(int(p)) for p in re.split(r"[._]", str(row["version"])))
        match = expected.get(version)
        if not match or any(row.get(key) != match[key] for key in ("script", "checksum")) or version in versions:
            raise ValueError("Database migration checksum/history differs from tested release")
        versions.append(version)
    wanted = [r["version"] for r in migrations]
    if versions != wanted[:len(versions)] or (not allow_pending and versions != wanted):
        raise ValueError("Database has pending, missing or out-of-order migrations")
    return versions[-1]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--commit", required=True)
    parser.add_argument("--backend-base", required=True)
    parser.add_argument("--frontend-base", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    args.output.write_text(json.dumps(classify(root, args.commit, args.backend_base, args.frontend_base), indent=2) + "\n")


if __name__ == "__main__":
    main()
