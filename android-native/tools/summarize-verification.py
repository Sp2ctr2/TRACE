#!/usr/bin/env python3
"""Fail-closed evidence summary, including missing/failed runs and captures."""
from pathlib import Path
import hashlib
import json
import re

root = Path('verification')
root.mkdir(exist_ok=True)
logs = root / 'logs'
full_classes = ['RepositoryDeviceTest', 'BankUiFlowTest', 'GoldenScreensTest', 'PrivacyLifecycleTest']
test_source = Path('app/src/androidTest/kotlin/app/saeon/trace')
full_count = sum(len(re.findall(r'@Test\b', (test_source / (name + '.kt')).read_text())) for name in full_classes)
expected = {}
for number in (1, 2):
    expected[f'pass-{number}'] = full_count
    expected[f'pass-{number}-seed'] = 1
    expected[f'pass-{number}-restore'] = 1
matrix = [f'matrix-{size}-{scale}' for size in ('720x1600', '786x1746', '824x1830') for scale in ('1.0', '1.15', '1.3', '1.5', '2.0')]
matrix += ['matrix-landscape-1.0', 'matrix-landscape-2.0', 'dark-system-forced-light']
expected.update({name: 1 for name in matrix})
results = {}
for name, count in expected.items():
    path = logs / (name + '.txt')
    text = path.read_text(errors='replace') if path.exists() else ''
    match = re.search(r'^OK \((\d+) tests?\)', text, re.M)
    ran = int(match.group(1)) if match else 0
    errors = any(word in text for word in ('FAILURES!!!', 'INSTRUMENTATION_FAILED', 'Process crashed', 'INSTRUMENTATION_ABORTED'))
    status = 'PASS' if match and ran == count and not errors else ('FAIL' if path.exists() else 'NOT_RUN')
    results[name] = {'status': status, 'tests': ran, 'expected_tests': count}

images = sorted((root / 'screens').rglob('*.png'))
audits = sorted((root / 'screens').rglob('*.audit.txt'))
audit_failures = [str(path) for path in audits if not path.read_text().startswith('PASS:')]
golden = ['01_Home', '02_Assets', '03_Account_Detail', '04_Transfer_Recipient', '05_Transfer_Amount', '06_Transfer_Review',
          '07_Evaluating', '08_Normal_Complete', '09_WARN', '10_HOLD', '11_HOLD_Reason_Sheet', '12_Safety_Guide',
          '13_Risk_Timeline', '14_VERIFY', '15_Official_Route', '16_UNKNOWN', '17_Safety_Center', '18_Shared_Text_Review',
          '19_Privacy', '20_Easy_Mode', '21_History', '22_Settings', '23_Demo_Lab']
required_images = [(f'pass-{n}', name) for n in (1, 2) for name in golden]
required_images += [(run, name) for run in matrix for name in ('matrix_Home', 'matrix_Review', 'matrix_HOLD', 'matrix_Easy_HOLD')]
missing_images = []
for run, name in required_images:
    matches = [p for p in images if p.parent.name == run and p.stem == name]
    if len(matches) != 1 or not matches[0].with_suffix('.audit.txt').exists():
        missing_images.append(f'{run}/{name}')

summary = {
    'instrumentation_runs': results,
    'instrumentation_test_executions': sum(result['tests'] for result in results.values()),
    'expected_instrumentation_test_executions': sum(expected.values()),
    'two_consecutive_full_passes': all((root / f'pass-{n}.passed').exists() and all(results[f'pass-{n}{suffix}']['status'] == 'PASS' for suffix in ('', '-seed', '-restore')) for n in (1, 2)),
    'screenshots': len(images),
    'required_screenshots': len(required_images),
    'missing_or_duplicate_required_screenshots': missing_images,
    'layout_audits': len(audits),
    'layout_audit_failures': audit_failures,
    'talkback_manual_listening': 'NOT_RUN; semantics inspection does not prove spoken usability',
    'physical_device_performance': 'NOT_RUN; emulator evidence only',
    'apk_sha256': {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in (root / 'apk').glob('*.apk')},
}
summary['passed'] = bool(summary['two_consecutive_full_passes'] and full_count > 0 and not missing_images and not audit_failures
                         and summary['apk_sha256'] and all(r['status'] == 'PASS' for r in results.values()))
(root / 'verification.json').write_text(json.dumps(summary, indent=2, ensure_ascii=False) + '\n')
lines = ['# Android device verification', '', f"Overall: {'PASS' if summary['passed'] else 'INCOMPLETE / FAIL'}", '',
         'Results below are parsed from actual Android instrumentation, not inferred from source or APK build success.', '',
         f"Two consecutive full passes: {summary['two_consecutive_full_passes']}",
         f"Test executions: {summary['instrumentation_test_executions']} / {sum(expected.values())} expected",
         f"Actual screenshots: {len(images)}; required named captures: {len(required_images)}",
         f"Missing/duplicate captures: {len(missing_images)}; failed layout audits: {len(audit_failures)}", '', '## Runs', '']
lines += [f"- {name}: {r['status']}; {r['tests']}/{r['expected_tests']} tests" for name, r in results.items()]
lines += ['', '## Limits', '', 'Manual TalkBack listening and physical-device performance: NOT_RUN.',
          'Biometric success needs enrolled hardware; offline speech needs a device-local recognition model.',
          'Layout assertions are not a certification of usability or fraud detection.',
          'All transactions use fictional funds. Rules and a same-process gateway do not establish remote-bank security.']
if missing_images:
    lines += ['', '## Missing or ambiguous captures', ''] + missing_images
(root / 'VERIFICATION.md').write_text('\n'.join(lines) + '\n')
if not summary['passed']:
    raise SystemExit('Device evidence incomplete or failed; inspect verification.json and instrumentation logs.')
