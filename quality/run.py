"""Run the project's style and dependency checks without starting its services."""
import argparse
from datetime import datetime
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MODULES = ('flotte', 'chargements', 'bons', 'transports')
TOOLS = ROOT / '.tools/quality-tools'
BIN = TOOLS / 'node_modules/.bin'
SUFFIX = '.cmd' if os.name == 'nt' else ''


def execute(command, cwd, log=None):
    result = subprocess.run([str(value) for value in command], cwd=cwd,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            encoding='utf-8', errors='replace')
    if log:
        log.write_text(result.stdout, encoding='utf-8')
    return result


def tool(name):
    executable = BIN / (name + SUFFIX)
    if not executable.exists():
        npm = shutil.which('npm' + SUFFIX)
        if not npm:
            raise RuntimeError('Node.js and npm are required for Snyk.')
        install = execute([npm, 'install', '--prefix', TOOLS, '--no-audit', '--no-fund',
                           '--save-exact', 'snyk@1.1309.0', 'snyk-to-html@3.7.12'], ROOT)
        if install.returncode:
            raise RuntimeError(install.stdout)
    return executable


def checkstyle(output):
    maven = shutil.which('mvn' + SUFFIX)
    if not maven:
        raise RuntimeError('Maven and Java 21 are required.')
    folder = output / 'Checkstyle'
    folder.mkdir(parents=True, exist_ok=True)
    results = []
    for module in MODULES:
        source = ROOT / 'services' / module
        target = folder / module
        target.mkdir(exist_ok=True)
        print(f'Checkstyle : {module} ...', flush=True)
        # A fresh report must list every source, including files clean in a prior run.
        cache = source / 'target/checkstyle-cachefile'
        cache.unlink(missing_ok=True)
        command = [maven, '-B', '-ntp', '-Pqualite', 'checkstyle:checkstyle']
        run = execute(command, source, target / 'maven.log')
        xml = source / 'target/checkstyle-result.xml'
        # Never label a failed Maven invocation as a successful style check.
        files = ET.parse(xml).findall('file') if xml.exists() else []
        violations = sum(len(file.findall('error')) for file in files)
        report = source / 'target/reports'
        if report.exists():
            shutil.copytree(report, target, dirs_exist_ok=True)
        if xml.exists():
            shutil.copy2(xml, target / xml.name)
        item = dict(module=module, exitCode=run.returncode, files=len(files),
                    violations=violations if files else None,
                    passed=run.returncode == 0 and bool(files) and violations == 0)
        results.append(item)
        print(f"  {item['files']} fichiers ; violations : {item['violations']} ; "
              f"{'SUCCES' if item['passed'] else 'ECHEC - voir maven.log'}", flush=True)
    (folder / 'resultats.json').write_text(json.dumps(results, indent=2), encoding='utf-8')
    return all(item['passed'] for item in results)


def snyk(output):
    cli, converter = tool('snyk'), tool('snyk-to-html')
    folder = output / 'Snyk'
    folder.mkdir(parents=True, exist_ok=True)
    version = execute([cli, '--version'], ROOT).stdout.strip()
    results = []
    projects = [(module, ROOT / 'services' / module, 'pom.xml', 'maven') for module in MODULES]
    projects.append(('frontend', ROOT / 'frontend', 'package.json', 'npm'))
    for name, source, manifest, manager in projects:
        print(f'Snyk : {name} ...', flush=True)
        # Each invocation has a unique raw output; old successful data cannot mask a failure.
        raw = folder / f'{name}-{datetime.now():%Y%m%d-%H%M%S}.json'
        run = execute([cli, 'test', f'--file={manifest}', f'--package-manager={manager}',
                       '--dev', f'--json-file-output={raw}'], source, folder / f'{name}.log')
        data = json.loads(raw.read_text(encoding='utf-8-sig')) if raw.exists() else {}
        scanned = (run.returncode in (0, 1) and isinstance(data, dict)
                   and isinstance(data.get('vulnerabilities'), list) and 'dependencyCount' in data)
        item = dict(project=name, version=version, exitCode=run.returncode, scanned=scanned,
                    dependencies=data.get('dependencyCount') if scanned else None)
        if scanned:
            shutil.copy2(raw, folder / f'{name}.json')
            issues = {issue['id']: issue for issue in data['vulnerabilities']}
            item['issues'] = len(issues)
            item['severities'] = {level: sum(v['severity'] == level for v in issues.values())
                                  for level in ('critical', 'high', 'medium', 'low')}
            item['htmlExitCode'] = execute([converter, '-i', raw, '-o', folder / f'{name}.html'],
                                            ROOT, folder / f'{name}-html.log').returncode
            print(f"  {item['dependencies']} dependances ; {item['issues']} alertes distinctes ; "
                  f"{item['severities']}", flush=True)
        else:
            item['error'] = data.get('error', 'Scan incomplete; see the project log.')
            print(f'  SCAN NON TERMINE - voir {name}.log', flush=True)
        results.append(item)
    (folder / 'resultats.json').write_text(json.dumps(results, indent=2), encoding='utf-8')
    return all(item['scanned'] and item['issues'] == 0 and item.get('htmlExitCode') == 0
               for item in results)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=('checkstyle', 'snyk', 'auth', 'all'))
    parser.add_argument('--output', type=Path, default=ROOT / '.tools/qualite')
    args = parser.parse_args()
    if args.action == 'auth':
        return subprocess.call([str(tool('snyk')), 'auth'], cwd=ROOT)
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    ok = True
    if args.action in ('checkstyle', 'all'):
        ok = checkstyle(output) and ok
    if args.action in ('snyk', 'all'):
        ok = snyk(output) and ok
    print(f'Rapports : {output}')
    return 0 if ok else 1


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    sys.exit(main())
