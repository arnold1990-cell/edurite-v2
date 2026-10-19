import copy
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import classify_release as classifier
import release
import test_release as fixtures
from test_release import MANIFEST, MIGRATIONS, SHA, SNAPSHOT


class ClassificationTests(unittest.TestCase):
    def test_crawler_config_exception_is_revalidated_from_real_git_objects(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            classifier.git(root, 'init', '-q')
            classifier.git(root, 'config', 'user.name', 'Release validation fixture')
            classifier.git(root, 'config', 'user.email', 'fixture@example.invalid')
            for path in classifier.PROTECTED:
                target = root / path
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text('protected fixture')
            migration = root / classifier.MIGRATIONS / 'V79__fixture.sql'
            migration.parent.mkdir(parents=True)
            migration.write_text('SELECT 1;')
            config = root / 'backend/src/main/resources/application.yml'
            original = 'spring:\n  flyway:\n    enabled: true\nedurite:\n  university:\n    crawl:\n      max-fetch-retries: 2\n'
            config.write_text(original)
            classifier.git(root, 'add', '.')
            classifier.git(root, 'commit', '-qm', 'Isolated baseline fixture')
            base = classifier.git(root, 'rev-parse', 'HEAD').decode().strip()
            config.write_text(original.replace('max-fetch-retries: 2', 'max-fetch-retries: 3'))
            classifier.git(root, 'add', '.')
            classifier.git(root, 'commit', '-qm', 'Isolated crawler fixture')
            commit = classifier.git(root, 'rev-parse', 'HEAD').decode().strip()
            plan = classifier.classify(root, commit, base, base)
            plan['expected_inventory'] = {'revisions': {'backend': base, 'frontend': base}}
            manifest = {'migrations': plan['migrations'], 'protected_files': plan['protected_files']}
            self.assertEqual(plan['classification'], 'BACKEND_NO_SCHEMA_CHANGE')
            release.validate_plan(plan, manifest, commit, source_repository=root)
            # A self-declared exception without the underlying Git evidence is insufficient.
            with self.assertRaises(ValueError):
                release.validate_plan(plan, manifest, commit)
            with tempfile.TemporaryDirectory() as missing, self.assertRaises(ValueError):
                release.validate_plan(plan, manifest, commit, source_repository=Path(missing))

            # Changing schema/runtime settings cannot use the crawler-only exception.
            config.write_text(original.replace('enabled: true', 'enabled: false'))
            classifier.git(root, 'add', '.')
            classifier.git(root, 'commit', '-qm', 'Isolated unsafe configuration fixture')
            unsafe = classifier.git(root, 'rev-parse', 'HEAD').decode().strip()
            unsafe_plan = classifier.classify(root, unsafe, base, base)
            self.assertEqual(unsafe_plan['classification'], 'DATABASE_MIGRATION')
            unsafe_plan['classification'] = 'BACKEND_NO_SCHEMA_CHANGE'
            unsafe_plan['expected_inventory'] = plan['expected_inventory']
            with self.assertRaises(ValueError):
                release.validate_plan(unsafe_plan, manifest, unsafe, source_repository=root)

    def classify(self, changes, added=False, frontend=None, diff_text=''):
        old = MIGRATIONS
        new = old + [dict(version='80', script='V80__new.sql', checksum=456, sha256='a' * 64)] if added else old

        def git(root, *args):
            if args[0] == 'diff':
                if '-U0' in args:
                    return diff_text.encode()
                paths = changes if '--' not in args else (frontend if frontend is not None else [p for p in changes if p.startswith('frontend/')])
                return '\n'.join(paths).encode()
            if args[0] == 'show':
                return b'protected source'
            raise AssertionError(args)

        with patch.object(classifier, 'revision'), patch.object(classifier, 'inventory', side_effect=[old, new]), \
             patch.object(classifier, 'git', side_effect=git), patch.object(classifier.subprocess, 'run'):
            return classifier.classify(Path('.'), SHA, 'f' * 40, 'f' * 40)

    def test_frontend_only(self):
        self.assertEqual(self.classify(['frontend/src/app/App.tsx'])['classification'], 'FRONTEND_ONLY')

    def test_backend_without_schema(self):
        self.assertEqual(self.classify(['backend/src/main/java/com/edurite/tutor/service/TutorService.java'])['classification'], 'BACKEND_NO_SCHEMA_CHANGE')

    def test_mixed_release(self):
        self.assertEqual(self.classify(['frontend/package.json', 'backend/pom.xml'])['classification'], 'BACKEND_NO_SCHEMA_CHANGE')

    def test_pending_migration_cannot_hide_behind_latest_frontend_commit(self):
        self.assertEqual(self.classify(['frontend/src/app/App.tsx'], added=True)['classification'], 'DATABASE_MIGRATION')

    def test_schema_sensitive_changes_fail_conservatively(self):
        for path in ['backend/src/main/java/com/edurite/user/entity/User.java', 'backend/src/main/resources/application.yml', 'database/schema.sql']:
            with self.subTest(path=path):
                self.assertEqual(self.classify([path])['classification'], 'DATABASE_MIGRATION')

    def test_inline_ddl_and_schema_annotations_take_migration_gate(self):
        for text in ['+@Entity', '+ execute("ALTER TABLE learners ADD COLUMN value TEXT")']:
            self.assertEqual(self.classify(['backend/src/main/java/com/edurite/custom/Schema.java'], diff_text=text)['classification'], 'DATABASE_MIGRATION')

    def test_only_reviewed_registry_subtrees_are_schema_inert(self):
        original = 'spring:\n  flyway:\n    enabled: true\nedurite:\n  university:\n    registry:\n      - university-name: Old\n        base-domain: old.ac.za\n    crawl:\n      max-fetch-retries: 2\n'
        changed = original.replace('Old', 'New').replace('old.ac.za', 'new.ac.za')
        self.assertTrue(classifier.reviewed_registry_config_only(original, changed))
        self.assertTrue(classifier.reviewed_registry_config_only(original, original.replace('max-fetch-retries: 2', 'max-fetch-retries: 3')))
        for unsafe in [changed.replace('enabled: true', 'enabled: false'),
                       changed + 'spring.config.import: optional:file:external.yml\n',
                       changed.replace('base-domain: new.ac.za', 'base-domain: &alias new.ac.za'),
                       changed.replace('base-domain: new.ac.za', 'base-domain: !!str new.ac.za'),
                       changed.replace('max-fetch-retries: 2', 'ddl-auto: update'),
                       changed + '---\nspring:\n  flyway:\n    enabled: false\n']:
            with self.subTest(unsafe=unsafe):
                self.assertFalse(classifier.reviewed_registry_config_only(original, unsafe))

    def test_reviewed_config_exception_cannot_hide_migrations_or_schema_edits(self):
        with patch.object(classifier, 'reviewed_registry_config_only', return_value=True):
            config = ['backend/src/main/resources/application.yml']
            self.assertEqual(self.classify(config)['classification'], 'BACKEND_NO_SCHEMA_CHANGE')
            self.assertEqual(self.classify(config, added=True)['classification'], 'DATABASE_MIGRATION')
            self.assertEqual(self.classify(config, diff_text='+ spring.jpa.hibernate.ddl-auto: update')['classification'], 'DATABASE_MIGRATION')

    def test_unknown_path_fails_closed(self):
        with self.assertRaises(ValueError):
            self.classify(['unknown-root-script.sh'])

    def test_reviewed_workflow_backups_use_conservative_backend_policy(self):
        for path in ['.cicd-backup/ci.yml', '.cicd-backup/deploy.yml']:
            for changes in ([path], [path, 'frontend/src/app/App.tsx']):
                with self.subTest(changes=changes):
                    self.assertEqual(self.classify(changes)['classification'], 'BACKEND_NO_SCHEMA_CHANGE')

    def test_workflow_backups_cannot_hide_pending_migrations(self):
        changes = ['.cicd-backup/ci.yml', '.cicd-backup/deploy.yml', 'frontend/src/app/App.tsx']
        self.assertEqual(self.classify(changes, added=True)['classification'], 'DATABASE_MIGRATION')

    def test_ci_and_backend_test_only_changes_use_backend_policy(self):
        for path in ['.github/workflows/ci.yml', '.github/workflows/deploy.yml',
                     'scripts/check-test-reports.py', 'scripts/tests/test_classified_release.py',
                     'backend/src/test/java/com/edurite/auth/service/EmailRecoveryPostgresTest.java',
                     'backend/src/test/java/com/edurite/roadmap/V77SchemaDriftIntegrationTest.java']:
            with self.subTest(path=path):
                self.assertEqual(self.classify([path])['classification'], 'BACKEND_NO_SCHEMA_CHANGE')
                self.assertEqual(self.classify([path], added=True)['classification'], 'DATABASE_MIGRATION')

    def test_frontend_test_only_changes_follow_frontend_policy(self):
        changes = ['frontend/src/pages/public/PasswordRecovery.test.tsx',
                   'frontend/src/services/authService.recovery.test.ts']
        self.assertEqual(self.classify(changes)['classification'], 'FRONTEND_ONLY')
        self.assertEqual(self.classify(changes, added=True)['classification'], 'DATABASE_MIGRATION')

    def test_unreviewed_backup_paths_still_fail_closed_and_are_reported(self):
        unknown = ['.cicd-backup/new-workflow.yml', '.cicd-backup/ci.yml.sh',
                   '.cicd-backup/nested/ci.yml', '.other-backup/ci.yml', 'unknown-root-script.sh']
        with self.assertRaises(ValueError) as error:
            self.classify(['.cicd-backup/ci.yml', 'frontend/src/app/App.tsx'] + unknown, added=True)
        for path in unknown:
            self.assertIn(path, str(error.exception))

    def test_v81_v82_upgrade_with_workflow_backups_in_real_git_history(self):
        source = Path(__file__).resolve().parents[2]
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            classifier.git(root, 'init', '-q')
            classifier.git(root, 'config', 'user.name', 'Release test')
            classifier.git(root, 'config', 'user.email', 'release-test@example.invalid')
            for path in classifier.PROTECTED:
                target = root / path
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text('protected source')
            migrations = root / classifier.MIGRATIONS
            migrations.mkdir(parents=True)
            (migrations / 'V80__fixture.sql').write_text('SELECT 1;')
            classifier.git(root, 'add', '.')
            classifier.git(root, 'commit', '-qm', 'Deployed V80 baseline')
            base = classifier.git(root, 'rev-parse', 'HEAD').decode().strip()
            for name in ['V81__email_password_recovery.sql', 'V82__password_recovery_token_revocation.sql']:
                (migrations / name).write_bytes((source / classifier.MIGRATIONS / name).read_bytes())
            for path in ['.cicd-backup/ci.yml', '.cicd-backup/deploy.yml']:
                target = root / path
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes((source / path).read_bytes())
            classifier.git(root, 'add', '.')
            classifier.git(root, 'commit', '-qm', 'Email recovery with archived workflows')
            commit = classifier.git(root, 'rev-parse', 'HEAD').decode().strip()
            plan = classifier.classify(root, commit, base, base)
            self.assertEqual(plan['classification'], 'DATABASE_MIGRATION')
            self.assertEqual([row['version'] for row in plan['migrations']], ['80', '81', '82'])
            self.assertIn('.cicd-backup/ci.yml', plan['changes'])
            self.assertIn('.cicd-backup/deploy.yml', plan['changes'])

    def test_deleted_or_edited_migrations_fail_closed(self):
        with patch.object(classifier, 'revision'), patch.object(classifier.subprocess, 'run'), \
             patch.object(classifier, 'inventory', side_effect=[MIGRATIONS, []]):
            with self.assertRaises(ValueError):
                classifier.classify(Path('.'), SHA, 'f' * 40, 'f' * 40)

    def test_nonancestor_baseline_fails_closed(self):
        with patch.object(classifier, 'revision'), patch.object(classifier.subprocess, 'run', side_effect=subprocess.CalledProcessError(1, 'git')):
            with self.assertRaises(subprocess.CalledProcessError):
                classifier.classify(Path('.'), SHA, 'f' * 40, 'f' * 40)

    def test_missing_or_nonsha_revision_fails_closed(self):
        with self.assertRaises(ValueError):
            classifier.revision(Path('.'), 'latest')

    def test_repository_inventory_is_complete_and_deterministic(self):
        root = Path(__file__).resolve().parents[2]
        sha = classifier.git(root, 'rev-parse', 'HEAD').decode().strip()
        rows = classifier.inventory(root, sha)
        self.assertTrue(rows)
        paths = classifier.git(root, 'ls-tree', '-r', '--name-only', sha, '--', classifier.MIGRATIONS).decode().splitlines()
        self.assertEqual(len(rows), len(paths))
        self.assertEqual(rows, classifier.inventory(root, sha))

    def test_history_checksums_and_pending_schema(self):
        rows = [dict(MIGRATIONS[0], success=True, type='SQL')]
        self.assertEqual(classifier.validate_history(rows, MIGRATIONS), '79')
        for change in [dict(checksum=0), dict(success=False), dict(type='BASELINE'), dict(script='different.sql')]:
            with self.subTest(change=change), self.assertRaises(ValueError):
                classifier.validate_history([dict(rows[0], **change)], MIGRATIONS)
        pending = MIGRATIONS + [dict(version='80', script='V80__new.sql', checksum=456, sha256='a' * 64)]
        with self.assertRaises(ValueError):
            classifier.validate_history(rows, pending)
        self.assertEqual(classifier.validate_history(rows, pending, allow_pending=True), '79')
        with self.assertRaises(ValueError):
            classifier.validate_history([], MIGRATIONS)


class ClassifiedReleaseTests(unittest.TestCase):
    setup_release = fixtures.ReleaseSafetyTests.setup_release
    fake_run = fixtures.ReleaseSafetyTests.fake_run

    def configure(self, staged, kind):
        plan = json.loads((staged / 'deployment-plan.json').read_text())
        plan['classification'] = kind
        plan['changes'] = ['frontend/src/app/App.tsx'] if kind == 'FRONTEND_ONLY' else ['backend/src/main/java/com/edurite/tutor/service/TutorService.java']
        release.write_json(staged / 'deployment-plan.json', plan)

    def test_frontend_needs_no_restore_or_ses_attestation_and_never_touches_data_services(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(release.os.environ):
            args, project, staged, state = self.setup_release(directory)
            self.configure(staged, 'FRONTEND_ONLY')
            args.backup = args.evidence = None
            (state / 'production-checkout-review.json').unlink()
            calls = []
            with patch.object(release, 'run', side_effect=self.fake_run(calls)):
                release.execute(args, project, staged, state)
            self.assertEqual([c[-1] for c in calls if 'up' in c], ['frontend'])
            self.assertEqual([c[-1] for c in calls if 'pull' in c], ['frontend'])
            self.assertFalse(any('exec' in c and ('postgres' in c or 'redis' in c or 'backend' in c) for c in calls))
            self.assertFalse(any('build' in c or 'down' in c for c in calls))
            self.assertTrue(any(c[0] == 'curl' and c[-1] == 'https://edurite.co.za/' for c in calls))
            self.assertEqual((project / '.env').read_text(), 'preserve me')

    def test_frontend_failure_rolls_back_only_frontend(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(release.os.environ):
            args, project, staged, state = self.setup_release(directory)
            self.configure(staged, 'FRONTEND_ONLY')
            calls, delegate = [], self.fake_run([])
            failed = False
            def runner(*command, **kwargs):
                nonlocal failed
                calls.append(command)
                if 'up' in command and not failed:
                    failed = True
                    raise RuntimeError('synthetic unhealthy frontend')
                return delegate(*command, **kwargs)
            with patch.object(release, 'run', side_effect=runner), self.assertRaises(RuntimeError):
                release.execute(args, project, staged, state)
            self.assertEqual([c[-1] for c in calls if 'up' in c], ['frontend', 'frontend'])
            self.assertEqual(release.os.environ['FRONTEND_IMAGE'], SNAPSHOT['images']['frontend'])
            self.assertEqual(json.loads(next(state.glob(SHA + '-*.json')).read_text())['status'], 'rolled-back')

    def test_recorded_frontend_rollback_needs_no_schema_review(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(release.os.environ):
            args, project, staged, state = self.setup_release(directory)
            self.configure(staged, 'FRONTEND_ONLY')
            with patch.object(release, 'run', side_effect=self.fake_run([])):
                release.execute(args, project, staged, state)
            args.action, args.state = 'rollback', next(state.glob(SHA + '-*.json'))
            calls = []
            with patch.object(release, 'run', side_effect=self.fake_run(calls)):
                release.execute(args, project, staged, state)
            self.assertEqual([c[-1] for c in calls if 'up' in c], ['frontend'])
            self.assertFalse(any('pull' in c for c in calls))

    def test_backend_no_schema_uses_history_without_restore(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(release.os.environ):
            args, project, staged, state = self.setup_release(directory)
            self.configure(staged, 'BACKEND_NO_SCHEMA_CHANGE')
            args.backup = args.evidence = None
            calls = []
            with patch.object(release, 'run', side_effect=self.fake_run(calls)):
                release.execute(args, project, staged, state)
            self.assertTrue(any('flyway_schema_history' in ' '.join(c) for c in calls))
            self.assertEqual([c[-1] for c in calls if 'up' in c], ['backend', 'frontend'])

    def test_backend_pending_or_drifted_schema_blocks_replacement(self):
        with tempfile.TemporaryDirectory() as directory:
            args, project, staged, state = self.setup_release(directory)
            self.configure(staged, 'BACKEND_NO_SCHEMA_CHANGE')
            calls, delegate = [], self.fake_run([])
            def runner(*command, **kwargs):
                calls.append(command)
                if 'SELECT coalesce(json_agg' in ' '.join(command):
                    return '[]'
                return delegate(*command, **kwargs)
            with patch.object(release, 'run', side_effect=runner), self.assertRaises(ValueError):
                release.execute(args, project, staged, state)
            self.assertFalse(any('up' in c for c in calls))

    def test_migration_cannot_bypass_approval_or_restore(self):
        for field in ['migration_approved', 'backup', 'evidence']:
            with self.subTest(field=field), tempfile.TemporaryDirectory() as directory:
                args, project, staged, state = self.setup_release(directory)
                setattr(args, field, False if field == 'migration_approved' else None)
                calls = []
                with patch.object(release, 'run', side_effect=self.fake_run(calls)), self.assertRaises(ValueError):
                    release.execute(args, project, staged, state)
                self.assertFalse(any('up' in c for c in calls))

    def test_migration_evidence_must_match_live_source_and_inventory(self):
        for change in [dict(source_version='78'), dict(target_version='80'), dict(migrations=[])]:
            with self.subTest(change=change), tempfile.TemporaryDirectory() as directory:
                args, project, staged, state = self.setup_release(directory)
                evidence = json.loads(args.evidence.read_text())
                release.write_json(args.evidence, dict(evidence, **change))
                calls = []
                with patch.object(release, 'run', side_effect=self.fake_run(calls)), self.assertRaises(ValueError):
                    release.execute(args, project, staged, state)
                self.assertFalse(any('up' in c for c in calls))

    def test_classification_downgrade_fails(self):
        with tempfile.TemporaryDirectory() as directory:
            args, project, staged, state = self.setup_release(directory)
            plan = json.loads((staged / 'deployment-plan.json').read_text())
            plan['classification'] = 'FRONTEND_ONLY'
            release.write_json(staged / 'deployment-plan.json', plan)
            with patch.object(release, 'run') as runner, self.assertRaises(ValueError):
                release.execute(args, project, staged, state)
            runner.assert_not_called()

    def test_changed_production_inventory_invalidates_plan(self):
        with tempfile.TemporaryDirectory() as directory:
            args, project, staged, state = self.setup_release(directory)
            with patch.object(release, 'inspect_running', return_value=dict(SNAPSHOT, project='different')), self.assertRaises(ValueError):
                release.execute(args, project, staged, state)


    def test_backend_rollback_without_schema_review(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(release.os.environ):
            args, project, staged, state = self.setup_release(directory)
            self.configure(staged, 'BACKEND_NO_SCHEMA_CHANGE')
            with patch.object(release, 'run', side_effect=self.fake_run([])):
                release.execute(args, project, staged, state)
            args.action, args.state = 'rollback', next(state.glob(SHA + '-*.json'))
            calls = []
            with patch.object(release, 'run', side_effect=self.fake_run(calls)):
                release.execute(args, project, staged, state)
            self.assertFalse(any('pull' in c for c in calls))
            self.assertEqual([c[-1] for c in calls if 'up' in c], ['backend', 'frontend'])

    def test_frontend_rollback_refuses_configuration_or_backend_drift(self):
        for drift in ('env', 'backend'):
            with self.subTest(drift=drift), tempfile.TemporaryDirectory() as directory, patch.dict(release.os.environ):
                args, project, staged, state = self.setup_release(directory)
                self.configure(staged, 'FRONTEND_ONLY')
                with patch.object(release, 'run', side_effect=self.fake_run([])):
                    release.execute(args, project, staged, state)
                args.action, args.state = 'rollback', next(state.glob(SHA + '-*.json'))
                snapshot = copy.deepcopy(SNAPSHOT)
                if drift == 'env':
                    (project / '.env').write_text('changed configuration')
                else:
                    snapshot['containers']['backend'] = 'different-backend'
                calls = []
                with patch.object(release, 'inspect_running', return_value=snapshot), \
                     patch.object(release, 'run', side_effect=self.fake_run(calls)), self.assertRaises(ValueError):
                    release.execute(args, project, staged, state)
                self.assertFalse(any('up' in c for c in calls))


    def test_ses_review_reused_only_for_identical_production_and_release_sources(self):
        for change in (False, True):
            with self.subTest(source_changed=change), tempfile.TemporaryDirectory() as directory, patch.dict(release.os.environ):
                args, project, staged, state = self.setup_release(directory)
                self.configure(staged, 'BACKEND_NO_SCHEMA_CHANGE')
                path = state / 'production-checkout-review.json'
                review = json.loads(path.read_text())
                review.pop('commit')
                review['release_files'] = dict(MANIFEST['protected_files'])
                if change:
                    review['release_files'][release.PROTECTED[0]] = '0' * 64
                release.write_json(path, review)
                calls = []
                with patch.object(release, 'run', side_effect=self.fake_run(calls)):
                    if change:
                        with self.assertRaises(ValueError):
                            release.execute(args, project, staged, state)
                    else:
                        release.execute(args, project, staged, state)
                if change:
                    self.assertFalse(any('up' in c for c in calls))

    def test_stale_application_rollback_refused(self):
        for kind in ('FRONTEND_ONLY', 'BACKEND_NO_SCHEMA_CHANGE'):
            with self.subTest(kind=kind), tempfile.TemporaryDirectory() as directory, patch.dict(release.os.environ):
                args, project, staged, state = self.setup_release(directory)
                self.configure(staged, kind)
                with patch.object(release, 'run', side_effect=self.fake_run([])):
                    release.execute(args, project, staged, state)
                args.action, args.state = 'rollback', next(state.glob(SHA + '-*.json'))
                snapshot = copy.deepcopy(SNAPSHOT)
                snapshot['images']['frontend'] = 'sha256:' + '0' * 64
                calls = []
                with patch.object(release, 'inspect_running', return_value=snapshot), \
                     patch.object(release, 'run', side_effect=self.fake_run(calls)), self.assertRaises(ValueError):
                    release.execute(args, project, staged, state)
                self.assertFalse(any('up' in c for c in calls))


if __name__ == '__main__':
    unittest.main()
