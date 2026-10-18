# V80 deployment investigation — 16 October 2026

Target: `7f8458827dc21afb8359f552f335558bed48a9f4`.
Production database and application containers were not replaced or migrated.

## Root cause and evidence

The reported `Backup and separate restore evidence are required` exception is
raised only when `--backup` or `--evidence` is absent. It does not mean that Python
searched for `restore-evidence.json` and failed to find it. The CLI has no default
for either argument; `deploy.sh` forwards the arguments supplied by its caller.

The actual latest GitHub Actions failure, run `38858488083`, supplied both
arguments and failed with `Missing restore and migration evidence for selected
release`. SSH inspection confirmed the installed JSON belonged to commit
`8c7dca8f8c949f3153b836f3a98e36418814a17a`, source 74 / target 79, and backup
hash `bad575f0435b3c81b3de0a2b75845e29193d35e7c91efdf03c43567c61fca979`.
The installed dump instead had hash
`03a72b955c740697cf784010b3c712e879badf056e1bca96febdc7411d604b7c`.

A newer local JSON existed for commit `92dcf187605e2013032983106a99be3aabde66b9`
and that earlier dump. It was never installed at the workflow location and cannot
be relabeled as evidence for the selected commit or new dump. Git history shows
the validator already generated genuine evidence successfully. The broken link
was the manual transfer/install handoff, not V80 SQL or JSON generation.

The requested backup `/home/ubuntu/edurite-pre-v80-20261016-094851.dump`
has SHA-256 `f67e5aea3db905dd34cc2539c980b4cd335e35045dfc9c08921fb20e71c9fe53`.
Its archive timestamp is 16 October 2026 09:48:51 UTC. It was securely copied to
the separate local validation machine for actual restoration.

## Required evidence contract

`release.py` reads exactly the paths passed via `--backup` and `--evidence`.
The existing workflow supplies `/opt/edurite-backups/release.dump` and
`/opt/edurite-backups/restore-evidence.json`.

The JSON must contain:

- `commit`: the exact full release SHA.
- `backup_sha256`: SHA-256 of the exact dump file passed to the release command.
- `backup_created_at`, `validated_at`: finite Unix epoch timestamps, neither in
  the future nor more than 24 hours old. Validation must follow backup creation.
- `restored`, `migrated`, `data_preserved`: JSON boolean `true`, produced only
  after the real restore/migration/preservation checks finish.
- `source_version`, `target_version`: strings matching the live source and final
  tested migration version (this incident: `"79"` and `"80"`).
- `migrations`: the exact CI inventory, ordered numerically, each row containing
  `version`, `script`, signed Flyway CRC32 `checksum`, and file-byte `sha256`.

These fields describe the format; this document is not validation evidence.
The evidence remains a trusted operator artifact. Its backup hash is a binding
to bytes, not a signature proving who executed the validation.

## Corrections

- `scripts/stage-restore-evidence.py`: installs the real uploaded evidence and
  its matching dump after validating both against the CI manifest. Revalidates
  copied bytes before replacement, uses private files and the deployment lock,
  retains the input originals, and supports a preflight check without database
  access. Invalid input cannot replace an existing pair; an interrupted install
  fails closed on hash mismatch.
- `scripts/release.py`: actionable missing-file/argument diagnostics, shared
  full evidence validation, timestamp ordering, inventory validation before
  pulling images, and reuse of the same loaded evidence for the live-schema
  check. All existing backup, migration approval and rollback gates remain.
- `.github/workflows/deploy.yml`: includes the staging tool in the tested release
  bundle and verifies the installed pair before invoking deployment. CI provenance,
  immutable images, protected migration approval, live-history checks, sequential
  readiness checks and prohibition on automatic schema rollback remain intact.
- `scripts/tests/test_restore_staging.py`: real filesystem tests for verified
  installation, mismatch rejection, missing files, copy races and check-only mode.
- `scripts/tests/test_release.py`: fixture timestamps now represent backup
  creation before/equal to validation.
- `.gitignore`: excludes private deployment dumps/evidence from source control.
- `docs/production-release-runbook.md`: explicit secure upload/install/preflight
  handoff, exact workflow paths and safety behavior.

## V1–V80 and application compatibility

The migration directory contains 80 migrations in numeric V1–V80 order. The
classifier inventories every file and rejects duplicate, renamed, edited,
removed or out-of-order applied migrations. Read-only production inspection
matched all 79 applied versions, filenames and Flyway checksums to the target
commit and staged CI inventory. V80 is the only pending migration.

V80 depends on `users` and `subscriptions` from V1 and `subscriptions.provider`
from V16. The status constraints and trial/subscription extensions in intervening
migrations remain compatible. V80 creates a partial unique index for active
`ADMIN_OVERRIDE` subscriptions, an audit table with foreign keys and plan/reason
constraints, and a user/time index. It has no DROP, TRUNCATE, DELETE or UPDATE.
No migration SQL was changed, preserving all existing checksums. V80 checksum:
`591329627`; SHA-256:
`724283a891385a19f62467576e6b8f4f8685585b70209b55131e817a556c9ba8`.

There were zero duplicate active override groups in the live preflight. The
normal index build can briefly block subscription writes; concurrent writes
between preflight and deployment could still introduce a conflict. Failure must
stop the migration; do not delete rows to force it through.

The Java validator restores with `pg_restore --exit-on-error` into a newly
initialized private loopback cluster, executes Flyway migrate and validate,
requires a second migrate to execute zero migrations, and compares all original
table rows using `EXCEPT ALL`. Source 79 crosses none of the explicitly reviewed
V75/V76 row-change exclusions. Backend database tests cover Hibernate startup,
administrator authorization, audit persistence, repeated override replacement,
transaction rollback and preservation of paid billing agreements.

## Deployment limits

Backups/evidence expire after 24 hours; a new commit requires a new isolated
rehearsal. Backup validation cannot prevent changes made by trusted operators
after verification, so restrict writes and retain originals. Ordinary production
writes continue after the dump; the rehearsal proves compatibility with that
snapshot, not every later write. Production rollout still requires protected
review and readiness checks. No database rollback is automatic.

The local source corrections are not yet committed or pushed. Retrying the
existing target uses its original tested code and the newly installed evidence;
publishing the local corrections creates a new release SHA and requires evidence
for that SHA. Do not rewrite the target's evidence to match a new commit.

## Recorded verification and recovery commands

The actual supplied backup passed isolated restore, V79→V80 migration, Flyway
validation, repeat migration with zero work, and existing-row preservation on
PostgreSQL 17 and again on PostgreSQL **16.15**, matching production. The second
run used a clean detached checkout of the exact target and the unchanged
committed validator. No evidence was synthesized, relabeled or edited.

The exact successful matching-version command on the isolated Windows machine:

```powershell
python C:/Users/user/edurite-validation/target-7f84588/scripts/validate-restore.py --backup C:/Users/user/edurite-validation/production-v80-20261016-094851.dump --commit 7f8458827dc21afb8359f552f335558bed48a9f4 --pg-bin C:/Users/user/edurite-validation/pg16-matching/pgsql/bin --output C:/Users/user/edurite-validation/restore-v80-7f84588-pg16.json
```

The genuine result was securely uploaded to the private host directory
`/home/ubuntu/edurite-v80-validation-7f84588-20261016`. The new staging helper
and its Python dependencies were uploaded there without modifying the original
target's production checkout or staged release code. Prior canonical backup and
evidence were preserved there as `previous-release.dump` and
`previous-restore-evidence.json`.

The installation command on EC2 is:

```bash
python3 /home/ubuntu/edurite-v80-validation-7f84588-20261016/stage-restore-evidence.py \
  --release-dir /opt/edurite-releases/7f8458827dc21afb8359f552f335558bed48a9f4-38858488083 \
  --commit 7f8458827dc21afb8359f552f335558bed48a9f4 \
  --backup /home/ubuntu/edurite-pre-v80-20261016-094851.dump \
  --evidence /home/ubuntu/edurite-v80-validation-7f84588-20261016/restore-v80-7f84588-pg16.json
```

For a safe recheck without starting deployment, run on EC2:

```bash
python3 /home/ubuntu/edurite-v80-validation-7f84588-20261016/stage-restore-evidence.py --check-only \
  --release-dir /opt/edurite-releases/7f8458827dc21afb8359f552f335558bed48a9f4-38858488083 \
  --commit 7f8458827dc21afb8359f552f335558bed48a9f4 \
  --backup /opt/edurite-backups/release.dump \
  --evidence /opt/edurite-backups/restore-evidence.json
```

After review, if current main is still the selected target and the backup has
not expired, the existing workflow can be retried from this workstation:

```powershell
gh workflow run deploy.yml --ref main
```

This command was not executed. It selects verified CI artifacts and recomputes
the deployed baseline; it does not remove the protected migration review.
Do not invoke a direct manual deployment omitting `--backup` and `--evidence`.

Other validation: 39 Python release/staging tests passed; frontend encoding,
typecheck, all 150 tests across 20 files, and production build passed; Compose
configuration parsed with synthetic credentials; actionlint 1.7.12 passed both
workflows; Python compilation and `git diff --check` passed. Backend Maven
verification on disposable PostgreSQL 17 passed 424 tests, with zero failures/
errors and two explicitly optional live-provider skips; the full verification
was repeated successfully on a separate disposable PostgreSQL 16.15 cluster with
the same results and report gate passed after completion.
Docker image builds were not rerun locally because the Docker daemon was
unavailable. Existing CI performs backend/frontend image builds and HTTP checks.
The production homepage returned HTTP 200 during read-only verification.

The verified pair was installed successfully and the host check-only command
passed. The original dump remained intact and the previous canonical files were
preserved. A final read-only check confirmed production is still V79, all 79
checksums still match, and every running container/image remains equal to the
original classified plan. Current main was rechecked and still equals the target.
The backup/evidence freshness window ends **17 October 2026 09:48:51 UTC**.
