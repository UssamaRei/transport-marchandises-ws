"""Verify the real UI against the disposable four-backend integration stack."""
import argparse
import os
import shutil
import socket
import subprocess
import sys
from run import ROOT, compose, wait_url


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--reuse-stack', action='store_true', help='Reuse the stack already verified with run.py --keep.')
    args = parser.parse_args()
    npm = shutil.which('npm.cmd' if os.name == 'nt' else 'npm')
    node = shutil.which('node')
    if not npm or not node:
        raise RuntimeError('Node.js and npm are required.')
    try:
        probe = socket.create_connection(('localhost', 5173), timeout=1)
    except OSError:
        pass
    else:
        probe.close()
        raise RuntimeError('Port 5173 is in use. Stop the current frontend before running the isolated UI tests.')
    env = os.environ.copy()
    env.update(VITE_KEYCLOAK_URL='http://localhost:18090', VITE_API_BASE_URL='',
               CAMIONS_PROXY_TARGET='http://127.0.0.1:18081', CHARGEMENTS_PROXY_TARGET='http://127.0.0.1:18082',
               BONS_PROXY_TARGET='http://127.0.0.1:18083', TRANSPORTS_PROXY_TARGET='http://127.0.0.1:18084',
               E2E_ISOLATED_STACK='1')
    folder = ROOT / 'frontend'
    output = ROOT / '.tools/frontend-systeme'
    output.mkdir(parents=True, exist_ok=True)
    server = None
    try:
        if not args.reuse_stack:
            subprocess.run([sys.executable, str(ROOT / 'tests/integration/run.py'), '--keep'], cwd=ROOT, check=True)
        subprocess.run([npm, 'ci', '--ignore-scripts'], cwd=folder, check=True)
        subprocess.run([npm, 'run', 'build'], cwd=folder, check=True)
        with (output / 'vite.log').open('w', encoding='utf-8') as log:
            server = subprocess.Popen([node, str(folder / 'node_modules/vite/bin/vite.js')], cwd=folder, env=env,
                                      stdout=log, stderr=subprocess.STDOUT,
                                      creationflags=subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0)
            wait_url('http://localhost:5173')
            subprocess.run([npm, 'run', 'test:e2e'], cwd=folder, env=env, check=True)
    finally:
        if server:
            server.terminate()
            try: server.wait(timeout=10)
            except subprocess.TimeoutExpired: server.kill(); server.wait()
        result = compose('logs', '--no-color', check=False)
        (output / 'containers.log').write_text(result.stdout + result.stderr, encoding='utf-8')
        compose('down', '--volumes', '--remove-orphans')


if __name__ == '__main__':
    main()
