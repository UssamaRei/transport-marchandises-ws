"""Verify live access, JWT rejection, CORS and headers on the isolated APIs."""
import base64
from datetime import datetime, timezone
import json
import sys
from urllib.error import HTTPError
from urllib.parse import urlencode
from urllib.request import Request, urlopen
from zap_runtime import ROOT, OUT

sys.path.insert(0, str(ROOT / 'tests/integration'))
import test_system as support


def http(url, method='GET', token=None, headers=None, data=None):
    values = {'Accept': 'application/json', **(headers or {})}
    if token:
        values['Authorization'] = 'Bearer ' + token
    request = Request(url, data=data, headers=values, method=method)
    try:
        response = urlopen(request, timeout=25)
    except HTTPError as error:
        response = error
    with response:
        return response.status, dict(response.headers), response.read()


def run():
    OUT.mkdir(parents=True, exist_ok=True)
    reader = support.user_token('lecteur-ws', support.VALUES['WS_LECTEUR_PASSWORD'])
    agent = support.user_token('agent-ws', support.VALUES['WS_AGENT_PASSWORD'])
    parts = reader.split('.')
    tampered = parts[0] + '.' + parts[1][:-2] + 'xx.' + parts[2]
    unsigned = base64.urlsafe_b64encode(b'{"alg":"none","typ":"JWT"}').decode().rstrip('=') + '.' + parts[1] + '.'
    results = []

    def record(service, name, status, expected, condition=True, details=None):
        row = {'service': service, 'controle': name, 'HTTP': status, 'HTTPAttendu': expected,
               'reussi': status in expected and bool(condition), 'detail': details}
        results.append(row)
        print(f'{service}: {name} — HTTP {status} — ' + ('OK' if row['reussi'] else 'ECHEC'), flush=True)

    for service, port in support.PORTS.items():
        url = f'http://127.0.0.1:{port}/api/v1/{service}'
        for name, token, expected in [('Sans JWT', None, [401]), ('LECTEUR en lecture', reader, [200]),
                                     ('AGENT en lecture', agent, [200]), ('JWT altéré', tampered, [401]),
                                     ('JWT non signé alg=none', unsigned, [401])]:
            status, headers, body = http(url, token=token)
            record(service, name, status, expected)
        status, headers, body = http(url, 'POST', reader, {'Content-Type': 'application/json'}, b'{}')
        record(service, 'Écriture LECTEUR refusée', status, [403])
        status, headers, body = http(url, token=reader)
        selected = {k: v for k, v in headers.items() if k.lower() in
                    ('cache-control', 'x-content-type-options', 'x-frame-options', 'content-type')}
        lowered = {k.lower(): v for k, v in headers.items()}
        record(service, 'Protection nosniff', status, [200], lowered.get('x-content-type-options') == 'nosniff', selected)
        record(service, 'Protection anti-iframe', status, [200], lowered.get('x-frame-options', '').upper() == 'DENY')
        record(service, 'Réponses métier non stockées en cache', status, [200], 'no-store' in lowered.get('cache-control', ''))
        for origin, allowed in [('http://localhost:5173', True), ('https://example.invalid', False)]:
            status, headers, body = http(url, 'OPTIONS', headers={
                'Origin': origin, 'Access-Control-Request-Method': 'POST',
                'Access-Control-Request-Headers': 'authorization,content-type'})
            acao = next((v for k, v in headers.items() if k.lower() == 'access-control-allow-origin'), None)
            record(service, 'CORS origine autorisée' if allowed else 'CORS origine étrangère refusée',
                   status, [200] if allowed else [403], acao == origin if allowed else acao is None,
                   {'origin': origin, 'accessControlAllowOrigin': acao})
        status, headers, body = http(url, 'TRACE', reader)
        record(service, 'Méthode TRACE refusée', status, [400, 403, 405, 501],
               reader.encode() not in body,
               {'jetonNonReflete': reader.encode() not in body,
                'interpretation': 'Requête rejetée par le serveur ou sa chaîne de sécurité, sans écho du jeton.'})

    technical = support.token_request({'grant_type': 'client_credentials', 'client_id': 'bons-service',
                                       'client_secret': support.VALUES['BONS_CLIENT_SECRET']})
    for service, port in support.PORTS.items():
        status, headers, body = http(f'http://127.0.0.1:{port}/api/v1/{service}', token=technical)
        record(service, 'Compte technique Bons limité à sa dépendance', status,
               [200] if service == 'chargements' else [401, 403])
    status, headers, body = http(support.ISSUER + '/protocol/openid-connect/token', 'POST', headers={
        'Content-Type': 'application/x-www-form-urlencoded'}, data=urlencode({
        'client_id': 'transport-web', 'grant_type': 'password', 'username': 'lecteur-ws',
        'password': support.VALUES['WS_LECTEUR_PASSWORD']}).encode())
    error = json.loads(body)
    record('Keycloak', 'Grant password désactivé pour le client web PKCE', status, [400, 401],
           error.get('error') in ('unauthorized_client', 'unsupported_grant_type'), {'error': error.get('error')})
    report = {'dateUTC': datetime.now(timezone.utc).isoformat(), 'controles': results,
              'total': len(results), 'reussis': sum(r['reussi'] for r in results),
              'base': 'PostgreSQL local isolé ; aucun accès Neon',
              'limites': 'Ces contrôles portent sur les cas explicitement testés, pas sur toutes les attaques possibles.'}
    (OUT / 'controles-acces.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    assert report['reussis'] == report['total'], 'Some live security checks failed; inspect controles-acces.json.'


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    run()
