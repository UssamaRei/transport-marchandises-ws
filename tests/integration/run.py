"""Build and test the four real backends in a disposable, local Docker stack."""
import argparse
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from urllib.error import HTTPError, URLError
from urllib.request import urlopen

ROOT = Path(__file__).resolve().parents[2]
PRIVATE = ROOT / '.secrets' / 'systeme-test.env'
OUTPUT = ROOT / '.tools' / 'systeme-test'
PROJECT = 'ws-systeme-test'


def compose(*args, check=True):
    return subprocess.run(
        ['docker', 'compose', '-p', PROJECT, '--env-file', str(PRIVATE),
         '-f', str(ROOT / 'tests/integration/compose.yaml'), *args],
        cwd=ROOT, capture_output=True, text=True, encoding='utf-8', errors='replace', check=check)


def prepare():
    PRIVATE.parent.mkdir(exist_ok=True)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    if not PRIVATE.exists():
        keys = ['TEST_DB_PASSWORD', 'KEYCLOAK_ADMIN_PASSWORD',
                'CHARGEMENTS_CLIENT_SECRET', 'CAMIONS_CLIENT_SECRET',
                'BONS_CLIENT_SECRET', 'TRANSPORTS_CLIENT_SECRET',
                'WS_ADMIN_PASSWORD', 'WS_AGENT_PASSWORD', 'WS_LECTEUR_PASSWORD']
        PRIVATE.write_text(''.join(f'{key}={secrets.token_hex(24)}\n' for key in keys), encoding='utf-8')


def wait_url(url, statuses=(200,), timeout=150):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            with urlopen(url, timeout=3) as response:
                status = response.status
        except HTTPError as error:
            status = error.code
        except (URLError, TimeoutError, OSError):
            status = None
        if status in statuses:
            return
        time.sleep(1)
    raise RuntimeError(f'Service not ready: {url}')


def build():
    maven = shutil.which('mvn.cmd' if os.name == 'nt' else 'mvn')
    if not maven:
        raise RuntimeError('Install Maven and Java 21 first.')
    values = dict(line.split('=', 1) for line in PRIVATE.read_text().splitlines() if '=' in line)
    env = os.environ.copy()
    env.update(FLOTTE_TEST_DATABASE_URL='jdbc:postgresql://localhost:15432/flotte_test',
               FLOTTE_TEST_DATABASE_USERNAME='postgres',
               FLOTTE_TEST_DATABASE_PASSWORD=values['TEST_DB_PASSWORD'])
    counts = {}
    for module in ['flotte', 'chargements', 'bons', 'transports']:
        print(f'Building and verifying {module}...', flush=True)
        with (OUTPUT / f'{module}-verify.log').open('w', encoding='utf-8') as log:
            subprocess.run([maven, '-B', 'clean', 'verify'], cwd=ROOT / 'services' / module,
                           env=env, stdout=log, stderr=subprocess.STDOUT, check=True)
        totals = dict(tests=0, failures=0, errors=0, skipped=0)
        for report in (ROOT / 'services' / module / 'target/surefire-reports').glob('TEST-*.xml'):
            attrs = ET.parse(report).getroot().attrib
            for key in totals:
                totals[key] += int(attrs.get(key, 0))
        if totals['tests'] == 0 or totals['skipped']:
            raise RuntimeError(f'{module}: expected tests to run without skips')
        counts[module] = totals
        print(f"{module}: {totals['tests']} tests passed, none skipped.", flush=True)
    (OUTPUT / 'maven-resultats.json').write_text(json.dumps(counts, indent=2), encoding='utf-8')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--skip-build', action='store_true', help='Reuse the current packaged JARs.')
    parser.add_argument('--keep', action='store_true', help='Keep the isolated stack after the test.')
    parser.add_argument('--down', action='store_true', help='Remove only this isolated test stack.')
    args = parser.parse_args()
    prepare()
    if args.down:
        compose('down', '--volumes', '--remove-orphans')
        return
    try:
        (OUTPUT / 'resultats.json').write_text(json.dumps({'passed': False, 'phase': 'startup'}))
        # This project contains only disposable test containers and data.
        compose('down', '--volumes', '--remove-orphans')
        compose('up', '-d', '--wait', 'db')
        if not args.skip_build:
            build()
        compose('up', '-d')
        wait_url('http://localhost:18090/realms/transport-ws/.well-known/openid-configuration')
        for port in [18081, 18082, 18083, 18084]:
            wait_url(f'http://127.0.0.1:{port}/api/v1/not-a-route', (401, 403, 404))
        print('Four real backends + Keycloak + one PostgreSQL database ready.', flush=True)
        result = subprocess.run([sys.executable, str(ROOT / 'tests/integration/test_system.py')], cwd=ROOT)
        if result.returncode:
            raise RuntimeError('Integration tests failed; see the results and container logs.')
    finally:
        logs = compose('logs', '--no-color', check=False)
        (OUTPUT / 'containers.log').write_text(logs.stdout + logs.stderr, encoding='utf-8')
        if not args.keep:
            compose('down', '--volumes', '--remove-orphans')


if __name__ == '__main__':
    main()
