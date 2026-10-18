# Deployment #66 classification investigation

## Failure and changed paths

[Deployment #66](https://github.com/arnold1990-cell/edurite-v2/actions/runs/39117771284)
targeted `ca429bcf83650df48c87405fbcf0c7e8958cd0c7`. CI/artifact provenance
verification and deployed-image inspection succeeded. Classification failed before
the deploy job, which was skipped.

The exact unclassified paths were:

```text
.cicd-backup/ci.yml
.cicd-backup/deploy.yml
```

Both were added in `10daf1a`. Review found archived YAML workflows, no references
from executable release tooling, and no inclusion in the backend or frontend
Docker build contexts. GitHub Actions runs the active workflows under
`.github/workflows/`, not these archived files.

The previous policy allowed neither exact path nor their directory. Its generic
error omitted the offending filenames, obscuring the reason that CI's latest-push
classification succeeded while deployment's cumulative classification failed.

## Baseline and release replay

The most recent successful deployment was
[#62](https://github.com/arnold1990-cell/edurite-v2/actions/runs/39069029109),
targeting `d9a08946d26c97c989d8e576cb7c7e099733dad2`. Its downloaded prepared
plan records FRONTEND_ONLY, with both pre-deployment revisions at
`b318f241f34250904ad182db743f0693288e4445`.
Applying that successful frontend-only rollout reconstructs these baselines:

- Backend: `b318f241f34250904ad182db743f0693288e4445`.
- Frontend: `d9a08946d26c97c989d8e576cb7c7e099733dad2`.

The subsequent commits through the failed release were `1e24440`, `10daf1a`,
`7e1ffc8` and `ca429bc`. The replay uses separate backend/frontend baselines;
it does not assume the backend advanced with the latest frontend deployment.
This is a local historical replay, not a fresh assertion about live host state.

Before the exact-path allowance, the replay rejects both archived paths. After
the allowance, the same release is DATABASE_MIGRATION, with these additions:

```text
backend/src/main/resources/db/migration/V81__email_password_recovery.sql
backend/src/main/resources/db/migration/V82__password_recovery_token_revocation.sql
```

The plan's full migration inventory and protected-source hashes match the
downloaded release manifest from successful
[CI run 39117506338](https://github.com/arnold1990-cell/edurite-v2/actions/runs/39117506338).
Manifest validation also confirmed the commit, CI run ID and immutable image
digest formats.

## Policy correction

Only the two reviewed archive paths are added to the exact-file allowance.
Archive-only changes, or archives mixed with frontend changes, conservatively
use BACKEND_NO_SCHEMA_CHANGE. CI/tooling and backend Java test changes retain
that policy; frontend-only tests retain FRONTEND_ONLY. Pending migrations and
schema-sensitive changes always take DATABASE_MIGRATION. Unknown backup siblings
and unknown roots still fail closed, now with offending paths in the error.

The active workflows, release executor, historical migrations and application
code were not changed. Successful-current-main CI verification, artifact checks,
backup/restore evidence, protected migration approval, inventory checks and
rollback safeguards remain in force. This correction does not authorize the
V81/V82 production rollout; its migration approval and backup evidence are still
required.

## Local validation

- `python -m unittest discover -s scripts/tests -p test_classified_release.py -v`:
  31 tests passed, including new exact-path, CI/test-only, unknown-path and real
  temporary Git history V80-to-V82 regression coverage.
- `python -m unittest discover -s scripts/tests -v`: all 45 deployment verification
  tests passed. Deployment execution is mocked; no production commands run.
- Actionlint 1.7.12: both active workflows passed.
- Historical release replay: DATABASE_MIGRATION; V81 and V82 detected.
- CI release manifest comparison and `git diff --check`: passed.

No manual deployment, production database access, production data changes,
workflow dispatch, commit or push was performed for this investigation.
