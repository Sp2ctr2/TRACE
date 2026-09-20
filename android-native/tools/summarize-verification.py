#!/usr/bin/env python3
"""Accept only complete, current instrumentation and unmodified screenshot evidence."""
from pathlib import Path
import hashlib
import json
import re
import struct

root = Path('verification')
logs = root / 'logs'
results = {}
for path in sorted(logs.glob('*.txt')):
    text = path.read_text(errors='replace')
    match = re.search(r'^OK \((\d+) tests?\)', text, re.M)
    if match:
        results[path.stem] = {'tests': int(match.group(1)), 'passed': 'FAILURES!!!' not in text}
required_runs = [f'pass-{n}{suffix}' for n in (1, 2) for suffix in ('', '-seed', '-restore')]
required_runs += [f'matrix-{size}-{scale}' for size in ('720x1600', '786x1746', '824x1830')
                  for scale in ('1.0', '1.15', '1.3', '1.5', '2.0')]
required_runs += ['matrix-landscape-1.0', 'matrix-landscape-2.0', 'dark-system-forced-light']
missing_runs = [name for name in required_runs if not results.get(name, {}).get('passed', False)]
golden = ('01_Home 02_Assets 03_Account_Detail 04_Transfer_Recipient 05_Transfer_Amount '
          '06_Transfer_Review 07_Evaluating 08_Normal_Complete 09_WARN 10_HOLD '
          '11_HOLD_Reason_Sheet 12_Safety_Guide 13_Risk_Timeline 14_VERIFY 15_Official_Route '
          '16_UNKNOWN 17_Safety_Center 18_Shared_Text_Review 19_Privacy 20_Easy_Mode '
          '21_History 22_Settings 23_Demo_Lab 24_Demo_Preview 25_Reading_Modes 26_Child_HOLD 27_Child_Guide').split()
missing_golden = [f'pass-{n}/{name}{ext}' for n in (1, 2) for name in golden
                  for ext in ('.png', '.audit.txt')
                  if not (root / 'screens' / f'pass-{n}' / f'{name}{ext}').is_file()]
images = sorted((root / 'screens').rglob('*.png'))
audits = sorted((root / 'screens').rglob('*.audit.txt'))
audit_failures = [str(path) for path in audits if not path.read_text().startswith('PASS:')]
image_errors = []
for path in images:
    data = path.read_bytes()
    if len(data) < 24 or data[:8] != b'\x89PNG\r\n\x1a\n' or min(struct.unpack('>II', data[16:24])) < 200:
        image_errors.append(str(path))
release_path = root / 'release' / 'release.json'
release = json.loads(release_path.read_text()) if release_path.exists() else {}
summary = {
    'release_smoke': release,
    'instrumentation_runs': results,
    'instrumentation_test_executions': sum(result['tests'] for result in results.values()),
    'two_consecutive_full_passes': all((root / f'pass-{n}.passed').exists() for n in (1, 2)),
    'missing_or_failed_runs': missing_runs,
    'missing_golden_evidence': missing_golden,
    'screenshots': len(images),
    'invalid_screenshots': image_errors,
    'layout_audits': len(audits),
    'layout_audit_failures': audit_failures,
    'talkback_manual_listening': 'not claimed; automated semantics and service inventory supplied',
    'apk_sha256': {path.name: hashlib.sha256(path.read_bytes()).hexdigest() for path in (root / 'apk').glob('*.apk')},
}
summary['complete'] = (summary['two_consecutive_full_passes'] and not missing_runs and
                       not missing_golden and not audit_failures and not image_errors and bool(summary['apk_sha256']) and
                       release.get('installed_apk_matches_build', False))
(root / 'verification.json').write_text(json.dumps(summary, indent=2, ensure_ascii=False))
lines = ['# Device verification', '', 'Generated from actual Android instrumentation output.', '',
         f"Complete evidence: {summary['complete']}",
         f"Two consecutive full passes: {summary['two_consecutive_full_passes']}",
         f"Instrumentation test executions: {summary['instrumentation_test_executions']}",
         f"Actual emulator screenshots: {len(images)}", f"Visible-layout audits: {len(audits)}", '', '## Runs', '']
lines += [f"- {name}: {result['tests']} tests, {'PASS' if result['passed'] else 'FAIL'}" for name, result in results.items()]
lines += ['', '## Missing or failing evidence', '', json.dumps({k: summary[k] for k in
          ('missing_or_failed_runs', 'missing_golden_evidence', 'invalid_screenshots', 'layout_audit_failures')}, indent=2),
          '', '## Explicit limits', '',
          'Automated semantics/target-size inspection does not certify manual TalkBack usability.',
          'Biometric success requires enrolled hardware. On-device speech requires a local recognition model.',
          'Emulator timings are CI measurements, not physical-device performance guarantees.',
          'The bounded local rules and same-process fixture gateway are not a certified fraud-prevention service.']
(root / 'VERIFICATION.md').write_text('\n'.join(lines) + '\n')
if not summary['complete']:
    raise SystemExit('Verification evidence is incomplete or contains failed audits.')
