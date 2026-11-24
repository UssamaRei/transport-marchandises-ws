"""Authenticated passive analysis and five bounded, GET-only active scans."""
from datetime import datetime, timezone
import json
import re
import shutil
import sys
import time
from urllib.parse import quote, urlparse
from zap_runtime import ROOT, PRIVATE, OUT, call

sys.path.insert(0, str(ROOT / 'tests/integration'))
import test_system as support

POLICY = 'DAT-lectures'
CONTEXT = 'DAT-local'
SERVICES = {'camions': 8081, 'chargements': 8082, 'bons': 8083, 'transports': 8084}
FRONT = 'http://ws-dat-frontend:8080'
TARGETS = {
    'camions': 'http://camions:8081/api/v1/camions?page=0&size=2&disponible=true',
    'chargements': 'http://chargements:8082/api/v1/chargements?limit=2&offset=0',
    'bons': 'http://bons:8083/api/v1/bons?page=0&size=2&statut=EMIS',
    'transports': 'http://transports:8084/api/v1/transports?page=0&size=2',
    'frontend': FRONT + '/?recherche=transport',
}
last_refresh = 0.0
tokens = []


def refresh_token(force=False):
    global last_refresh
    if force or time.monotonic() - last_refresh > 60:
        token = support.user_token('lecteur-ws', support.VALUES['WS_LECTEUR_PASSWORD'])
        tokens.append(token)
        temp = PRIVATE / 'reader-token.tmp'
        temp.write_text(token, encoding='utf-8')
        temp.replace(PRIVATE / 'reader-token.txt')
        last_refresh = time.monotonic()


def safe(value):
    data = json.dumps(value, ensure_ascii=False)
    private_values = tokens + [(PRIVATE / 'api-key.txt').read_text().strip()] + list(support.VALUES.values())
    for secret in private_values:
        if len(secret) > 15:
            data = data.replace(secret, '[SECRET_RETIRE]')
    data = re.sub(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+', '[JETON_RETIRE]', data)
    return json.loads(data)


def save(name, data):
    (OUT / name).write_text(json.dumps(safe(data), ensure_ascii=False, indent=2), encoding='utf-8')


def resolve_fixtures():
    token = (PRIVATE / 'reader-token.txt').read_text().strip()
    fixtures = {}
    for service in SERVICES:
        path = f'/{service}?limit=1&offset=0' if service == 'chargements' else f'/{service}?page=0&size=1'
        status, data = support.api(service, path, token=token)
        assert status == 200, (service, status)
        rows = data if service == 'chargements' else data['items' if service == 'camions' else 'content']
        if not rows:
            raise RuntimeError('Seed the isolated integration dataset before scanning: ' + service)
        fixtures[service] = rows[0]['id']
    return fixtures


def send(url, expected=200):
    host = urlparse(url).netloc
    if host not in {f'{s}:{p}' for s, p in SERVICES.items()} | {'ws-dat-frontend:8080'}:
        raise RuntimeError('Target outside the isolated allowlist.')
    refresh_token()
    raw = f'GET {url} HTTP/1.1\r\nHost: {host}\r\nAccept: */*\r\nConnection: close\r\n\r\n'
    messages = call('core', 'action', 'sendRequest', request=raw, followRedirects='false')['sendRequest']
    message = messages[-1]
    status = int(message['responseHeader'].splitlines()[0].split()[1])
    if status != expected:
        raise RuntimeError(f'Preflight {url}: HTTP {status}, expected {expected}.')
    return {'url': url, 'methode': 'GET', 'HTTP': status, 'messageId': message.get('id')}


def run():
    OUT.mkdir(parents=True, exist_ok=True)
    call('core', 'action', 'newSession', name='/zap/wrk/session-dat', overwrite='true')
    context_id = call('context', 'action', 'newContext', contextName=CONTEXT)['contextId']
    for regex in (r'^http://(?:camions:8081|chargements:8082|bons:8083|transports:8084)/api/v1/.*',
                  r'^http://ws-dat-frontend:8080/.*'):
        call('context', 'action', 'includeInContext', contextName=CONTEXT, regex=regex)
    call('context', 'action', 'setContextInScope', contextName=CONTEXT, booleanInScope='true')
    call('pscan', 'action', 'setScanOnlyInScope', onlyInScope='true')
    refresh_token(force=True)
    fixtures = resolve_fixtures()
    TARGETS['transports'] += '&camionId=' + quote(fixtures['camions'])
    shutil.copy2(ROOT / 'quality/security/authorization.js', PRIVATE / 'authorization.js')
    existing = call('script', 'view', 'listScripts')['listScripts']
    if any(s.get('name') == 'DAT-local-auth' for s in existing):
        call('script', 'action', 'remove', scriptName='DAT-local-auth')
    call('script', 'action', 'load', scriptName='DAT-local-auth', scriptType='httpsender',
         scriptEngine='ECMAScript : Graal.js', fileName='/zap/wrk/authorization.js',
         scriptDescription='JWT LECTEUR, exclusivement sur les quatre API internes')
    call('script', 'action', 'enable', scriptName='DAT-local-auth')
    if POLICY in call('ascan', 'view', 'scanPolicyNames')['scanPolicyNames']:
        call('ascan', 'action', 'removeScanPolicy', scanPolicyName=POLICY)
    call('ascan', 'action', 'addScanPolicy', scanPolicyName=POLICY)
    call('ascan', 'action', 'disableAllScanners', scanPolicyName=POLICY)
    call('ascan', 'action', 'enableScanners', ids='6,40012,40018', scanPolicyName=POLICY)
    for scanner in ('6', '40012', '40018'):
        call('ascan', 'action', 'setScannerAttackStrength', id=scanner, attackStrength='LOW', scanPolicyName=POLICY)
        call('ascan', 'action', 'setScannerAlertThreshold', id=scanner, alertThreshold='MEDIUM', scanPolicyName=POLICY)
    for operation, value in [('setOptionThreadPerHost', 2), ('setOptionDelayInMs', 40),
                             ('setOptionMaxRuleDurationInMins', 3), ('setOptionMaxScanDurationInMins', 6),
                             ('setOptionTargetParamsInjectable', 1), ('setOptionTargetParamsEnabledRPC', 0)]:
        call('ascan', 'action', operation, Integer=value)
    rules = [s for s in call('ascan', 'view', 'scanners', scanPolicyName=POLICY)['scanners'] if s['enabled'] == 'true']
    assert {s['id'] for s in rules} == {'6', '40012', '40018'}
    protocol = {'dateDebutUTC': datetime.now(timezone.utc).isoformat(), 'ciblesActives': TARGETS,
                'reglesActives': rules, 'intensite': 'LOW', 'seuilAlerte': 'MEDIUM',
                'vecteurs': 'Paramètres de requête uniquement ; pas de corps, cookie, en-tête ou segment de chemin injecté.',
                'methode': 'GET', 'threadsParHote': 2, 'pauseMs': 40,
                'maximumMinutesParRegle': 3, 'maximumMinutesParCible': 6,
                'authentification': 'JWT LECTEUR réel, renouvelé au moins toutes les 60 secondes ; script limité aux quatre hôtes et ports API.',
                'contexteId': context_id, 'frontend': 'Bundle construit, servi par Nginx de test sans port hôte publié.',
                'limites': 'Analyse ciblée, pas un pentest exhaustif. Pas de scan de routes d’écriture, pas de spider AJAX ni exécution des payloads dans un navigateur. Authentification Keycloak vérifiée séparément, non attaquée par ZAP. HTTP local, TLS de production non vérifié.'}
    save('protocole-zap.json', protocol)
    probes = []
    for url in TARGETS.values():
        probes.append(send(url))
    for service, item_id in fixtures.items():
        probes.append(send(f'http://{service}:{SERVICES[service]}/api/v1/{service}/{quote(item_id)}'))
    for route in ('/', '/chargements', '/camions', '/bons', '/transports'):
        probes.append(send(FRONT + route))
    for asset in sorted((ROOT / 'frontend/dist/assets').iterdir()):
        if asset.is_file():
            probes.append(send(FRONT + '/assets/' + quote(asset.name)))
    save('requetes-initiales.json', probes)
    runs = []
    for service, url in TARGETS.items():
        refresh_token(force=True)
        print(f'Active scan: {service}, GET only, three selected rules...', flush=True)
        start = time.monotonic()
        scan_id = call('ascan', 'action', 'scan', url=url, recurse='false', inScopeOnly='true',
                       scanPolicyName=POLICY, method='GET')['scan']
        last_print = -1
        while True:
            refresh_token()
            progress = int(call('ascan', 'view', 'status', scanId=scan_id)['status'])
            if progress // 20 != last_print:
                print(f'  {service}: {progress} %', flush=True)
                last_print = progress // 20
            if progress == 100:
                break
            if time.monotonic() - start > 420:
                call('ascan', 'action', 'stop', scanId=scan_id)
                raise RuntimeError('Active scan exceeded its bounded execution window.')
            time.sleep(2)
        duration = round(time.monotonic() - start, 2)
        postflight = send(url)
        runs.append({'service': service, 'url': url, 'scanId': scan_id, 'progression': progress,
                     'dureeSecondes': duration, 'etatRegles': call('ascan', 'view', 'scanProgress', scanId=scan_id),
                     'lectureApresScan': postflight})
        save('scans-actifs.json', runs)
        print(f'  Finished {service}: {duration} s; authenticated postflight HTTP 200.', flush=True)
    deadline = time.monotonic() + 180
    while int(call('pscan', 'view', 'recordsToScan')['recordsToScan']):
        if time.monotonic() > deadline:
            raise RuntimeError('Passive scan queue did not drain.')
        time.sleep(1)
    save('alertes-zap.json', call('core', 'view', 'alerts', start=0, count=10000))
    save('statistiques-zap.json', {'messages': call('core', 'view', 'numberOfMessages'),
                                 'sites': call('core', 'view', 'sites'),
                                 'scans': call('ascan', 'view', 'scans'),
                                 'filePassiveRestante': call('pscan', 'view', 'recordsToScan'),
                                 'scripts': call('script', 'view', 'listScripts')})
    (OUT / 'ZAP').mkdir(exist_ok=True)
    for template, extension in [('traditional-html', 'html'), ('traditional-json', 'json')]:
        result = call('reports', 'action', 'generate', title='DAT Transport — Analyse locale OWASP ZAP',
                      template=template, description='Quatre API authentifiées et bundle frontend sur serveur statique de test. Analyse ciblée, non exhaustive.',
                      contexts=CONTEXT, reportDir='/zap/wrk/reports', reportFileName='rapport-zap.' + extension,
                      display='false')
        source = PRIVATE / 'reports' / ('rapport-zap.' + extension)
        assert source.exists(), result
        raw = source.read_text(encoding='utf-8')
        # Redact both exact secrets and any bearer JWT in the shareable export.
        for secret in tokens + [(PRIVATE / 'api-key.txt').read_text().strip()] + list(support.VALUES.values()):
            if len(secret) > 15:
                raw = raw.replace(secret, '[SECRET_RETIRE]')
        raw = re.sub(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+', '[JETON_RETIRE]', raw)
        (OUT / 'ZAP' / source.name).write_text(raw, encoding='utf-8')
    save('execution-zap.json', {'terminee': True, 'dateFinUTC': datetime.now(timezone.utc).isoformat(),
                              'sondesInitiales': len(probes), 'scansActifs': len(runs),
                              'renouvellementsJWT': len(tokens), 'neonUtilise': False,
                              'reglesActivesToutesDisponibles': True})
    print('ZAP campaign completed. Reports exported with tokens removed.', flush=True)


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    run()
