"""Local-only OWASP ZAP runtime; credentials and raw HTTP material stay private."""
from pathlib import Path
import json
import secrets
import subprocess
import time
from urllib.parse import urlencode
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[2]
PRIVATE = ROOT / '.secrets/stage4'
OUT = Path('C:/Users/pc/Desktop/WS/DAT_Transport/Preuves/Tache_4_Securite')
NETWORK = 'ws-systeme-test_default'
API = 'http://127.0.0.1:18085'


def docker(*args, check=True):
    result = subprocess.run(['docker', *map(str, args)], cwd=ROOT, capture_output=True,
                            text=True, encoding='utf-8', errors='replace')
    if check and result.returncode:
        raise RuntimeError(result.stderr.strip())
    return result


def setup():
    PRIVATE.mkdir(parents=True, exist_ok=True)
    (PRIVATE / 'reports').mkdir(exist_ok=True)
    OUT.mkdir(parents=True, exist_ok=True)
    keyfile = PRIVATE / 'api-key.txt'
    if not keyfile.exists():
        keyfile.write_text(secrets.token_hex(32), encoding='utf-8')
    (PRIVATE / 'zap.properties').write_text(
        'api.key=' + keyfile.read_text().strip() + '\n'
        'api.addrs.addr.name=.*\napi.addrs.addr.regex=true\n'
        'connection.timeoutInSecs=10\n', encoding='utf-8')
    if not (ROOT / 'frontend/dist/index.html').exists():
        raise RuntimeError('Build the frontend first with npm run build.')
    ensure_container('ws-dat-frontend', ['--network', NETWORK,
                     '-v', f'{ROOT / "frontend/dist"}:/usr/share/nginx/html:ro',
                     '-v', f'{ROOT / "quality/security/nginx-test.conf"}:/etc/nginx/conf.d/default.conf:ro',
                     'nginx:stable-alpine'])
    ensure_container('ws-dat-zap', ['--network', NETWORK, '--memory', '1536m', '--cpus', '2',
                     '-p', '127.0.0.1:18085:8080', '-v', f'{PRIVATE}:/zap/wrk:rw',
                     'ghcr.io/zaproxy/zaproxy:stable', 'zap.sh', '-daemon', '-host', '0.0.0.0',
                     '-port', '8080', '-silent', '-configfile', '/zap/wrk/zap.properties'])
    deadline = time.monotonic() + 180
    while time.monotonic() < deadline:
        try:
            version = call('core', 'view', 'version')
            break
        except RuntimeError:
            time.sleep(2)
    else:
        raise RuntimeError('ZAP API did not become ready. Inspect its private Docker logs.')
    images = {}
    for image in ('ghcr.io/zaproxy/zaproxy:stable', 'nginx:stable-alpine'):
        info = json.loads(docker('image', 'inspect', image).stdout)[0]
        images[image] = {'id': info['Id'], 'digests': info.get('RepoDigests', [])}
    data = {'dateDebutUTC': __import__('datetime').datetime.now(__import__('datetime').timezone.utc).isoformat(),
            'zap': version, 'images': images, 'apiControle': '127.0.0.1:18085, clé privée',
            'reseau': NETWORK, 'frontend': 'Bundle construit, servi par un Nginx provisoire de test. Aucun serveur de production n’est certifié par cette campagne.',
            'base': 'PostgreSQL 18 local isolé ; Neon exclu'}
    (OUT / 'environnement.json').write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')
    return version


def ensure_container(name, options):
    existing = docker('inspect', name, check=False)
    if existing.returncode == 0:
        info = json.loads(existing.stdout)[0]
        if info.get('Config', {}).get('Labels', {}).get('dat.task') != '4':
            raise RuntimeError('Container name already used outside this DAT task: ' + name)
        docker('start', name)
    else:
        docker('run', '-d', '--name', name, '--label', 'dat.task=4', *options)


def call(component, kind, operation, fmt='JSON', **params):
    params['apikey'] = (PRIVATE / 'api-key.txt').read_text().strip()
    url = f'{API}/{fmt}/{component}/{kind}/{operation}/?' + urlencode(params)
    try:
        with urlopen(Request(url, headers={'Host': 'zap'}), timeout=60) as response:
            raw = response.read()
        if fmt != 'JSON':
            return raw
        data = json.loads(raw)
        if isinstance(data, dict) and 'code' in data:
            raise RuntimeError(f'ZAP {component}/{operation}: {data.get("code")} {data.get("message", "")}')
        return data
    except RuntimeError:
        raise
    except Exception as error:
        raise RuntimeError(f'ZAP {component}/{operation}: {type(error).__name__}') from None


if __name__ == '__main__':
    print(json.dumps(setup()))
