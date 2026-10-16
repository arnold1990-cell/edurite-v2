import importlib.util
import json
import os
from pathlib import Path
import sys
import tempfile
import time
from types import SimpleNamespace
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import release

SHA = 'a' * 40
MIGRATIONS = [dict(version='79', script='V79__fixture.sql', checksum=123, sha256='e' * 64)]
SNAPSHOT = dict(project='existing-project',
                containers={s: 'container-' + s for s in ('backend', 'frontend', 'postgres', 'redis')},
                images={s: 'sha256:' + 'd' * 64 for s in ('backend', 'frontend')},
                revisions={s: 'f' * 40 for s in ('backend', 'frontend')})
MANIFEST = dict(commit=SHA, migrations=MIGRATIONS, protected_files={p: 'f' * 64 for p in release.PROTECTED},
 backend='ghcr.io/example/backend@sha256:' + 'b' * 64,
                frontend='ghcr.io/example/frontend@sha256:' + 'c' * 64)


class ReleaseSafetyTests(unittest.TestCase):
    def test_rejects_floating_tags_and_wrong_commit(self):
        release.validate_manifest(MANIFEST, SHA)
        for manifest in (dict(MANIFEST, backend='ghcr.io/example/backend:latest'), dict(MANIFEST, commit='b' * 40)):
            with self.assertRaises(ValueError):
                release.validate_manifest(manifest, SHA)

    def test_backup_requires_matching_fresh_restore_and_data_evidence(self):
        with tempfile.TemporaryDirectory() as directory:
            backup = Path(directory) / 'backup'
            backup.write_bytes(b'synthetic fixture')
            evidence = dict(commit=SHA, restored=True, migrated=True, data_preserved=True,
                            backup_sha256=release.digest(backup), validated_at=100000, backup_created_at=100000)
            release.validate_backup(evidence, backup, SHA, now=100001)
            for change in (dict(validated_at=0), dict(backup_created_at=0), dict(restored=False),
                           dict(migrated=False), dict(data_preserved=False), dict(commit='wrong'),
                           dict(backup_sha256='wrong'), dict(validated_at=float('nan')), dict(backup_created_at=float('inf')), dict(validated_at=100002), dict(validated_at=99999)):
                with self.subTest(change=change), self.assertRaises(ValueError):
                    release.validate_backup(dict(evidence, **change), backup, SHA, now=100001)

    def setup_release(self, directory):
        root = Path(directory)
        project, staged, state = root / 'project', root / 'release', root / 'state'
        for path in (project, staged, state):
            path.mkdir()
        for name in ('.env', 'backend/pom.xml', 'backend/src/main/java/com/edurite/auth/service/EmailVerificationService.java'):
            path = project / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text('preserve me')
        release.write_json(staged / 'release.json', MANIFEST)
        release.write_json(staged / 'deployment-plan.json', dict(commit=SHA, classification='DATABASE_MIGRATION',
            backend_base='f' * 40, frontend_base='f' * 40, changes=['backend/src/main/resources/application.yml'],
            baseline_migrations=MIGRATIONS, migrations=MIGRATIONS, protected_files=MANIFEST['protected_files'], expected_inventory=SNAPSHOT))
        for name in ('docker-compose.yml', 'docker-compose.production.yml'):
            (staged / name).write_text('services: {}')
        files = {p: release.digest(project / p) for p in ('backend/pom.xml', 'backend/src/main/java/com/edurite/auth/service/EmailVerificationService.java')}
        release.write_json(state / 'production-checkout-review.json', dict(commit=SHA, files=files))
        backup = root / 'backup'
        backup.write_bytes(b'synthetic')
        evidence = root / 'evidence.json'
        now = time.time()
        release.write_json(evidence, dict(commit=SHA, restored=True, migrated=True, data_preserved=True,
                                         backup_sha256=release.digest(backup), validated_at=now, backup_created_at=now, source_version='79', target_version='79', migrations=MIGRATIONS))
        args = SimpleNamespace(action='deploy', commit=SHA, backup=backup, evidence=evidence, state=None, schema_compatibility_reviewed=False, migration_approved=True, public_url='https://edurite.co.za')
        return args, project, staged, state

    def fake_run(self, calls, failure=False):
        def run(*args, **kwargs):
            calls.append(args)
            if 'config' in args:
                return json.dumps({'services': {'backend': {'volumes': [{'type': 'bind', 'target': '/app/data/uploads', 'source': '/opt/edurite-uploads'}], 'environment': {
                    'SPRING_DATASOURCE_PASSWORD': 'synthetic', 'SPRING_JPA_HIBERNATE_DDL_AUTO': 'validate',
                    'SPRING_PROFILES_ACTIVE': 'prod', 'JWT_SECRET': 'synthetic-test-only-secret-at-least-32-characters',
                    'GEMINI_API_KEY': 'synthetic', 'APP_CORS_ALLOWED_ORIGINS': 'https://edurite.co.za',
                    'SPRING_FLYWAY_ENABLED': 'true', 'APP_EMAIL_VERIFICATION_BASE_URL': 'https://edurite.co.za',
                    'APP_EMAIL_VERIFICATION_AWS_REGION': 'af-south-1', 'APP_EMAIL_VERIFICATION_FROM': 'info@edurite.co.za'}}}})
            if 'SELECT coalesce(json_agg' in ' '.join(args):
                return json.dumps([dict(MIGRATIONS[0], success=True, type='SQL')])
            if args[:2] == ('docker', 'ps'):
                return 'container-' + args[-1].split('=')[-1]
            if 'ps' in args:
                return 'container-' + args[-1]
            if 'inspect' in args:
                if '{{json .Mounts}}' in args: return '[]'
                if 'com.docker.compose.project' in ' '.join(args): return 'existing-project'
                if 'image.revision' in ' '.join(args): return 'f' * 40 if args[-1].startswith('sha256:') else SHA
                return 'sha256:' + 'd' * 64
            if 'ping' in args: return 'PONG'
            if 'up' in args and args[-1] == 'backend' and failure:
                raise RuntimeError('backend readiness failed')
            return ''
        return run

    def test_backend_failure_does_not_start_frontend_or_rollback_database(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(os.environ):
            args, project, staged, state = self.setup_release(directory)
            calls = []
            with patch.object(release, 'run', side_effect=self.fake_run(calls, failure=True)), self.assertRaises(RuntimeError):
                release.execute(args, project, staged, state)
            up = [call for call in calls if 'up' in call]
            self.assertEqual([call[-1] for call in up], ['backend'])
            self.assertIn('--no-deps', up[0])
            self.assertIn('--no-build', up[0])
            self.assertFalse(any('down' in call or 'build' in call or 'reset' in call for call in calls))
            result = json.loads(next(state.glob(SHA + '-*.json')).read_text())
            self.assertEqual(result['status'], 'failed-review-schema-before-rollback')
            self.assertIn('previous', result)

    def test_success_preserves_checkout_and_orders_application_services(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(os.environ):
            args, project, staged, state = self.setup_release(directory)
            calls = []
            with patch.object(release, 'run', side_effect=self.fake_run(calls)):
                release.execute(args, project, staged, state)
            self.assertEqual([c[-1] for c in calls if 'up' in c], ['backend', 'frontend'])
            self.assertEqual((project / '.env').read_text(), 'preserve me')
            result = json.loads(next(state.glob(SHA + '-*.json')).read_text())
            self.assertEqual(result['status'], 'healthy')

    def test_rollback_requires_explicit_schema_review(self):
        with tempfile.TemporaryDirectory() as directory:
            args, project, staged, state = self.setup_release(directory)
            args.action = 'rollback'
            with patch.object(release, 'run') as runner, self.assertRaises(ValueError):
                release.execute(args, project, staged, state)
            runner.assert_not_called()

    def test_reviewed_rollback_restores_recorded_image_ids_without_pull_or_db_restart(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(os.environ):
            args, project, staged, state = self.setup_release(directory)
            with patch.object(release, 'run', side_effect=self.fake_run([])):
                release.execute(args, project, staged, state)
            args.action = 'rollback'
            args.state = next(state.glob(SHA + '-*.json'))
            args.schema_compatibility_reviewed = True
            calls = []
            with patch.object(release, 'run', side_effect=self.fake_run(calls)):
                release.execute(args, project, staged, state)
            self.assertEqual(os.environ['BACKEND_IMAGE'], 'sha256:' + 'd' * 64)
            self.assertEqual(os.environ['FRONTEND_IMAGE'], 'sha256:' + 'd' * 64)
            self.assertFalse(any('pull' in c or 'down' in c for c in calls))
            self.assertEqual([c[-1] for c in calls if 'up' in c], ['backend', 'frontend'])

    def test_unmounted_documents_block_container_replacement(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(os.environ):
            args, project, staged, state = self.setup_release(directory)
            calls = []
            delegate = self.fake_run(calls)
            def run(*command, **kwargs):
                if command[:2] == ('docker', 'exec') and 'storage-check' in command:
                    raise RuntimeError('unmounted uploads found')
                return delegate(*command, **kwargs)
            with patch.object(release, 'run', side_effect=run), self.assertRaises(RuntimeError):
                release.execute(args, project, staged, state)
            self.assertFalse(any('up' in c for c in calls))


class ReportGateTests(unittest.TestCase):
    def test_missing_reports_fail(self):
        path = Path(__file__).resolve().parents[1] / 'check-test-reports.py'
        spec = importlib.util.spec_from_file_location('gate', path)
        gate = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(gate)
        with tempfile.TemporaryDirectory() as directory, self.assertRaises(SystemExit):
            gate.check(directory)

    def test_unexpected_database_skip_fails(self):
        path = Path(__file__).resolve().parents[1] / 'check-test-reports.py'
        spec = importlib.util.spec_from_file_location('gate', path)
        gate = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(gate)
        with tempfile.TemporaryDirectory() as directory:
            for name in gate.REQUIRED:
                (Path(directory) / f'TEST-{name}.xml').write_text(f'<testsuite name="{name}" tests="1" skipped="0"/>')
            gate.check(directory)
            (Path(directory) / 'TEST-TrialMigrationTest.xml').write_text('<testsuite name="TrialMigrationTest" tests="1" skipped="1"/>')
            with self.assertRaises(SystemExit): gate.check(directory)


if __name__ == '__main__':
    unittest.main()
