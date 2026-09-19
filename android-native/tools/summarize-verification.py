#!/usr/bin/env python3
"""Summarize only existing device output; never turns missing evidence into a pass."""
from pathlib import Path
import hashlib
import json
import re

root = Path('verification')
logs = root / 'logs'
results = {}
for path in sorted(logs.glob('*.txt')):
    text = path.read_text(errors='replace')
    match = re.search(r'^OK \((\d+) tests?\)', text, re.M)
    if match:
        results[path.stem] = {'tests': int(match.group(1)), 'passed': 'FAILURES!!!' not in text}
images = sorted((root / 'screens').rglob('*.png'))
audits = sorted((root / 'screens').rglob('*.audit.txt'))
audit_failures = [str(path) for path in audits if not path.read_text().startswith('PASS:')]
summary = {
    'instrumentation_runs': results,
    'instrumentation_test_executions': sum(result['tests'] for result in results.values()),
    'two_consecutive_full_passes': all((root / f'pass-{number}.passed').exists() for number in (1, 2)),
    'screenshots': len(images),
    'layout_audits': len(audits),
    'layout_audit_failures': audit_failures,
    'talkback_manual_listening': 'not claimed; automatic semantics and device-service inventory are supplied',
    'apk_sha256': {path.name: hashlib.sha256(path.read_bytes()).hexdigest() for path in (root / 'apk').glob('*.apk')},
}
(root / 'verification.json').write_text(json.dumps(summary, indent=2, ensure_ascii=False))
lines = ['# Device verification', '',
         'This report is generated from actual Android instrumentation output.', '',
         f"Two consecutive full passes: {summary['two_consecutive_full_passes']}",
         f"Instrumentation test executions: {summary['instrumentation_test_executions']}",
         f"Actual emulator screenshots: {len(images)}", f"Visible-layout audits: {len(audits)}", '',
         '## Runs', '']
lines += [f"- {name}: {result['tests']} tests, {'PASS' if result['passed'] else 'FAIL'}" for name, result in results.items()]
lines += ['', '## Explicit limits', '',
          'Automated semantics/target-size inspection is not a claim of manual TalkBack listening.',
          'Biometric success requires suitable enrolled hardware. On-device speech requires a locally installed recognition model.',
          'Emulator timings are CI measurements, not physical-device performance guarantees.',
          'The local rules and same-process fixture gateway are not a certified production fraud-prevention service.']
(root / 'VERIFICATION.md').write_text('\n'.join(lines) + '\n')
if not summary['two_consecutive_full_passes'] or audit_failures:
    raise SystemExit('Verification evidence is incomplete or contains failed layout audits.')
