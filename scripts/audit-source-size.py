#!/usr/bin/env python3
"""Repeatable internal size estimate; not a tokenizer or Plugin Hub limit.

Run from any directory. Optionally pass --base <git-ref> for checkpoint growth.
The estimate is ceil(Unicode source characters / 4), including comments/whitespace.
"""
import argparse
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base', help='Git revision to compare with the working tree')
args = parser.parse_args()


def summarize(sources):
    rows = [{'path': path, 'characters': len(text), 'lines': len(text.splitlines())}
            for path, text in sources]
    characters = sum(row['characters'] for row in rows)
    return {'files': len(rows), 'lines': sum(row['lines'] for row in rows),
            'characters': characters, 'estimated_tokens': (characters + 3) // 4,
            'largest': sorted(rows, key=lambda row: (-row['characters'], row['path']))[:5]}


current = summarize((str(path.relative_to(ROOT)), path.read_text(encoding='utf-8'))
                    for path in sorted((ROOT / 'src/main/java').rglob('*.java')))
report = {'estimate': 'ceil(Unicode characters / 4); all production Java', 'current': current}
if args.base:
    revision = subprocess.check_output(['git', 'rev-parse', '--verify', args.base + '^{commit}'],
                                       cwd=ROOT, text=True).strip()
    paths = subprocess.check_output(['git', 'ls-tree', '-r', '--name-only', revision,
                                    '--', 'src/main/java'], cwd=ROOT, text=True).splitlines()
    baseline = summarize((path, subprocess.check_output(['git', 'show', revision + ':' + path],
                          cwd=ROOT).decode('utf-8')) for path in paths if path.endswith('.java'))
    report['base'] = revision
    report['growth'] = {key: current[key] - baseline[key]
                        for key in ('files', 'lines', 'characters', 'estimated_tokens')}
report['internal_guardrail'] = ('architecture intervention' if current['estimated_tokens'] >= 150000
                                else 'warning' if current['estimated_tokens'] >= 130000 else 'below warning')
print(json.dumps(report, indent=2))
