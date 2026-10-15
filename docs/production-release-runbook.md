# Controlled production release

Use this after the changes have been reviewed, committed and pushed, and the **CI**
workflow for that exact main commit has succeeded. These instructions have not been
executed on production. Never use the inspection baseline SHA as the new release.

## Prerequisites

- Configure GitHub environment `production`, required reviewers where supported,
  and deployment branch restriction to main. A YAML environment reference alone
  does not configure reviewers in GitHub settings.
- Configure existing EC2 SSH secrets plus `EC2_KNOWN_HOSTS` from a trusted host-key
  exchange. Do not generate trust with an unchecked `ssh-keyscan` during deployment.
- Permit the CI token to publish GHCR packages. Provision EC2 read access to the
  resulting packages separately, or make the application images public if appropriate.
  Never print a registry credential in deployment logs.
- EC2 needs Python 3, Docker with Compose supporting `--wait`, and its existing
  production checkout/configuration/data services. The deployer needs write access
  to `/opt/edurite-releases` and `/opt/edurite-release-state`.
- Keep the existing `.env`, PostgreSQL volume, Redis container and production
  checkout. The release uses a separate staged directory and the existing Compose
  project name discovered from `edurite-postgres`.
- Confirm the IAM role permits SES sending in `af-south-1`, the sender identity is
  verified and SES recipient restrictions allow the required delivery. Verify IMDS
  access from the backend container. Use the SDK default credentials provider.
- Confirm verification links and frontend routing at `https://edurite.co.za`, and
  confirm production CORS/public URL settings include that domain. Reconcile existing
  `.org` defaults with actual environment values; do not blindly replace payment URLs.

## Review the production SES modifications

Keep production `backend/pom.xml`, `EmailVerificationService.java` and their backups.
Compare them privately against the selected release files and reconcile any required
functionality in the repository before publishing the final release. Do not copy the
whole production files over the repository or print private diffs into Actions logs.

After that review, record the hashes of the two unchanged production files for the
selected release. This is an operator attestation, not an automated semantic review:

```bash
export RELEASE_SHA='FULL_40_CHARACTER_TESTED_MAIN_SHA'
install -m 700 -d /opt/edurite-release-state
python3 - <<'PY'
import hashlib, json, os
from pathlib import Path
root = Path('/opt/edurite-v2')
names = ['backend/pom.xml', 'backend/src/main/java/com/edurite/auth/service/EmailVerificationService.java']
Path('/opt/edurite-release-state/production-checkout-review.json').write_text(json.dumps({
    'commit': os.environ['RELEASE_SHA'],
    'files': {name: hashlib.sha256((root/name).read_bytes()).hexdigest() for name in names}
}))
PY
```

## Persist existing documents before replacing an application container

The original Compose file did not mount document storage. The production override
requires a pre-existing writable bind directory, default `/opt/edurite-uploads` at
`/app/data/uploads`. Custom storage paths require matching absolute mount targets.
`DOCUMENT_UPLOADS_HOST_DIRECTORY` selects an existing host directory.

Inspect the existing backend's configured storage path and mounts privately. If it
contains unmounted uploads, the release deliberately stops. Migrate those documents
under a maintenance window: quiesce writes, copy the existing directory to persistent
storage, compare file names and content hashes, preserve ownership, and establish the
same persistent mount on the current application image before this release. Keep the
original container/export until the copy and access checks pass. Do not mount an empty
directory over existing documents or delete the old copy. This migration needs the
actual host paths and current container details; it has not been performed here.

## Create a backup and validate restoration separately

On EC2, create a custom-format dump with UTC archive timestamps. Keep timestamped
backups; never overwrite the only known good backup. Commands use the PostgreSQL
container's existing environment and do not display its password:

```bash
set -euo pipefail
umask 077
install -m 700 -d /opt/edurite-backups
backup="/opt/edurite-backups/production-$(date -u +%Y%m%dT%H%M%SZ).dump"
docker exec edurite-postgres sh -c 'TZ=UTC pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$backup.partial"
mv "$backup.partial" "$backup"
```

Securely transfer that dump to an isolated validation machine with PostgreSQL tools
matching production's major version, Java 21+, Maven and a clean checkout of the
selected release. Do not run restoration on the small production EC2 machine.

```bash
python3 scripts/validate-restore.py \
  --backup /private/production.dump \
  --commit "$RELEASE_SHA" \
  --pg-bin /usr/lib/postgresql/16/bin \
  --output /private/restore-evidence.json
```

The validator creates its own loopback cluster and database. It accepts no application
database URL, expects V74, restores with `--exit-on-error`, migrates through V79,
validates checksums, reruns migration as a no-op and compares pre-existing rows across
public tables. Only the specifically authorized Basic pricing fields and eligible
V76 recipient's legacy plan cache/timestamp are excluded from row equality. New
columns/rows are allowed. It never runs Flyway clean or repair.

The dump archive's creation time and validation must both be less than 24 hours old.
For an existing non-UTC dump, supply its actual `--archive-utc-offset-minutes`.
Copy the validated exact dump to `/opt/edurite-backups/release.dump` and its evidence
to `/opt/edurite-backups/restore-evidence.json` with private permissions. The deployer
checks the dump SHA-256, freshness and selected commit. Keep original timestamped
backups. Restrict write access to evidence; it is trusted operator-owned evidence,
not a cryptographic attestation from an external service.

## Deploy the tested images

From an authenticated GitHub CLI workstation, choose the successful **CI** run that
published both images and artifact `release-$RELEASE_SHA`:

```bash
export CI_RUN_ID='SUCCESSFUL_CI_RUN_ID'
gh workflow run deploy.yml --ref main \
  -f release_sha="$RELEASE_SHA" -f ci_run_id="$CI_RUN_ID"
gh run list --workflow deploy.yml --limit 5
```

Approve through the configured GitHub production environment when required. No
`workflow_run` trigger deploys automatically. The workflow verifies SHA/main ancestry,
CI path/event/result and matching artifact provenance. It stages release files outside
the checkout, then runs the equivalent host command:

```bash
python3 "$STAGED_RELEASE/scripts/release.py" deploy \
  --release-dir "$STAGED_RELEASE" --project-dir /opt/edurite-v2 \
  --commit "$RELEASE_SHA" \
  --backup /opt/edurite-backups/release.dump \
  --evidence /opt/edurite-backups/restore-evidence.json
```

`STAGED_RELEASE` is the workflow's `/opt/edurite-releases/SHA-DEPLOY_RUN_ID` directory.
The release validates configuration, image revision labels, persistent uploads and
data-service connectivity, then runs backend and frontend sequentially with
`--no-build --no-deps --pull never --wait`. It checks backend readiness, frontend
HTML, frontend-to-backend readiness, PostgreSQL SQL connectivity and Redis PONG.
It also verifies that data-service container IDs and protected file hashes stayed
unchanged. No environment expansion is printed. Host results are recorded in
`/opt/edurite-release-state/SHA-TIMESTAMP.json`.

After the host reports healthy, check the public route through the real TLS proxy:

```bash
curl --fail --silent --show-error https://edurite.co.za/ >/dev/null
curl --fail --silent --show-error https://edurite.co.za/actuator/health/readiness >/dev/null
```

Use isolated test accounts to verify actual SES delivery, single-use links, login,
profile reload, Career Match/Explorer, saved roadmap/study-plan progress and tutor
responses. Container readiness alone does not establish deliverability or AI quality.

## Application rollback

On a failed release, inspect the state record and migration outcome privately first.
If the old application is compatible with the current schema, use the failed
release's staged files and state record:

```bash
python3 "$STAGED_RELEASE/scripts/release.py" rollback \
  --release-dir "$STAGED_RELEASE" --project-dir /opt/edurite-v2 \
  --commit "$RELEASE_SHA" --state "$DEPLOYMENT_STATE_FILE" \
  --schema-compatibility-reviewed
```

Rollback restores the recorded immutable local backend/frontend image IDs, checks
environment/Compose hashes and repeats health checks. Images are retained; no image
pruning occurs. This restores application images using the staged configuration;
it does not reconstruct arbitrary earlier configuration. Reconcile any unrelated
configuration drift before using it. It never rolls back PostgreSQL schema/data.
If a migration is incompatible, stop and prepare a forward fix. Do not attempt a
destructive schema rollback or restore a database over live learner writes.

Compose option behavior was checked against [Docker's official reference](https://docs.docker.com/reference/cli/docker/compose/up/).
Flyway migration behavior was checked against [Redgate's documentation](https://documentation.red-gate.com/flyway/reference/usage/command-line).
