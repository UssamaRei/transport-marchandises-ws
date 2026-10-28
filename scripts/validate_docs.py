"""Validate the shared contracts and documentation without running a backend."""
from pathlib import Path
import json
import re
import sys
from urllib.parse import unquote

import yaml
from openapi_spec_validator import OpenAPIV30SpecValidator
from openapi_schema_validator import OAS30Validator

ROOT = Path(__file__).resolve().parents[1]
CONTRACTS = ROOT / 'contracts'
SERVICES = ('flotte', 'chargements', 'bons', 'transports')


def main():
    failures = []
    specs = {}
    operation_count = 0
    for service in SERVICES:
        path = CONTRACTS / f'{service}.openapi.yaml'
        try:
            doc = yaml.safe_load(path.read_text(encoding='utf-8'))
            errors = list(OpenAPIV30SpecValidator(doc).iter_errors())
            failures.extend(f'{path.name}: {error.message}' for error in errors)
            specs[service] = doc
            operation_count += sum(method in ('get', 'post', 'patch', 'delete') for item in doc['paths'].values() for method in item)
        except Exception as error:
            failures.append(f'{path.name}: {error}')

    manifest = json.loads((CONTRACTS / 'examples/manifest.json').read_text(encoding='utf-8'))
    for item in manifest:
        try:
            doc = specs[item['contract'].removesuffix('.openapi.yaml')]
            schema = {'$ref': f"#/components/schemas/{item['schema']}", 'components': doc['components']}
            example = json.loads((CONTRACTS / 'examples' / item['file']).read_text(encoding='utf-8'))
            OAS30Validator(schema, format_checker=OAS30Validator.FORMAT_CHECKER).validate(example)
            if doc['components']['schemas'][item['schema']].get('example') != example:
                failures.append(f"Exemple YAML et JSON differents : {item['file']}")
        except Exception as error:
            failures.append(f"Exemple {item['file']}: {error}")

    calls = yaml.safe_load((CONTRACTS / 'communications.yaml').read_text(encoding='utf-8'))['calls']
    for call in calls:
        provider = specs.get(call['to'], {})
        operation = provider.get('paths', {}).get(call['path'], {}).get(call['method'])
        if not operation:
            failures.append(f'Appel sans operation exposee : {call}')
        elif call['from'] not in operation.get('x-consumers', []):
            failures.append(f'Consommateur absent du contrat : {call}')
    for service in SERVICES:
        if not any(c['from'] == service for c in calls):
            failures.append(f'{service} ne consomme aucune API')
        if not any(c['to'] == service for c in calls):
            failures.append(f'{service} ne fournit aucune API aux autres modules')

    diagrams_folder = ROOT / 'docs/diagrams'
    diagrams_manifest = json.loads((diagrams_folder / 'manifest.json').read_text(encoding='utf-8'))
    for item in diagrams_manifest:
        blocks = re.findall(r'```mermaid\s*\n(.*?)```', (ROOT / item['document']).read_text(encoding='utf-8'), re.S)
        original = blocks[item['index']].strip()
        exported = (diagrams_folder / f"{item['name']}.mmd").read_text(encoding='utf-8').strip()
        if original != exported:
            failures.append(f"Diagramme non synchronise : {item['name']}")

    link_count = 0
    for path in ROOT.rglob('*.md'):
        if any(part in ('.git', '.venv', 'node_modules') for part in path.relative_to(ROOT).parts):
            continue
        for match in re.finditer(r'!?\[[^\]]*\]\(([^)]+)\)', path.read_text(encoding='utf-8')):
            link = match.group(1).strip().strip('<>')
            if re.match(r'^[a-zA-Z][a-zA-Z0-9+.-]*:', link) or link.startswith('#'):
                continue
            target = unquote(link.split('#', 1)[0])
            link_count += 1
            if target and not (path.parent / target).exists():
                failures.append(f'Lien absent dans {path.relative_to(ROOT)} : {link}')

    if failures:
        print('\n'.join(f'ERROR: {item}' for item in failures))
        return 1
    print(f'OK: 4 contrats, {operation_count} operations, {len(manifest)} exemples, {len(calls)} appels interservices, {len(diagrams_manifest)} diagrammes synchronises et {link_count} liens locaux.')
    print('Les backends metier et le lancement des conteneurs restent a verifier separement.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
