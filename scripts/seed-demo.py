"""Add a resumable fictional demonstration through the four local APIs; preserve existing data."""
import base64
from datetime import datetime, timedelta, timezone
import hashlib
from html.parser import HTMLParser
from http.cookiejar import CookieJar
import json
from pathlib import Path
import secrets
from urllib.error import HTTPError
from urllib.parse import parse_qs, urlencode, urlparse
from urllib.request import Request, urlopen, build_opener, HTTPRedirectHandler, HTTPCookieProcessor

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / '.tools/demo-data'
OUTPUT.mkdir(parents=True, exist_ok=True)
MANIFEST = OUTPUT / 'manifest.json'
AUTH = dict(line.split('=', 1) for line in (ROOT / '.secrets/auth.env').read_text(encoding='utf-8').splitlines() if '=' in line)
ISSUER = 'http://localhost:8090/realms/transport-ws'
PORTS = dict(camions=8081, chargements=8082, bons=8083, transports=8084)
state = json.loads(MANIFEST.read_text(encoding='utf-8')) if MANIFEST.exists() else {
    'batch': 'transit-demo-v1', 'createdAt': datetime.now(timezone.utc).isoformat(), 'trucks': {}, 'loadings': {}, 'bons': {}, 'transports': {}, 'events': []}


def checkpoint():
    temporary = MANIFEST.with_suffix('.tmp')
    temporary.write_text(json.dumps(state, ensure_ascii=False, indent=2), encoding='utf-8')
    temporary.replace(MANIFEST)


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


class LocalRedirect(HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, message, headers, newurl):
        if newurl.startswith('http://localhost:5173'):
            return None
        return super().redirect_request(request, fp, code, message, headers, newurl)


def login():
    verifier, nonce = secrets.token_urlsafe(48), secrets.token_urlsafe(24)
    challenge = base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest()).decode().rstrip('=')
    query = urlencode(dict(client_id='transport-web', redirect_uri='http://localhost:5173', response_type='code',
                           scope='openid', state=nonce, code_challenge=challenge, code_challenge_method='S256'))
    cookies = CookieJar()
    opener = build_opener(HTTPCookieProcessor(cookies), LocalRedirect())
    with opener.open(ISSUER + '/protocol/openid-connect/auth?' + query, timeout=30) as response:
        form = LoginForm()
        form.feed(response.read().decode())
    # urllib needs the same trusted-localhost behaviour as the browser for this dev login.
    for cookie in cookies:
        cookie.secure = False
    if not form.action:
        raise RuntimeError('Local Keycloak login form missing.')
    form.fields.update(username='agent-ws', password=AUTH['WS_AGENT_PASSWORD'], credentialId='')
    try:
        opener.open(Request(form.action, data=urlencode(form.fields).encode()), timeout=30)
    except HTTPError as error:
        if error.code != 302:
            raise RuntimeError('Local Keycloak authentication failed.') from None
        callback = parse_qs(urlparse(error.headers['Location']).query)
    else:
        raise RuntimeError('Keycloak did not return an authorization code.')
    if callback.get('state') != [nonce] or 'code' not in callback:
        raise RuntimeError('Unexpected OAuth callback.')
    with urlopen(Request(ISSUER + '/protocol/openid-connect/token', data=urlencode(dict(
            grant_type='authorization_code', client_id='transport-web', redirect_uri='http://localhost:5173',
            code=callback['code'][0], code_verifier=verifier)).encode()), timeout=30) as response:
        return json.load(response)['access_token']


token = login()


def api(service, path, method='GET', body=None):
    global token
    def send():
        return urlopen(Request(f'http://127.0.0.1:{PORTS[service]}/api/v1{path}', method=method,
            data=json.dumps(body).encode() if body is not None else None,
            headers={'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json'}), timeout=40)
    try:
        response = send()
    except HTTPError as error:
        if error.code == 401:
            token = login()
            response = send()
        else:
            raise RuntimeError(f'{service} {method} {path}: HTTP {error.code}: {error.read().decode()[:350]}') from None
    with response:
        data = response.read()
        return json.loads(data) if data else None


def all_rows(service):
    rows, page = [], 0
    while True:
        if service == 'chargements':
            batch = api(service, f'/{service}?limit=100&offset={page * 100}')
        else:
            result = api(service, f'/{service}?size=100&page={page}')
            batch = result.get('items', result.get('content', []))
        rows.extend(batch)
        if len(batch) < 100:
            return rows
        page += 1


if 'before' not in state:
    state['before'] = {service: all_rows(service) for service in PORTS}
    checkpoint()

capacities = [3500, 7500, 10000, 12000, 7500, 3500, 10000, 7500, 18000, 26000, 12000, 7500]
plates = ['90821-A-33', '90822-A-26', '90823-A-40', '90824-A-23', '90825-A-15', '90826-A-6',
          '90827-A-14', '90828-A-48', '90829-A-67', '90830-A-9', '90831-A-33', '90832-A-6']
for i, (plate, capacity) in enumerate(zip(plates, capacities)):
    key = str(i)
    if key not in state['trucks']:
        if any(row['immatriculation'] == plate for row in all_rows('camions')):
            raise RuntimeError(f'Existing registration {plate}: stopped to avoid modifying an unrelated record.')
        state['trucks'][key] = api('camions', '/camions', 'POST', dict(immatriculation=plate, capaciteKg=capacity))['id']
        checkpoint()
print('12 demonstration trucks ready.', flush=True)

# Origin, destination, fictional recipient, truck, target state, goods (quantity × unit kg).
scenarios = [
    ('Agadir', 'Casablanca', 'Atlas Distribution — dépôt de Casablanca', 0, 'LIVRE', [('Caisses de tomates', 80, 20), ('Cartons de poivrons', 30, 12)]),
    ('Marrakech', 'Rabat', 'Jardins du Sud — plateforme de Rabat', 1, 'LIVRE', [('Sacs de semoule', 100, 25), ('Cartons de dattes', 30, 10)]),
    ('Tanger', 'Fès', 'Tissage Atlas — entrepôt de Fès', 2, 'LIVRE', [('Ballots de tissu', 60, 30), ('Cartons de linge', 15, 40)]),
    ('Safi', 'Marrakech', 'Épiceries Al Manar — dépôt de Marrakech', 3, 'EN_COURS', [('Cartons de conserves de poisson', 90, 24), ('Palettes de bocaux', 12, 65)]),
    ('Fès', 'Casablanca', 'Maison Zellige — showroom de Casablanca', 4, 'EN_COURS', [('Caisses de céramique', 45, 45), ('Cartons de vaisselle', 20, 20)]),
    ('Casablanca', 'Agadir', 'Hygiène du Souss — centrale de distribution', 5, 'PLANIFIE', [('Cartons de produits d’hygiène', 120, 8), ('Cartons de consommables', 24, 15)]),
    ('Kénitra', 'Tanger', 'Pièces Nord — plateforme de Tanger', 6, 'PLANIFIE', [('Bacs de pièces automobiles', 110, 18), ('Caisses de composants', 12, 100)]),
    ('Oujda', 'Meknès', 'Verger du Saïss — dépôt de Meknès', 7, 'CLOTURE_EN_ATTENTE', [('Caisses d’agrumes', 75, 30), ('Caisses de pommes', 45, 20)]),
    ('Dakhla', 'Agadir', 'Atlantique Frais — agence d’Agadir', 8, 'ANNULE', [('Cartons de produits secs', 50, 15), ('Caisses isothermes vides', 24, 20)]),
    ('Tétouan', 'Rabat', 'Équipement Rif — dépôt de Rabat', 8, 'BON', [('Cartons de petit équipement', 130, 12)]),
    ('Béni Mellal', 'Casablanca', 'Moulins de Tadla — plateforme de Casablanca', 9, 'BON', [('Sacs de farine', 180, 25)]),
    ('El Jadida', 'Casablanca', 'Emballages Doukkala — dépôt central', 0, 'VALIDE', [('Lots d’emballages alimentaires', 150, 10)]),
    ('Agadir', 'Marrakech', 'Coopérative Tifawt — boutique de Marrakech', None, 'BROUILLON', [('Cartons d’huile d’argan', 40, 12), ('Cartons de miel', 30, 10)]),
    ('Casablanca', 'Ouarzazate', 'Mobilier Atlas — agence de Ouarzazate', None, 'BROUILLON', [('Tables emballées', 20, 35), ('Chaises emballées', 80, 8)]),
    ('Nador', 'Fès', 'Comptoir Méditerranée — dépôt de Fès', None, 'BROUILLON', [('Cartons de fournitures', 65, 15)]),
    ('Essaouira', 'Marrakech', 'Artisanat Mogador — dépôt de Marrakech', None, 'BROUILLON', [('Caisses d’objets artisanaux', 35, 18), ('Cartons de textiles', 20, 14)]),
]
base = datetime.fromisoformat(state['createdAt'])


def instant(hours):
    return (base + timedelta(hours=hours)).isoformat()


for index, (origin, destination, recipient, truck_index, target, goods) in enumerate(scenarios):
    key = str(index)
    recipient += ' (démo)'
    if key not in state['loadings']:
        state['loadings'][key] = api('chargements', '/chargements', 'POST', dict(
            origine=origin, destination=destination, destinataireNom=recipient, destinataireTelephone=None))['id']
        checkpoint()
    loading_id = state['loadings'][key]
    loading = api('chargements', '/chargements/' + loading_id)
    if loading['statut'] == 'BROUILLON':
        for designation, quantity, weight in goods:
            if not any(row['designation'] == designation for row in loading['lignes']):
                loading = api('chargements', f'/chargements/{loading_id}/lignes', 'POST', dict(
                    designation=designation, quantite=quantity, poidsUnitaireKg=weight))
        if target != 'BROUILLON':
            loading = api('chargements', f'/chargements/{loading_id}/validation', 'POST', dict(camionId=state['trucks'][str(truck_index)]))
    if target in ['BROUILLON', 'VALIDE']:
        print(f'{index + 1:02d}. {origin} -> {destination}: {loading["statut"]}', flush=True)
        continue
    if key not in state['bons']:
        existing = [row for row in all_rows('bons') if row['chargementId'] == loading_id]
        state['bons'][key] = existing[0]['id'] if existing else api('bons', '/bons', 'POST', dict(chargementId=loading_id))['id']
        checkpoint()
    if target == 'BON':
        print(f'{index + 1:02d}. {origin} -> {destination}: BON EMIS', flush=True)
        continue
    if key not in state['transports']:
        state['transports'][key] = api('transports', '/transports', 'POST', dict(
            bonId=state['bons'][key], camionId=state['trucks'][str(truck_index)],
            departPrevu=instant(3 + index if target == 'PLANIFIE' else -24 - index),
            operationKey=f'transit-demo-v1:{key}'))['id']
        checkpoint()
    transport_id = state['transports'][key]
    transport_path = '/transports/' + transport_id
    transport = api('transports', transport_path)
    if target == 'ANNULE' and transport['statut'] in ['PLANIFIE', 'ANNULATION_EN_ATTENTE']:
        transport = api('transports', transport_path + '/annulation', 'POST')
    if target in ['EN_COURS', 'LIVRE', 'CLOTURE_EN_ATTENTE'] and transport['statut'] == 'PLANIFIE':
        transport = api('transports', transport_path + '/depart', 'POST', dict(departReel=instant(-23 - index)))
    if target in ['EN_COURS', 'LIVRE', 'CLOTURE_EN_ATTENTE'] and key not in state['events'] and transport['statut'] == 'EN_COURS':
        description = 'Pointage logistique : marchandise sécurisée, trajet conforme au planning (démo).'
        existing_events = api('transports', transport_path + '/evenements?size=100')['content']
        if not any(row['description'] == description for row in existing_events):
            api('transports', transport_path + '/evenements', 'POST', dict(type='POINTAGE', description=description, dateEvenement=instant(-21 - index)))
        state['events'].append(key)
        checkpoint()
    if target in ['LIVRE', 'CLOTURE_EN_ATTENTE'] and transport['statut'] == 'EN_COURS':
        transport = api('transports', transport_path + '/arrivee', 'POST', dict(arriveeReelle=instant(-16 - index)))
    if target == 'LIVRE' and transport['statut'] == 'CLOTURE_EN_ATTENTE':
        transport = api('transports', transport_path + '/livraison', 'POST', dict(receptionnaire='Responsable du dépôt (démo)',
            dateReception=(base + timedelta(hours=-16 - index)).date().isoformat(), commentaire='Marchandises reçues au complet. Emballages intacts. Exemple fictif de démonstration.'))
    print(f'{index + 1:02d}. {origin} -> {destination}: {transport["statut"]}', flush=True)

after = {service: all_rows(service) for service in PORTS}
for service, before in state['before'].items():
    by_id = {row['id']: row for row in after[service]}
    for row in before:
        if row != by_id.get(row['id']):
            raise RuntimeError(f'Pre-existing {service} record changed: {row["id"]}; review before continuing.')
state['verifiedAt'] = datetime.now(timezone.utc).isoformat()
checkpoint()
(OUTPUT / 'summary.json').write_text(json.dumps(dict(
    added={service: len(after[service]) - len(state['before'][service]) for service in PORTS},
    previousRecordsUnchanged=True, totals={service: len(rows) for service, rows in after.items()}), indent=2), encoding='utf-8')
print('Demo ready. All pre-existing records preserved. Manifest: .tools/demo-data/manifest.json', flush=True)
