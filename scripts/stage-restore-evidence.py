"""Verify/install an actual isolated-restore result. Never connects to a database.

Run on EC2 after securely uploading the evidence generated on a separate machine.
The deployment lock prevents a release from observing a partially installed pair.
A crash between replacements fails closed on the backup hash at the next check.
"""
import argparse
import json
from pathlib import Path
import shutil
import tempfile
from release import load_restore_evidence, validate_manifest


def stage(backup, evidence, manifest, destination, check_only=False):
    record = load_restore_evidence(backup, evidence, manifest)
    if check_only:
        return record
    destination.mkdir(mode=0o700, parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='.restore-stage-', dir=destination) as work:
        work = Path(work)
        for source, name in ((backup, 'release.dump'), (evidence, 'restore-evidence.json')):
            target = work / name
            shutil.copyfile(source, target)
            target.chmod(0o600)
        # Recheck the copied bytes, including any change during upload/copy.
        load_restore_evidence(work / 'release.dump', work / 'restore-evidence.json', manifest)
        for name in ('release.dump', 'restore-evidence.json'):
            (work / name).replace(destination / name)
    return record


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--release-dir', type=Path, required=True)
    parser.add_argument('--commit', required=True)
    parser.add_argument('--backup', type=Path, required=True)
    parser.add_argument('--evidence', type=Path, required=True)
    parser.add_argument('--destination', type=Path, default=Path('/opt/edurite-backups'))
    parser.add_argument('--state-dir', type=Path, default=Path('/opt/edurite-release-state'))
    parser.add_argument('--check-only', action='store_true')
    args = parser.parse_args()
    manifest = json.loads((args.release_dir / 'release.json').read_text())
    validate_manifest(manifest, args.commit)
    import fcntl
    args.state_dir.mkdir(mode=0o700, parents=True, exist_ok=True)
    with (args.state_dir / 'deployment.lock').open('a') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        record = stage(args.backup, args.evidence, manifest, args.destination, args.check_only)
    print(f'Verified backup SHA-256 {record["backup_sha256"]}; commit {args.commit}; '
          f'isolated migration {record["source_version"]} -> {record["target_version"]}')


if __name__ == '__main__':
    main()
