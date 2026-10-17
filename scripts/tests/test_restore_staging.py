import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import release
import test_release as fixtures

MANIFEST = fixtures.MANIFEST

spec = importlib.util.spec_from_file_location('staging', Path(__file__).resolve().parents[1] / 'stage-restore-evidence.py')
staging = importlib.util.module_from_spec(spec)
spec.loader.exec_module(staging)


class RestoreStagingTests(unittest.TestCase):
    def fixture(self, directory):
        args, _, _, _ = fixtures.ReleaseSafetyTests().setup_release(directory)
        return args.backup, args.evidence, Path(directory) / 'destination'

    def test_verified_pair_is_installed_and_originals_preserved(self):
        with tempfile.TemporaryDirectory() as directory:
            backup, evidence, destination = self.fixture(directory)
            staging.stage(backup, evidence, MANIFEST, destination)
            self.assertEqual((destination / 'release.dump').read_bytes(), backup.read_bytes())
            self.assertEqual((destination / 'restore-evidence.json').read_bytes(), evidence.read_bytes())
            release.load_restore_evidence(destination / 'release.dump', destination / 'restore-evidence.json', MANIFEST)

    def test_invalid_evidence_cannot_replace_existing_pair(self):
        for change in ({'commit': 'b' * 40}, {'backup_sha256': '0' * 64},
                       {'source_version': 79}, {'target_version': '80'}, {'migrations': []},
                       {'validated_at': 0}, {'restored': False}, {'data_preserved': False}):
            with self.subTest(change=change), tempfile.TemporaryDirectory() as directory:
                backup, evidence, destination = self.fixture(directory)
                destination.mkdir()
                (destination / 'release.dump').write_bytes(b'previous backup')
                record = json.loads(evidence.read_text())
                release.write_json(evidence, dict(record, **change))
                with self.assertRaises(ValueError):
                    staging.stage(backup, evidence, MANIFEST, destination)
                self.assertEqual((destination / 'release.dump').read_bytes(), b'previous backup')

    def test_check_only_has_no_install_side_effects(self):
        with tempfile.TemporaryDirectory() as directory:
            backup, evidence, destination = self.fixture(directory)
            staging.stage(backup, evidence, MANIFEST, destination, check_only=True)
            self.assertFalse(destination.exists())

    def test_copy_race_is_rejected_before_replacement(self):
        with tempfile.TemporaryDirectory() as directory:
            backup, evidence, destination = self.fixture(directory)
            copy = staging.shutil.copyfile
            def corrupt(source, target):
                copy(source, target)
                if source == backup:
                    target.write_bytes(b'changed while copying')
            with patch.object(staging.shutil, 'copyfile', side_effect=corrupt), self.assertRaises(ValueError):
                staging.stage(backup, evidence, MANIFEST, destination)
            self.assertFalse((destination / 'release.dump').exists())

    def test_missing_and_same_paths_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            backup, evidence, _ = self.fixture(directory)
            for pair in ((None, evidence), (backup, None), (backup, backup), (backup, Path(directory) / 'missing')):
                with self.subTest(pair=pair), self.assertRaises(ValueError):
                    release.load_restore_evidence(*pair, MANIFEST)


if __name__ == '__main__':
    unittest.main()
