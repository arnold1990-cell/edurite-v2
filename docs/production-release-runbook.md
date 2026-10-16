# Existing CI/CD pipeline: classified production releases

Updated locally on 15 October 2026. No production deployment was performed.
This extends ci.yml, deploy.yml and release.py; it does not add another deployment system.

## Release behavior

| Classification | Application containers replaced | Database gate | Approval |
| --- | --- | --- | --- |
| FRONTEND_ONLY | Frontend only | No SQL/Redis commands, backup or restore requirement | Automatic after successful main CI |
| BACKEND_NO_SCHEMA_CHANGE | Backend, then frontend (refreshes the proxy/upstream) | Exact live Flyway versions, filenames and checksums must match the tested inventory | Automatic after successful main CI |
| DATABASE_MIGRATION | Backend, then frontend | Recent actual backup, isolated restore/migrate/preservation evidence bound to commit, inventory and live source version | Required reviewers in production-migrations |

All paths preserve the existing production checkout, .env and SES backup files.
No path runs Compose down, rebuilds on EC2, restarts PostgreSQL/Redis, prunes
images, deletes volumes, repairs migration history or rolls back database data.

## CI and automatic handoff

Pushes (including main) and pull requests run the existing backend security/integration/
migration tests, frontend tests/typecheck/build, and release safety tests.
The two opt-in external AI tests remain optional; required database suites cannot
silently skip. Actionlint validates the two existing workflows.

Only a successful main push publishes GHCR images and a release-SHA artifact.
The backend image uses the JAR produced by Maven verification. The frontend keeps
the existing Google OAuth build arguments; the actual published frontend digest
is also pulled and checked for SPA routes/assets before the release artifact is
accepted. The artifact records both immutable image digests, migration inventory
(including Flyway checksums), SES source hashes, commit and CI run ID.

Successful CI completion triggers deploy.yml. The workflow independently checks
the CI repository, workflow path, push event, branch, result and current-main SHA.
It downloads that run's release artifact rather than accepting user-entered image
names, commit SHAs or CI IDs. A failed/PR/stale CI run cannot deploy.

A manual retry needs no inputs:

    gh workflow run deploy.yml --ref main

It selects the latest successful main-push CI run and still rejects it if it does
not match current main. Missing/expired artifacts require a new successful main CI
run; dispatching CI alone intentionally does not publish/deploy a new release.

## Classification and deployed baseline

CI classifies the push range for diagnostics. CD recomputes classification from
the backend AND frontend revisions actually running on EC2. It uses those images'
org.opencontainers.image.revision labels, not the production checkout's HEAD or
only the latest push. Thus a frontend commit following an undeployed migration
cannot hide that migration.

The classifier requires full reachable ancestor commits and a complete versioned
migration inventory. Edited/deleted/renamed/out-of-order/duplicate migrations,
repeatable/unrecognized migration names, nonancestor revisions and unknown paths
fail closed. New migrations, entity changes, Flyway/configuration/SQL changes and
schema-related annotations/DDL changes take the migration gate conservatively.
Other known backend/infrastructure changes take BACKEND_NO_SCHEMA_CHANGE.

The prepared plan includes the actual container/image inventory. release.py
rechecks it under the existing host deployment lock immediately before execution.
A changed host requires a fresh workflow run and classification.

An unlabeled legacy image is NOT assigned a guessed revision. Before enabling
automation, reconcile its actual source/build provenance and perform an explicitly
reviewed baseline rollout using the existing release process. This work did not
change or attest the current EC2 baseline. A database older than the target
migration inventory will not pass the no-schema compatibility gate.

## One-time GitHub/EC2 setup before enabling deployment

1. Keep the existing EC2_HOST, EC2_USERNAME, EC2_SSH_KEY and EC2_KNOWN_HOSTS secret
   names/values. Both production environments must be able to access them. If they
   are environment-scoped today, securely make the same existing values available
   to production-migrations as well; do not print or commit them.
2. Configure production for main-only automatic deployments WITHOUT required
   reviewers. Existing general production reviewers would still pause automatic
   releases. The prepare job uses this environment for read-only host inspection.
3. Configure production-migrations for main-only deployment, with actual required
   reviewers and administrator bypass disabled. The workflow queries this setting
   and fails if it is absent, unreadable or unprotected. A YAML environment name
   alone does not establish approval. Keep write/admin access tightly restricted.
4. Permit the workflow token to read Actions artifacts/environment configuration
   and CI to publish GHCR packages. EC2 needs existing read access to the images.
5. Retain the trusted known_hosts entry. Both SSH and SCP explicitly require strict
   host-key verification; there is no unattended ssh-keyscan trust bypass.
6. Retain Python 3, Docker/Compose with --wait, the existing Compose project,
   production .env and data-service containers. Keep uploaded documents on the
   reviewed persistent bind mount from docker-compose.production.yml.
7. Retain IAM-role SES delivery, af-south-1, info@edurite.co.za, the verification
   base URL, OAuth settings and production secrets. Nothing here changes them.

No GitHub environment setting, secret, registry permission or EC2 configuration
was changed locally. These setup requirements must be completed by the operator.

## Existing SES reconciliation, without a review per unchanged release

Backend releases retain the existing production SES reconciliation gate.
Frontend-only releases do not need it because they never replace the backend.

Review the production pom.xml, EmailVerificationService.java and their backup
files privately against the tested backend source. Preserve them. The host review
record now may bind the production-file hashes AND the tested source hashes:
an unchanged pair is reusable across ordinary backend releases. Changing either
requires a new explicit review. Legacy commit-specific records still work for
their exact commit.

After actually reviewing the tested release, record on the host:

    export STAGED_RELEASE=/opt/edurite-releases/SHA-DEPLOY_RUN_ID
    python3 - <<'PY'
    import hashlib, json, os
    from pathlib import Path
    root = Path('/opt/edurite-v2')
    manifest = json.loads((Path(os.environ['STAGED_RELEASE']) / 'release.json').read_text())
    names = ['backend/pom.xml', 'backend/src/main/java/com/edurite/auth/service/EmailVerificationService.java']
    record = {
        'files': {name: hashlib.sha256((root/name).read_bytes()).hexdigest() for name in names},
        'release_files': manifest['protected_files'],
    }
    path = Path('/opt/edurite-release-state/production-checkout-review.json')
    path.write_text(json.dumps(record, indent=2) + '\n')
    path.chmod(0o600)
    PY

This is a trusted operator attestation, not an automatic semantic comparison.
Do not generate the record before performing the review.

## Migration backup and genuine isolated restore validation

Only DATABASE_MIGRATION requires these files. During the approval wait, use the
exact commit shown in the workflow summary and a clean checkout of that release.

Create a fresh custom-format backup on EC2, preserving older backups:

    set -euo pipefail
    umask 077
    install -m 700 -d /opt/edurite-backups
    backup="/opt/edurite-backups/production-$(date -u +%Y%m%dT%H%M%SZ).dump"
    docker exec edurite-postgres sh -c 'TZ=UTC pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$backup.partial"
    mv "$backup.partial" "$backup"

Transfer the backup securely to a separate isolated validation machine. It needs
PostgreSQL tools matching production, Java 21+, Maven and the committed release
checkout. Do NOT restore onto production or run the isolated cluster on EC2.

    python3 scripts/validate-restore.py \
      --backup /private/production.dump \
      --commit "$RELEASE_SHA" \
      --pg-bin /usr/lib/postgresql/16/bin \
      --output /private/restore-evidence.json

The existing validator initializes a new private loopback cluster, restores with
--exit-on-error, reads the actual source migration version, migrates to the
release's actual latest version, validates checksums and repeats migrate as a
no-op. It compares pre-existing rows and writes evidence only after success.
It no longer assumes every release is V74 to V79. Pre-V74 backups still fail
closed pending a separate compatibility review. The V75/V76 known row changes
are excluded only when those migrations are actually being crossed; later
migrations must preserve existing rows or receive a reviewed validator update.

The dump archive creation time AND validation must be less than 24 hours old.
For a non-UTC archive supply its actual --archive-utc-offset-minutes. Evidence
contains the exact backup SHA-256, commit, full migration inventory, actual source
and target versions and successful data-preservation result. A copied file's
mtime is not backup evidence.

Securely upload the newly generated JSON to EC2. It is not a CI artifact and the
workflow deliberately never restores private production data on the EC2 host.
Generating JSON on a validation machine does not install it on EC2. Do not reuse
evidence for another commit, another dump, or an earlier migration target.

After upload, use the tested release's staging command on EC2 (it does not connect
to PostgreSQL). Keep the timestamped source dump and JSON as originals:

```bash
python3 "$STAGED_RELEASE/scripts/stage-restore-evidence.py" \
  --release-dir "$STAGED_RELEASE" --commit "$RELEASE_SHA" \
  --backup /private/path/to/the-exact-validated.dump \
  --evidence /private/path/to/the-newly-uploaded-evidence.json
```

The command verifies freshness, commit, backup bytes, successful restore/migrate/
preservation flags and the complete tested migration inventory. It checks the
copied bytes again before replacing the workflow files, with mode 0600, while
holding the same host lock as deployment. Invalid evidence leaves the existing
pair untouched. A crash between replacements produces a hash mismatch and blocks
deployment. Restrict manual writes to these paths to the same trusted operator.

The exact workflow locations remain:

    /opt/edurite-backups/release.dump
    /opt/edurite-backups/restore-evidence.json

Verify the installed pair before approval:

```bash
python3 "$STAGED_RELEASE/scripts/stage-restore-evidence.py" --check-only \
  --release-dir "$STAGED_RELEASE" --commit "$RELEASE_SHA" \
  --backup /opt/edurite-backups/release.dump \
  --evidence /opt/edurite-backups/restore-evidence.json
```

The deploy job now performs this same preflight, then release.py repeats the gate
under its deployment lock and compares the recorded source version to live
Flyway history. Frontend-only and no-schema releases keep their existing gates.

Keep timestamped originals. Restrict evidence write access; it remains trusted
operator-owned evidence, not a cryptographic external attestation. Approve the
production-migrations job only after reviewing these results. release.py checks
freshness, hash, commit, inventory and live source version again. Neither a
classification override nor --migration-approved alone bypasses restore checks.

## Health and rollback

FRONTEND_ONLY pulls only the frontend digest and starts only frontend with:

    docker compose ... up -d --no-build --no-deps --pull never --wait ... frontend

It checks container health, local frontend HTTP, public https://edurite.co.za/,
unchanged backend/PostgreSQL/Redis container IDs and unchanged .env bytes. It
does not execute SQL, Redis commands or backend restart/health commands.
On failure it attempts rollback to the previous retained immutable frontend
image ID, repeats checks and leaves the workflow failed with a recorded outcome.

Backend releases retain production configuration/SES/persistent-upload checks,
exact image revision checks, backend readiness, frontend HTTP, proxy-to-backend
readiness, PostgreSQL SELECT 1 and Redis PONG. No-schema releases require an exact
Flyway-history match before replacement. Migration releases permit only reviewed
pending migrations, and require matching restore evidence.

State records remain in /opt/edurite-release-state/SHA-TIMESTAMP.json. For a
recorded application rollback, use the failed release's staged directory:

    python3 "$STAGED_RELEASE/scripts/release.py" rollback \
      --release-dir "$STAGED_RELEASE" --project-dir /opt/edurite-v2 \
      --commit "$RELEASE_SHA" --state "$DEPLOYMENT_STATE_FILE"

For DATABASE_MIGRATION add --schema-compatibility-reviewed only after reviewing
the actual migration outcome and old-image compatibility. Frontend and
BACKEND_NO_SCHEMA_CHANGE rollbacks do not need that schema attestation. They
still verify recorded state, unchanged environment/Compose configuration and
retained immutable image IDs. Frontend rollback refuses intervening backend/data
container changes. Rollback never restores a database over live writes or changes
a schema. Failed migration releases need operator review/forward fixes.

## Local verification

    python -m unittest discover -s scripts/tests -v
    python scripts/check-test-reports.py
    cd frontend && npm run verify
    cd backend && mvn verify

Required backend database tests need explicit disposable EDURITE_TEST_DB_*,
EDURITE_TRIAL_TEST_DB_* and EDURITE_ACADEMIC_TEST_DB_* settings, as supplied by CI.
Do not point these variables at production. Restore validation also requires a
clean committed checkout and an actual backup; unit tests never substitute
fabricated production restore evidence.

Workflow syntax is checked with checksum-verified actionlint 1.7.12.
Deployment command execution is covered by controlled command doubles locally;
GitHub environment approvals, registry publication and real EC2 execution require
a later authorized rollout. No production readiness claim follows from unit
tests alone.

References: [GitHub environments](https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments),
[workflow events](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows),
[Compose up](https://docs.docker.com/reference/cli/docker/compose/up/).

## Recorded local results — 15 October 2026

- Backend: mvn -o verify passed; 408 reported tests, 406 passed, zero failures/errors, two optional live-provider tests skipped. Required database/security suites ran against a disposable PostgreSQL 16 database. The report gate passed.
- Frontend: npm run verify passed; 139 tests across 17 files, encoding/typecheck and production build passed. Existing bundle/Browserslist warnings remain.
- Release tooling: 34 tests passed, including classification, migration bypass rejection, selective frontend deployment, rollback, drift rejection and reusable SES review.
- Workflow YAML/expressions: checksum-verified actionlint 1.7.12 passed for both existing workflows.
- Flyway compatibility: the new inventory matched actual versions/scripts/checksums through V79 in the disposable migrated database.
- Standalone restore validator: compiled successfully; a real synthetic dump was restored into a separate disposable database, V79 validation/repeat-migrate passed and 116 tables preserved existing rows. Actual source=79 and target=79 were reported. This no-op fixture verifies later-baseline support; it is not a production restore or new-migration rehearsal.
- No production data/access, registry publication, GitHub configuration, commit, push or deployment occurred. Command-level deployment tests use doubles; remote execution and environment approval still need a separately authorized rollout.
