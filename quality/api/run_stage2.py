"""Run the DAT Postman collection and controlled read-only JMeter scenarios."""
import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
PRIVATE = ROOT / '.secrets/stage2'
PRIVATE.mkdir(parents=True, exist_ok=True)
sys.path.insert(0, str(ROOT / 'tests/integration'))
import test_system as support


def agent_token():
    return support.user_token('agent-ws', support.VALUES['WS_AGENT_PASSWORD'])


def postman(output):
    folder = output / 'PostmanNewman'
    folder.mkdir(parents=True, exist_ok=True)
    tokens = {
        'agentToken': agent_token(),
        'readerToken': support.user_token('lecteur-ws', support.VALUES['WS_LECTEUR_PASSWORD']),
        'bonsToken': support.token_request({'grant_type': 'client_credentials', 'client_id': 'bons-service',
                                           'client_secret': support.VALUES['BONS_CLIENT_SECRET']}),
    }
    env = {'name': 'DAT local isolé', 'values': [dict(key=k, value=v, enabled=True, type='secret')
                                              for k, v in tokens.items()]}
    (PRIVATE / 'postman-environment.json').write_text(json.dumps(env), encoding='utf-8')
    with (folder / 'execution-newman.log').open('w', encoding='utf-8') as log:
        result = subprocess.run(['node', str(ROOT / 'quality/api/run_newman.cjs'), str(folder)],
                                cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
    if result.returncode:
        raise RuntimeError('Postman/Newman failed. Read its output log and results before continuing.')
    data = json.loads((folder / 'resultats.json').read_text(encoding='utf-8'))
    print(f"Postman/Newman : {len(data['executions'])} requêtes ; "
          f"{data['statistiques']['assertions']['total']} assertions ; zéro échec.", flush=True)


def performance(output):
    fixtures = json.loads((PRIVATE / 'fixtures.json').read_text(encoding='utf-8'))
    required = ('truckId', 'loadingId', 'bonId', 'transportId')
    assert all(fixtures.get(key) for key in required)
    # Preflight every sample resource before starting a load generator.
    token = agent_token()
    for service, resource, key in [('camions', 'camions', 'truckId'), ('chargements', 'chargements', 'loadingId'),
                                  ('bons', 'bons', 'bonId'), ('transports', 'transports', 'transportId')]:
        status, data = support.api(service, '/' + resource + '/' + fixtures[key], token=token)
        assert status == 200 and data['id'] == fixtures[key], (service, status)
    jar = ROOT / '.tools/apache-jmeter-5.6.3/bin/ApacheJMeter.jar'
    if not jar.exists():
        raise RuntimeError('Install the official JMeter 5.6.3 binary under .tools first.')
    all_stats = []
    folder = output / 'JMeter'
    folder.mkdir(parents=True, exist_ok=True)
    for users, ramp in [(1, 1), (5, 3), (10, 5)]:
        dest = folder / f'{users:02}-utilisateurs'
        dest.mkdir(exist_ok=False)
        # A fresh user JWT avoids including token expiry in these short read tests.
        props = PRIVATE / 'jmeter.properties'
        props.write_text('agentToken=' + agent_token() + '\n' +
                         ''.join(key + '=' + fixtures[key] + '\n' for key in required), encoding='utf-8')
        command = ['java', '-Xms256m', '-Xmx768m', '-jar', str(jar), '-n',
                   '-t', str(ROOT / 'quality/api/Lectures_quatre_services.jmx'),
                   '-q', str(props), f'-Jusers={users}', f'-Jramp={ramp}', '-Jiterations=20',
                   '-Jjmeter.save.saveservice.requestHeaders=false',
                   '-Jjmeter.save.saveservice.responseHeaders=false',
                   '-l', str(dest / 'mesures.jtl'), '-j', str(dest / 'jmeter.log'),
                   '-e', '-o', str(dest / 'rapport')]
        print(f'JMeter : {users} utilisateur(s), 20 itérations, quatre GET authentifiés...', flush=True)
        with (dest / 'execution.log').open('w', encoding='utf-8') as log:
            result = subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
        if result.returncode:
            raise RuntimeError(f'JMeter failed for {users} users; keep the failed output for diagnosis.')
        statistics = json.loads((dest / 'rapport/statistics.json').read_text(encoding='utf-8'))
        total = statistics['Total']
        assert total['sampleCount'] == users * 20 * 4, total
        assert total['errorCount'] == 0, total
        row = {'utilisateurs': users, 'iterationsParUtilisateur': 20, 'rampUpSecondes': ramp,
               'pauseMsAvantChaqueRequete': 200, 'statistiques': statistics,
               'dateUTC': datetime.now(timezone.utc).isoformat()}
        all_stats.append(row)
        (folder / 'resultats.json').write_text(json.dumps(all_stats, indent=2), encoding='utf-8')
        print(f"  {total['sampleCount']} requêtes, {total['errorCount']} erreur, "
              f"p95 = {total['pct2ResTime']:.1f} ms.", flush=True)
    protocol = {'outil': 'Apache JMeter 5.6.3', 'java': '21', 'base': 'PostgreSQL 18 local isolé',
                'servicesReels': True, 'keycloakReel': True, 'ecrituresPendantLaCharge': False,
                'niveauxUtilisateurs': [1, 5, 10], 'iterationsParUtilisateur': 20,
                'routesParIteration': 4, 'echantillonsAttendus': 1280,
                'verification': 'HTTP 200 et identifiant JSON attendu pour chaque échantillon',
                'limites': 'Générateur et serveurs sur le même PC, jeu de données fictif réduit, JWT obtenu avant chaque niveau. Sans objectif de latence contractuel. Les seuils Apdex natifs sont les valeurs outil, pas un SLA du projet.'}
    (folder / 'protocole.json').write_text(json.dumps(protocol, ensure_ascii=False, indent=2), encoding='utf-8')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=('postman', 'performance', 'all'))
    parser.add_argument('--output', type=Path, default=ROOT / '.tools/api-performance')
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    if args.action in ('postman', 'all'):
        postman(output)
    if args.action in ('performance', 'all'):
        performance(output)


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    main()
