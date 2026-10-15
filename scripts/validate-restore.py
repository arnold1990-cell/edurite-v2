"""Restore a custom-format V74 dump to a NEW private local cluster, then test migrations.

Run on an isolated validation machine, never the production EC2 host. Requires native
PostgreSQL tools, Java 21+, Maven and this exact release checkout. Never accepts a DB URL.
"""
import argparse
from datetime import datetime, timedelta, timezone
import json
import os
from pathlib import Path
import secrets
import re
import shutil
import socket
import subprocess
import tempfile
import time
from release import digest, run, write_json


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--backup', type=Path, required=True)
    parser.add_argument('--commit', required=True)
    parser.add_argument('--pg-bin', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--archive-utc-offset-minutes', type=int, default=0,
                        help='Offset of the dump creation timezone; produce production dumps with TZ=UTC')
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    if run('git', '-C', str(root), 'rev-parse', 'HEAD') != args.commit:
        raise ValueError('Checkout does not match release commit')
    if run('git', '-C', str(root), 'status', '--porcelain', '--', 'backend', 'scripts/RestoreValidation.java', 'scripts/validate-restore.py'):
        raise ValueError('Restore validation requires committed backend and validation tools')
    backup_hash = digest(args.backup)
    suffix = '.exe' if os.name == 'nt' else ''
    pg = lambda name: str(args.pg_bin / (name + suffix))
    archive = run(pg('pg_restore'), '--list', str(args.backup.resolve()))
    created = re.search(r'^; Archive created at (\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})', archive, re.MULTILINE)
    if not created or not -720 <= args.archive_utc_offset_minutes <= 840:
        raise ValueError('Missing archive creation timestamp or invalid timezone offset')
    backup_created = datetime.fromisoformat(created.group(1)).replace(
        tzinfo=timezone(timedelta(minutes=args.archive_utc_offset_minutes))).timestamp()
    if not 0 <= time.time() - backup_created <= 86400:
        raise ValueError('Backup archive must be less than 24 hours old; copied file timestamps are not evidence')
    password = secrets.token_urlsafe(32)
    # No production credentials are read; this password exists only for this cluster.
    os.environ['PGPASSWORD'] = password
    with tempfile.TemporaryDirectory(prefix='edurite-restore-') as directory:
        work = Path(directory)
        pwfile = work / 'password'
        pwfile.write_text(password)
        pwfile.chmod(0o600)
        data = work / 'data'
        run(pg('initdb'), '-D', str(data), '-U', 'restore_validator', '--pwfile', str(pwfile), '-A', 'scram-sha-256', '--encoding=UTF8', '--locale=C')
        with socket.socket() as listener:
            listener.bind(('127.0.0.1', 0))
            port = listener.getsockname()[1]
        # pg_ctl refuses to reuse another cluster's port. No fallback to another DB.
        run(pg('pg_ctl'), '-D', str(data), '-l', str(work / 'postgres.log'), '-o', f'-p {port} -h 127.0.0.1', '-w', 'start')
        try:
            common = ['-h', '127.0.0.1', '-p', str(port), '-U', 'restore_validator']
            run(pg('createdb'), *common, 'restore_validation')
            run(pg('pg_restore'), *common, '--no-owner', '--no-privileges', '--exit-on-error', '-d', 'restore_validation', str(args.backup.resolve()))
            mvn = shutil.which('mvn.cmd' if os.name == 'nt' else 'mvn')
            classpath_file = work / 'classpath.txt'
            run(mvn, '-q', '-f', str(root / 'backend/pom.xml'), 'dependency:build-classpath', f'-Dmdep.outputFile={classpath_file}')
            classpath = classpath_file.read_text().strip()
            run('javac', '-cp', classpath, '-d', str(work), str(root / 'scripts/RestoreValidation.java'))
            output = run('java', '-cp', str(work) + os.pathsep + classpath, 'RestoreValidation',
                         f'jdbc:postgresql://127.0.0.1:{port}/restore_validation', str(root / 'backend/src/main/resources/db/migration'))
            if 'RESTORE_DATA_PRESERVED' not in output:
                raise RuntimeError('Restore validation did not complete')
            if digest(args.backup) != backup_hash:
                raise ValueError('Backup changed during restore validation')
            write_json(args.output, dict(commit=args.commit, backup_sha256=backup_hash,
                       backup_created_at=backup_created, validated_at=time.time(), restored=True,
                       migrated=True, data_preserved=True, source_version=74, target_version=79))
            print('Restore, V74 to V79 migration and existing-row preservation verified; evidence written.')
        finally:
            run(pg('pg_ctl'), '-D', str(data), '-m', 'fast', '-w', 'stop')


if __name__ == '__main__':
    main()
