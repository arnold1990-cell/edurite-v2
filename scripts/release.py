"""Controlled application-only release. No checkout mutation, image builds or DB rollback."""
import argparse
import hashlib
import json
import os
import posixpath
from pathlib import Path
import re
import subprocess
import time


def run(*args, input=None):
    if Path(args[0]).stem == 'pg_ctl':
        # On Windows the server can inherit pg_ctl's output pipe and keep communicate()
        # waiting after pg_ctl exits. Server logs already go to the private -l file.
        result = subprocess.run(args, stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL,
                                stderr=subprocess.DEVNULL, timeout=120)
        if result.returncode:
            raise RuntimeError('Isolated PostgreSQL cluster start/stop failed')
        return ''
    result = subprocess.run(args, input=input, text=True, capture_output=True, timeout=3600)
    if result.returncode:
        # Docker/config/SQL errors may contain environment values or learner data.
        raise RuntimeError(f"Command failed: {args[0]} (exit {result.returncode}); inspect privately on host")
    return result.stdout.strip()


def digest(path):
    h = hashlib.sha256()
    with Path(path).open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()


def validate_manifest(manifest, expected):
    if not re.fullmatch(r'[0-9a-f]{40}', expected) or manifest.get('commit') != expected:
        raise ValueError('Release commit does not match selected full SHA')
    for service in ('backend', 'frontend'):
        if not re.fullmatch(r'ghcr\.io/[a-z0-9_./-]+@sha256:[0-9a-f]{64}', manifest.get(service, '')):
            raise ValueError(f'{service} must use a GHCR image digest')


def validate_backup(evidence, backup, commit, now=None):
    now = time.time() if now is None else now
    for field in ('validated_at', 'backup_created_at'):
        age = now - float(evidence.get(field, 0))
        if age < 0 or age > 86400:
            raise ValueError('Backup and restore validation must be less than 24 hours old')
    if evidence.get('commit') != commit or evidence.get('restored') is not True or evidence.get('migrated') is not True:
        raise ValueError('Missing restore and migration evidence for selected release')
    if evidence.get('backup_sha256') != digest(backup):
        raise ValueError('Backup differs from the successfully restored backup')
    if evidence.get('data_preserved') is not True:
        raise ValueError('Restored data preservation was not verified')


def write_json(path, data):
    temporary = path.with_suffix('.tmp')
    temporary.write_text(json.dumps(data, indent=2) + '\n')
    temporary.replace(path)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('action', choices=['deploy', 'rollback'])
    parser.add_argument('--release-dir', required=True, type=Path)
    parser.add_argument('--project-dir', required=True, type=Path)
    parser.add_argument('--commit', required=True)
    parser.add_argument('--backup', type=Path)
    parser.add_argument('--evidence', type=Path)
    parser.add_argument('--state', type=Path, help='Deployment state to roll back')
    parser.add_argument('--schema-compatibility-reviewed', action='store_true')
    args = parser.parse_args()
    project, release = args.project_dir.resolve(), args.release_dir.resolve()
    if project == release:
        raise ValueError('Release must be staged outside the production checkout')
    state_dir = project.parent / 'edurite-release-state'
    state_dir.mkdir(mode=0o700, exist_ok=True)
    # Also serializes direct CLI invocations, not just Actions jobs.
    import fcntl
    with (state_dir / 'deployment.lock').open('w') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        execute(args, project, release, state_dir)


def execute(args, project, release, state_dir):
    manifest = json.loads((release / 'release.json').read_text())
    validate_manifest(manifest, args.commit)
    if not (project / '.env').is_file():
        raise ValueError('Existing production .env is required')
    env_hash = digest(project / '.env')
    compose_hashes = {name: digest(release / name) for name in ('docker-compose.yml', 'docker-compose.production.yml')}
    protected = ['backend/pom.xml', 'backend/src/main/java/com/edurite/auth/service/EmailVerificationService.java']
    checkout_hashes = {name: digest(project / name) for name in protected}
    # The operator creates this file only after reviewing the production SES diff.
    review = json.loads((state_dir / 'production-checkout-review.json').read_text())
    if review.get('commit') != args.commit or review.get('files') != checkout_hashes:
        raise ValueError('Production SES differences must be reviewed for this release')
    previous = None
    if args.action == 'deploy':
        if not args.backup or not args.evidence:
            raise ValueError('Backup and separate restore evidence are required')
        validate_backup(json.loads(args.evidence.read_text()), args.backup, args.commit)
    else:
        if not args.state or not args.schema_compatibility_reviewed:
            raise ValueError('Rollback requires recorded state and explicit schema compatibility review')
        previous = json.loads(args.state.read_text())
        if previous['commit'] != args.commit:
            raise ValueError('Rollback state belongs to another release')
        if previous['env_sha256'] != env_hash:
            raise ValueError('Environment changed since deployment; reconcile before rollback')
        if previous.get('compose_sha256') != compose_hashes:
            raise ValueError('Release Compose configuration changed since deployment')
        if any(not re.fullmatch(r'sha256:[0-9a-f]{64}', previous.get('previous', {}).get(service, '')) for service in ('backend', 'frontend')):
            raise ValueError('Rollback requires recorded immutable local image IDs')
        manifest = dict(manifest, **previous['previous'])
    os.environ['BACKEND_IMAGE'] = manifest['backend']
    os.environ['FRONTEND_IMAGE'] = manifest['frontend']
    # Discover existing Compose project; never guess a new volume/project name.
    project_name = run('docker', 'inspect', '--format', '{{index .Config.Labels "com.docker.compose.project"}}', 'edurite-postgres')
    if not re.fullmatch(r'[a-z0-9][a-z0-9_-]*', project_name):
        raise ValueError('Existing production Compose project not found')
    compose = ['docker', 'compose', '--project-directory', str(project), '--env-file', str(project / '.env'),
               '-p', project_name, '-f', str(release / 'docker-compose.yml'), '-f', str(release / 'docker-compose.production.yml')]
    config = json.loads(run(*compose, 'config', '--format', 'json'))
    backend_env = config['services']['backend']['environment']
    if 'prod' not in backend_env.get('SPRING_PROFILES_ACTIVE', '').split(','):
        raise ValueError('Production release requires the prod profile')
    for key in ('SPRING_DATASOURCE_PASSWORD',):
        if not backend_env.get(key):
            raise ValueError(f'Required configuration missing: {key}')
    if len(backend_env.get('JWT_SECRET', '').strip()) < 32:
        raise ValueError('Production requires a configured JWT_SECRET with at least 32 characters')
    providers = [backend_env.get('AI_PRIMARY_PROVIDER', 'gemini'), backend_env.get('AI_FALLBACK_PROVIDER', 'openai')]
    if any(re.sub('[^a-z0-9]', '', p.lower()).removesuffix('service') == 'gemini' for p in providers) and not backend_env.get('GEMINI_API_KEY', '').strip():
        raise ValueError('Configured Gemini provider requires its existing API key')
    if str(backend_env.get('GOOGLE_OAUTH_ENABLED', 'false')).lower() == 'true' and any(not backend_env.get(k) for k in ('GOOGLE_CLIENT_ID', 'GOOGLE_CLIENT_SECRET')):
        raise ValueError('Enabled Google OAuth requires its existing client configuration')
    if 'https://edurite.co.za' not in backend_env.get('APP_CORS_ALLOWED_ORIGINS', '').split(','):
        raise ValueError('Production CORS must allow the deployed edurite.co.za domain')
    if backend_env.get('APP_EMAIL_VERIFICATION_BASE_URL') != 'https://edurite.co.za' or backend_env.get('APP_EMAIL_VERIFICATION_AWS_REGION') != 'af-south-1' or backend_env.get('APP_EMAIL_VERIFICATION_FROM') != 'info@edurite.co.za':
        raise ValueError('Unexpected production SES configuration')
    if backend_env.get('SPRING_JPA_HIBERNATE_DDL_AUTO') != 'validate' or str(backend_env.get('SPRING_FLYWAY_ENABLED')).lower() != 'true':
        raise ValueError('Production requires schema validation and Flyway migrations')
    if any(backend_env.get(key) for key in ('AWS_ACCESS_KEY_ID', 'AWS_SECRET_ACCESS_KEY', 'AWS_SESSION_TOKEN')):
        raise ValueError('Production SES must use the IAM role, not static credentials')
    containers = {service: run(*compose, 'ps', '-q', service) for service in ('backend', 'frontend', 'postgres', 'redis')}
    if not all(containers.values()) or any('\n' in cid for cid in containers.values()):
        raise ValueError('Expected one existing running container per production service')
    old_images = {s: run('docker', 'inspect', '--format', '{{.Image}}', containers[s]) for s in ('backend', 'frontend')}
    # Record actual image IDs. Rollback also checks the environment and staged Compose hashes.
    def dependencies_healthy():
        run(*compose, 'exec', '-T', 'postgres', 'sh', '-c', 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -Atc "SELECT 1"')
        if run(*compose, 'exec', '-T', 'redis', 'redis-cli', 'ping') != 'PONG':
            raise RuntimeError('Redis health check failed')
    dependencies_healthy()
    if args.action == 'deploy':
        run(*compose, 'pull', 'backend', 'frontend')
        for service in ('backend', 'frontend'):
            revision = run('docker', 'image', 'inspect', '--format', '{{index .Config.Labels "org.opencontainers.image.revision"}}', manifest[service])
            if revision != args.commit:
                raise ValueError('Image revision does not match the approved release')
    else:
        for image in old_images.values():
            run('docker', 'image', 'inspect', image)
        # Local image IDs are immutable and retained; never pull them as registry names.
        for service in ('backend', 'frontend'):
            run('docker', 'image', 'inspect', manifest[service])
    storage = posixpath.normpath(posixpath.join('/app', backend_env.get('DOCUMENT_STORAGE_DIRECTORY', './data/uploads')))
    planned_mount = next((mount for mount in config['services']['backend'].get('volumes', []) if mount['target'] == storage), None)
    if not planned_mount or planned_mount['type'] != 'bind':
        raise ValueError('Production document storage requires an explicit persistent bind mount')
    mounts = json.loads(run('docker', 'inspect', '--format', '{{json .Mounts}}', containers['backend']))
    old_mount = next((mount for mount in mounts if mount['Destination'] == storage), None)
    if old_mount:
        if old_mount['Source'] != planned_mount['source']:
            raise ValueError('Upload storage source changed; reconcile existing files before deployment')
    else:
        # Fail closed if replacing this container would discard any documents. The
        # operator must copy and verify them to the persistent mount before release.
        run('docker', 'exec', containers['backend'], 'sh', '-c',
            'test ! -d "$1" || test -z "$(find "$1" -type f -print -quit)"', 'storage-check', storage)
    run('docker', 'run', '--rm', '--network', 'none', '--entrypoint', 'sh',
        '--mount', f'type=bind,source={planned_mount["source"]},target={storage}', manifest['backend'],
        '-c', 'test -w "$1"', 'storage-check', storage)
    state = dict(commit=args.commit, previous=old_images, selected={s: manifest[s] for s in old_images},
                 env_sha256=env_hash, compose_sha256=compose_hashes, checkout_sha256=checkout_hashes, project=project_name,
                 started_at=time.time(), status='deploying', action=args.action)
    state_path = state_dir / f'{args.commit}-{time.time_ns()}.json'
    write_json(state_path, state)
    try:
        run(*compose, 'up', '-d', '--no-build', '--no-deps', '--pull', 'never', '--wait', '--wait-timeout', '300', 'backend')
        run(*compose, 'exec', '-T', 'backend', 'curl', '--max-time', '10', '-fsS', 'http://127.0.0.1:8080/actuator/health/readiness')
        run(*compose, 'up', '-d', '--no-build', '--no-deps', '--pull', 'never', '--wait', '--wait-timeout', '120', 'frontend')
        run(*compose, 'exec', '-T', 'frontend', 'wget', '-T', '10', '-t', '1', '-qO-', 'http://127.0.0.1/')
        # Exercise Nginx -> backend as well as each container separately.
        run(*compose, 'exec', '-T', 'frontend', 'wget', '-T', '10', '-t', '1', '-qO-', 'http://127.0.0.1/actuator/health/readiness')
        dependencies_healthy()
        for service in ('postgres', 'redis'):
            if run(*compose, 'ps', '-q', service) != containers[service]:
                raise RuntimeError('Data service container unexpectedly changed')
        if digest(project / '.env') != env_hash or any(digest(project / p) != h for p, h in checkout_hashes.items()):
            raise RuntimeError('Protected production files changed during deployment')
        state['status'] = 'healthy'
    except Exception:
        state['status'] = 'failed-review-schema-before-rollback'
        raise
    finally:
        state['finished_at'] = time.time()
        write_json(state_path, state)
        print(f'Deployment result: {state["status"]}; record: {state_path}')


if __name__ == '__main__':
    main()
