# V77 local startup reconciliation

## Root cause

The native PostgreSQL 17 database at localhost:5432/edurite has Flyway history
through V76, but already contains the nullable career_program_requirements.
qualification_type VARCHAR(120) column with no default. V77 adds that column
unconditionally and fails with SQLSTATE 42701. PostgreSQL rolls back the whole
migration, including its earlier aps_rule addition. JPA, JWT and Tomcat startup
errors are downstream effects. The unmanaged column's origin is unconfirmed.

V76 manages pending grants. V78 adds study plan history fields. V79 adds nullable
admission metadata. V63's similarly named column belongs to university_programmes,
a different table. A new V80 alone cannot resolve a failure occurring at V77.

V77 has succeeded in other recorded local runs (completion-runtime.log and
phase-database-final.log). Its SQL remains unchanged. Its Flyway checksum remains
2050080389. No repair, baseline, skipped migration or fabricated history is used.

## Additive local fix

LocalV77Reconciliation is a Flyway callback discovered by Spring Boot, enabled
only for dev when neither prod nor production is active. Reconciliation also
requires a loopback PostgreSQL JDBC URL.

For the exact V77 script, both callbacks run on Flyway's migration connection
inside its transaction, holding an exclusive lock on the affected table:

1. Before V77, reject reserved-name collisions and incompatible definitions. If
   the known compatible column exists and aps_rule does not, temporarily rename
   the original column to qualification_type_pre_v77.
2. Run the original V77 SQL, including both original ADD COLUMN statements.
3. After V77, rename the newly added empty column to qualification_type_v77_added,
   then restore the original column's name.

The original column retains its values, identity and dependent objects. The new
empty column is deliberately retained to comply with the prohibition on dropping
columns. No data copying or UPDATE/DELETE is needed. Fresh schemas take a no-op
path through the callbacks. Already-applied V77 is never rerun. Errors roll back
the renames and V77's DDL together; normal retry needs no history repair.

An incompatible type, length, nullability, default, generated/identity definition,
existing aps_rule, reserved-name collision or autocommit connection stops the
operation. Remote drift is refused. Production does not register this callback.
This is a local development correction, not a production recovery procedure.

## Verification

V77SchemaDriftIntegrationTest exercises PostgreSQL 17 in disposable private
schemas: original failure, fresh upgrades, preservation of nonempty synthetic
values and a dependent view, immutable checksum, repeated runs, incompatible
columns, reserved names, forced failure after V77, atomic rollback and retry.
LocalV77ReconciliationTest checks profile exclusions, loopback restrictions and
refusal to operate outside a transaction.

Use a dedicated disposable database, never the application database. Set
EDURITE_TEST_DB_URL/USERNAME/PASSWORD and EDURITE_TRIAL_TEST_DB_URL/USER/PASSWORD
for that database, then run from backend:

    mvn -o '-Dtest=V77SchemaDriftIntegrationTest,LocalV77ReconciliationTest,RoadmapMigrationIntegrationTest,FlywayRuntimeConfigTest,DevProfileDatasourceIntegrationTest' test

These tests are complemented by startup against an isolated copy of the affected
schema and Flyway history. No application records are required for that copy.
Logs are kept in ignored build/v77-reproduction and backend/target directories.

## Verified local result - 16 October 2026

- Focused Maven verification: exit 0; 21 tests, no failures, errors or skips.
- The first isolated startup attempt raced with recompilation and failed with a
  missing class. Startup was repeated after compilation. This was a verification
  process error, not a migration failure.
- Actual native PostgreSQL 17.7: V77, V78 and V79 applied successfully; all 79
  migrations validated, zero pending. V77 checksum remains 2050080389.
- The original V1-V76 history fingerprint remains
  3b741cf337454827d5e97a906e6f8369. The original qualification_type column still
  has attnum 29, type OID 1043 and typmod 124; programme record count remains zero.
- Actual backend: Started EduRiteApplication; startup verification reported ready.
  GET http://localhost:8080/actuator/health/readiness returned HTTP 200 with
  {"status":"UP"}.
- Seeders (including PERF_TEST_SEED_ENABLED), scheduled jobs and the AI provider
  startup probe were disabled through command arguments during native startup.
  No .env edits were made. The verification server was stopped afterward so
  IntelliJ can use port 8080.
- No production access, deployment, commit, history repair or historical SQL edit.
