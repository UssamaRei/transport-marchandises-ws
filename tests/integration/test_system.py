"""API-only integration tests. Requires run.py's isolated stack; never targets Neon."""
import base64
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
import hashlib
from html.parser import HTMLParser
from http.cookiejar import CookieJar
import json
import secrets
import subprocess
import time
import unittest
from urllib.error import HTTPError
from urllib.parse import parse_qs, urlencode, urlparse
from urllib.request import (Request, urlopen, build_opener, HTTPRedirectHandler,
                            HTTPCookieProcessor)
import uuid

from run import ROOT, PRIVATE, OUTPUT, compose, wait_url

ISSUER = 'http://localhost:18090/realms/transport-ws'
PORTS = {'camions': 18081, 'chargements': 18082, 'bons': 18083, 'transports': 18084}
VALUES = dict(line.split('=', 1) for line in PRIVATE.read_text().splitlines() if '=' in line)


def api(service, path, method='GET', body=None, token=None):
    headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    request = Request(f'http://127.0.0.1:{PORTS[service]}/api/v1{path}',
                      data=json.dumps(body).encode() if body is not None else None,
                      method=method, headers=headers)
    try:
        with urlopen(request, timeout=25) as response:
            status, raw = response.status, response.read()
    except HTTPError as error:
        status, raw = error.code, error.read()
    try:
        data = json.loads(raw) if raw else None
    except ValueError:
        data = {'nonJsonResponse': raw.decode(errors='replace')[:200]}
    return status, data


class LoginForm(HTMLParser):
    def __init__(self):
        super().__init__()
        self.action, self.fields = None, {}

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == 'form' and attrs.get('id') == 'kc-form-login':
            self.action = attrs['action']
        if tag == 'input' and attrs.get('type') == 'hidden' and attrs.get('name'):
            self.fields[attrs['name']] = attrs.get('value', '')


class LoginRedirect(HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, msg, headers, newurl):
        if newurl.startswith('http://localhost:5173'):
            return None
        return super().redirect_request(request, fp, code, msg, headers, newurl)


def user_token(username, password):
    """Real Keycloak Authorization Code + PKCE, entirely over HTTP, no frontend."""
    verifier = secrets.token_urlsafe(48)
    challenge = base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest()).decode().rstrip('=')
    state = secrets.token_urlsafe(24)
    query = urlencode({'client_id': 'transport-web', 'redirect_uri': 'http://localhost:5173',
                       'response_type': 'code', 'scope': 'openid', 'state': state,
                       'code_challenge': challenge, 'code_challenge_method': 'S256'})
    cookies = CookieJar()
    opener = build_opener(HTTPCookieProcessor(cookies), LoginRedirect())
    with opener.open(ISSUER + '/protocol/openid-connect/auth?' + query, timeout=20) as response:
        form = LoginForm()
        form.feed(response.read().decode())
    # Browsers accept Secure cookies on trusted localhost HTTP; urllib does not.
    # This adjustment applies only to this disposable local Keycloak login.
    for cookie in cookies:
        cookie.secure = False
    if not form.action:
        raise RuntimeError('Keycloak login form missing')
    form.fields.update(username=username, password=password, credentialId='')
    try:
        opener.open(Request(form.action, data=urlencode(form.fields).encode()), timeout=20)
    except HTTPError as error:
        if error.code != 302:
            page = error.read().decode(errors='replace')
            import re
            message = re.search(r'<span[^>]*id="input-error"[^>]*>(.*?)</span>|<p[^>]*class="instruction"[^>]*>(.*?)</p>', page, re.S)
            detail = re.sub('<[^>]*>', '', next((item for item in message.groups() if item), '')) if message else 'no detail'
            raise RuntimeError(f'Keycloak login failed: {error.code}; {detail.strip()}') from None
        callback = parse_qs(urlparse(error.headers['Location']).query)
    else:
        raise RuntimeError('Keycloak did not return an authorization code')
    if callback.get('state') != [state] or 'code' not in callback:
        raise RuntimeError('Invalid OAuth callback')
    return token_request({'grant_type': 'authorization_code', 'client_id': 'transport-web',
                          'redirect_uri': 'http://localhost:5173', 'code': callback['code'][0],
                          'code_verifier': verifier})


def token_request(form):
    with urlopen(Request(ISSUER + '/protocol/openid-connect/token', data=urlencode(form).encode()),
                 timeout=20) as response:
        return json.load(response)['access_token']


def iso(hours=0):
    return (datetime.now(timezone.utc) + timedelta(hours=hours)).isoformat()


class SystemTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tokens = {}
        cls.token_time = 0
        cls.refresh_tokens()

    @classmethod
    def refresh_tokens(cls):
        if time.monotonic() - cls.token_time < 180:
            return
        for role in ['AGENT', 'ADMIN', 'LECTEUR']:
            cls.tokens[role] = user_token(role.lower() + '-ws', VALUES[f'WS_{role}_PASSWORD'])
        for service in PORTS:
            cls.tokens[service] = token_request({'grant_type': 'client_credentials',
                'client_id': service + '-service', 'client_secret': VALUES[service.upper() + '_CLIENT_SECRET']})
        cls.token_time = time.monotonic()

    def setUp(self):
        self.refresh_tokens()

    def call(self, service, path, method='GET', body=None, expected=200, role='AGENT'):
        status, data = api(service, path, method, body, self.tokens.get(role))
        self.assertEqual(status, expected, f'{service}: {method} {path}: {status} {data}')
        return data

    def truck(self, capacity=100):
        return self.call('camions', '/camions', 'POST',
                         {'immatriculation': 'TEST-' + uuid.uuid4().hex[:12], 'capaciteKg': capacity}, 201)

    def draft(self):
        return self.call('chargements', '/chargements', 'POST',
            {'origine': 'Agadir', 'destination': 'Casablanca', 'destinataireNom': 'Client integration',
             'destinataireTelephone': '0600000000'}, 201)

    def loading(self, truck, weight=100):
        draft = self.draft()
        self.call('chargements', f"/chargements/{draft['id']}/lignes", 'POST',
                  {'designation': 'Cartons', 'quantite': 10, 'poidsUnitaireKg': weight / 10}, 201)
        return self.call('chargements', f"/chargements/{draft['id']}/validation", 'POST',
                         {'camionId': truck['id']})

    def bon(self, loading):
        return self.call('bons', '/bons', 'POST', {'chargementId': loading['id']}, 201)

    def plan_request(self, bon, truck):
        return {'bonId': bon['id'], 'camionId': truck['id'],
                'departPrevu': iso(), 'operationKey': 'test:' + uuid.uuid4().hex}

    def journey(self):
        truck = self.truck()
        loading = self.loading(truck)
        bon = self.bon(loading)
        request = self.plan_request(bon, truck)
        transport = self.call('transports', '/transports', 'POST', request)
        self.assertEqual(transport['statut'], 'PLANIFIE')
        return truck, loading, bon, transport, request

    def arrive(self, transport):
        path = '/transports/' + transport['id']
        self.call('transports', path + '/depart', 'POST', {'departReel': iso()})
        self.call('transports', path + '/arrivee', 'POST', {'arriveeReelle': iso(1)})

    def proof(self):
        return {'receptionnaire': 'Destinataire Test',
                'dateReception': datetime.now(timezone.utc).date().isoformat(), 'commentaire': 'Recu complet'}

    def restart(self, service):
        compose('start', service)
        wait_url(f'http://127.0.0.1:{PORTS[service]}/api/v1/not-a-route', (401, 403, 404))

    def test_00_authentication_and_permissions(self):
        for service, path in [('camions', '/camions'), ('chargements', '/chargements'),
                              ('bons', '/bons/missing'), ('transports', '/transports')]:
            with self.subTest(service=service):
                self.call(service, path, role=None, expected=401)
                status, _ = api(service, path, token='invalid.jwt.token')
                self.assertEqual(status, 401)
                parts = self.tokens['AGENT'].split('.')
                parts[2] = ('A' if parts[2][0] != 'A' else 'B') + parts[2][1:]
                self.assertEqual(api(service, path, token='.'.join(parts))[0], 401)
        truck = self.truck()
        for service, path, body in [('camions', '/camions', {'immatriculation': 'DENIED', 'capaciteKg': 100}),
                                    ('chargements', '/chargements', {}), ('bons', '/bons', {'chargementId': 'missing'}),
                                    ('transports', '/transports', {})]:
            with self.subTest(reader_write=service):
                self.call(service, path, 'POST', body, 403, 'LECTEUR')
        self.call('camions', '/camions/' + truck['id'], role='LECTEUR')
        self.call('camions', '/camions/' + truck['id'], role='chargements')
        self.call('camions', '/reservations?transportId=missing', expected=403, role='chargements')
        self.call('camions', '/camions', 'POST', {}, 403, 'transports')
        self.call('bons', '/bons/missing', expected=404, role='transports')
        self.call('bons', '/bons', 'POST', {}, 403, 'transports')
        self.call('bons', '/bons/missing', expected=401, role='bons')
        self.call('chargements', '/chargements', 'POST', {}, 403, 'bons')
        self.call('transports', '/transports?camionId=' + truck['id'] + '&actifs=true', role='camions')
        self.call('transports', '/transports', expected=403, role='camions')

    def test_01_full_delivery(self):
        truck, loading, bon, transport, request = self.journey()
        self.assertEqual(loading['poidsTotalKg'], 100)
        self.assertEqual(bon['poidsTotalKg'], 100)
        self.assertEqual(bon['lignes'][0]['designation'], 'Cartons')
        self.assertEqual(bon['statut'], 'EMIS')
        self.assertIsNotNone(bon['createdAt'], 'Bons must return its persisted creation timestamp')
        self.assertEqual(self.call('bons', '/bons/' + bon['id'])['createdAt'], bon['createdAt'])
        self.assertFalse(self.call('camions', '/camions/' + truck['id'])['disponible'])
        path = '/transports/' + transport['id']
        self.call('transports', path + '/depart', 'POST', {'departReel': iso()})
        self.call('transports', path + '/evenements', 'POST', {'type': 'POINTAGE', 'description': 'En route',
            'dateEvenement': iso(0.5), 'latitude': 30.42, 'longitude': -9.60}, 201)
        arrival = iso(1)
        self.call('transports', path + '/arrivee', 'POST', {'arriveeReelle': arrival})
        self.call('transports', path + '/arrivee', 'POST', {'arriveeReelle': arrival})
        proof = self.proof()
        delivered = self.call('transports', path + '/livraison', 'POST', proof)
        self.assertEqual(delivered['statut'], 'LIVRE')
        self.assertTrue(delivered['preuveConfirmee'])
        self.assertTrue(delivered['liberationConfirmee'])
        self.assertEqual(self.call('transports', path + '/livraison', 'POST', proof), delivered)
        final_bon = self.call('bons', '/bons/' + bon['id'])
        self.assertEqual(final_bon['statut'], 'LIVRE')
        self.assertEqual(final_bon['transportId'], transport['id'])
        self.assertEqual(final_bon['dateReception'], proof['dateReception'])
        self.assertEqual(final_bon['lignes'], bon['lignes'])
        reservation = self.call('camions', '/reservations/' + transport['reservationId'])
        self.assertEqual(reservation['statut'], 'LIBEREE')
        self.assertEqual(reservation['motifLiberation'], 'LIVRAISON')
        self.assertTrue(self.call('camions', '/camions/' + truck['id'])['disponible'])
        events = self.call('transports', path + '/evenements')['content']
        self.assertCountEqual([event['type'] for event in events], ['DEPART', 'POINTAGE', 'ARRIVEE'])
        self.assertEqual(self.call('transports', '/transports?camionId=' + truck['id'] + '&actifs=true')['totalElements'], 0)
        self.call('transports', path + '/livraison', 'POST', {**proof, 'commentaire': 'Different'}, 409)
        self.call('chargements', '/chargements/' + loading['id'], 'PUT', {'origine': 'Modifie'}, 409)

    def test_02_cancel_and_replan(self):
        truck, _, bon, transport, request = self.journey()
        path = '/transports/' + transport['id'] + '/annulation'
        cancelled = self.call('transports', path, 'POST')
        self.assertEqual(cancelled['statut'], 'ANNULE')
        self.assertEqual(self.call('transports', path, 'POST'), cancelled)
        self.assertTrue(self.call('camions', '/camions/' + truck['id'])['disponible'])
        self.assertEqual(self.call('bons', '/bons/' + bon['id'])['statut'], 'EMIS')
        new_plan = self.call('transports', '/transports', 'POST', self.plan_request(bon, truck))
        self.assertNotEqual(new_plan['id'], transport['id'])
        self.assertEqual(new_plan['statut'], 'PLANIFIE')

    def test_03_capacity_drafts_and_missing_sources(self):
        truck = self.truck(99.999)
        draft = self.draft()
        path = '/chargements/' + draft['id']
        self.call('chargements', path + '/validation', 'POST', {'camionId': truck['id']}, 400)
        self.call('bons', '/bons', 'POST', {'chargementId': draft['id']}, 409)
        self.call('chargements', path + '/lignes', 'POST',
            {'designation': 'Cartons', 'quantite': 10, 'poidsUnitaireKg': 10}, 201)
        self.call('chargements', path + '/validation', 'POST', {'camionId': truck['id']}, 409)
        self.assertEqual(self.call('chargements', path)['statut'], 'BROUILLON')
        self.call('bons', '/bons', 'POST', {'chargementId': 'absent'}, 404)
        self.call('transports', '/transports', 'POST', self.plan_request({'id': 'absent'}, truck), 404)

    def test_04_plan_replays_and_conflicts(self):
        truck, loading, bon, transport, request = self.journey()
        replay = self.call('transports', '/transports', 'POST', request)
        self.assertEqual(replay['id'], transport['id'])
        self.assertEqual(replay['reservationId'], transport['reservationId'])
        self.assertEqual(replay['createdAt'], transport['createdAt'])
        self.call('transports', '/transports', 'POST', {**request, 'departPrevu': iso(2)}, 409)
        self.call('transports', '/transports', 'POST', self.plan_request(bon, truck), 409)
        self.call('bons', '/bons', 'POST', {'chargementId': loading['id']}, 409)

    def test_05_invalid_state_transitions(self):
        _, _, _, transport, _ = self.journey()
        path = '/transports/' + transport['id']
        self.call('transports', path + '/arrivee', 'POST', {'arriveeReelle': iso()}, 409)
        self.call('transports', path + '/livraison', 'POST', self.proof(), 409)
        self.call('transports', path + '/depart', 'POST', {'departReel': iso()})
        self.call('transports', path + '/annulation', 'POST', expected=409)
        self.call('transports', path + '/depart', 'POST', {'departReel': iso()}, 409)
        self.call('transports', path + '/arrivee', 'POST', {'arriveeReelle': iso(-1)}, 400)
        self.call('transports', path + '/evenements', 'POST',
            {'type': 'POINTAGE', 'description': 'Invalide', 'dateEvenement': iso(1), 'latitude': 91}, 400)

    def test_06_two_transports_compete_for_one_truck(self):
        truck = self.truck()
        bons = [self.bon(self.loading(truck)) for _ in range(2)]
        requests = [self.plan_request(bon, truck) for bon in bons]
        with ThreadPoolExecutor(2) as pool:
            results = list(pool.map(lambda body: api('transports', '/transports', 'POST', body, self.tokens['AGENT']), requests))
        self.assertCountEqual([status for status, _ in results], [200, 409])
        winner = next(data for status, data in results if status == 200)
        self.assertEqual(winner['statut'], 'PLANIFIE')
        self.assertFalse(self.call('camions', '/camions/' + truck['id'])['disponible'])

    def test_07_concurrent_bon_creation(self):
        loading = self.loading(self.truck())
        with ThreadPoolExecutor(2) as pool:
            results = list(pool.map(lambda _: api('bons', '/bons', 'POST',
                {'chargementId': loading['id']}, self.tokens['AGENT']), range(2)))
        self.assertCountEqual([status for status, _ in results], [201, 409])

    def test_08_concurrent_delivery_replays(self):
        truck, _, bon, transport, _ = self.journey()
        self.arrive(transport)
        with ThreadPoolExecutor(2) as pool:
            results = list(pool.map(lambda _: api('transports', '/transports/' + transport['id'] + '/livraison',
                'POST', self.proof(), self.tokens['AGENT']), range(2)))
        self.assertEqual([status for status, _ in results], [200, 200], str(results))
        self.assertEqual(results[0][1], results[1][1])
        self.assertTrue(self.call('camions', '/camions/' + truck['id'])['disponible'])
        self.assertEqual(self.call('bons', '/bons/' + bon['id'])['transportId'], transport['id'])

    def test_09_retry_when_bons_is_unavailable(self):
        truck, _, bon, transport, _ = self.journey()
        self.arrive(transport)
        path = '/transports/' + transport['id']
        proof = self.proof()
        try:
            compose('stop', '-t', '1', 'bons')
            self.call('transports', path + '/livraison', 'POST', proof, 503)
            pending = self.call('transports', path)
            self.assertEqual(pending['statut'], 'CLOTURE_EN_ATTENTE')
            self.assertFalse(pending['preuveConfirmee'])
            self.assertFalse(self.call('camions', '/camions/' + truck['id'])['disponible'])
        finally:
            self.restart('bons')
        self.call('transports', path + '/livraison', 'POST', {**proof, 'receptionnaire': 'Different'}, 409)
        self.assertEqual(self.call('transports', path + '/livraison', 'POST', proof)['statut'], 'LIVRE')

    def test_10_retry_release_after_proof_succeeds(self):
        truck, _, bon, transport, _ = self.journey()
        self.arrive(transport)
        path = '/transports/' + transport['id']
        proof = self.proof()
        try:
            compose('stop', '-t', '1', 'camions')
            self.call('transports', path + '/livraison', 'POST', proof, 503)
            pending = self.call('transports', path)
            self.assertEqual(pending['statut'], 'CLOTURE_EN_ATTENTE')
            self.assertTrue(pending['preuveConfirmee'])
            self.assertFalse(pending['liberationConfirmee'])
            self.assertEqual(self.call('bons', '/bons/' + bon['id'])['statut'], 'LIVRE')
        finally:
            self.restart('camions')
        self.assertEqual(self.call('transports', path + '/livraison', 'POST', proof)['statut'], 'LIVRE')
        self.assertTrue(self.call('camions', '/camions/' + truck['id'])['disponible'])

    def test_11_retry_plan_after_camions_returns(self):
        truck = self.truck()
        bon = self.bon(self.loading(truck))
        request = self.plan_request(bon, truck)
        try:
            compose('stop', '-t', '1', 'camions')
            self.call('transports', '/transports', 'POST', request, 503)
            rows = self.call('transports', '/transports?camionId=' + truck['id'])['content']
            self.assertEqual(len(rows), 1)
            self.assertEqual(rows[0]['statut'], 'PREPARATION')
            self.call('transports', '/transports/' + rows[0]['id'] + '/depart', 'POST', {'departReel': iso()}, 409)
        finally:
            self.restart('camions')
        resumed = self.call('transports', '/transports', 'POST', request)
        self.assertEqual(resumed['id'], rows[0]['id'])
        self.assertEqual(resumed['statut'], 'PLANIFIE')

    def test_12_proof_for_wrong_transport_is_atomic(self):
        _, _, bon, transport, _ = self.journey()
        _, _, _, other, _ = self.journey()
        proof = {**self.proof(), 'transportId': other['id'], 'operationKey': 'livraison:' + other['id']}
        path = '/bons/' + bon['id'] + '/preuve-livraison'
        self.call('bons', path, 'POST', proof, 409, 'transports')
        unchanged = self.call('bons', '/bons/' + bon['id'])
        self.assertEqual(unchanged['statut'], 'EMIS')
        self.assertIsNone(unchanged['transportId'])

    def test_13_nanosecond_replay(self):
        truck = self.truck()
        bon = self.bon(self.loading(truck))
        request = {**self.plan_request(bon, truck), 'departPrevu': '2026-11-19T09:00:00.123456789Z'}
        first = self.call('transports', '/transports', 'POST', request)
        again = self.call('transports', '/transports', 'POST', request)
        self.assertEqual(first, again)
        path = '/transports/' + first['id']
        self.call('transports', path + '/depart', 'POST', {'departReel': '2026-11-19T10:00:00.123456789Z'})
        arrival = {'arriveeReelle': '2026-11-19T11:00:00.123456789Z'}
        first_arrival = self.call('transports', path + '/arrivee', 'POST', arrival)
        again_arrival = self.call('transports', path + '/arrivee', 'POST', arrival)
        self.assertEqual(first_arrival, again_arrival)

    def test_14_database_accounts_are_limited(self):
        for role, own, foreign in [('camions_app', 'camion', 'bon_livraison'),
                ('chargements_app', 'chargement', 'camion'), ('bons_app', 'bon_livraison', 'chargement'),
                ('transports_app', 'transport', 'bon_livraison')]:
            sql = f"SELECT has_table_privilege('{role}','{own}','INSERT') AND NOT has_table_privilege('{role}','{foreign}','SELECT');"
            result = compose('exec', '-T', 'db', 'psql', '-U', 'postgres', '-d', 'transport_test', '-Atc', sql)
            self.assertEqual(result.stdout.strip(), 't', role)

    def test_15_database_invariants(self):
        sql = """SELECT count(*) FROM (
          SELECT t.id FROM transport t LEFT JOIN reservation_camion r ON r.id=t.reservation_id
          LEFT JOIN bon_livraison b ON b.id=t.bon_id
          WHERE t.statut='LIVRE' AND (NOT t.preuve_confirmee OR NOT t.liberation_confirmee
             OR r.statut IS DISTINCT FROM 'LIBEREE' OR b.statut IS DISTINCT FROM 'LIVRE'
             OR b.transport_preuve_id IS DISTINCT FROM t.id)
          UNION ALL SELECT camion_id FROM reservation_camion WHERE statut='ACTIVE'
             GROUP BY camion_id HAVING count(*)>1
          UNION ALL SELECT chargement_id FROM bon_livraison GROUP BY chargement_id HAVING count(*)>1
        ) anomalies;"""
        result = compose('exec', '-T', 'db', 'psql', '-U', 'postgres', '-d', 'transport_test', '-Atc', sql)
        self.assertEqual(result.stdout.strip(), '0')

    def test_16_bons_list_pagination_and_filter(self):
        bon = self.bon(self.loading(self.truck()))
        page = self.call('bons', '/bons?page=0&size=1', role='LECTEUR')
        self.assertEqual(len(page['content']), 1)
        self.assertEqual(page['content'][0]['id'], bon['id'])
        self.assertNotIn('lignes', page['content'][0])
        self.assertEqual(self.call('bons', '/bons?statut=EMIS&size=1')['content'][0]['statut'], 'EMIS')
        self.call('bons', '/bons?size=101', expected=400)
        self.call('bons', '/bons?statut=INVALIDE', expected=400)


if __name__ == '__main__':
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(SystemTest)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    report = {'passed': result.wasSuccessful(), 'tests': result.testsRun,
              'failures': [str(test) for test, _ in result.failures],
              'errors': [str(test) for test, _ in result.errors],
              'real_services': list(PORTS), 'database': 'one isolated PostgreSQL 18',
              'authentication': 'real Keycloak, user PKCE + service client_credentials',
              'frontend_used': False, 'business_api_mocks': False}
    (OUTPUT / 'resultats.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    raise SystemExit(0 if result.wasSuccessful() else 1)
