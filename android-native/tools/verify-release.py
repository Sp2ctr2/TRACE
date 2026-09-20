#!/usr/bin/env python3
"""Install the optimized demo APK and inspect its real, cold-launched native UI.

This is a release smoke test, not a substitute for the debug safety/UI suite.
It uses only ADB and Python's standard library on the dedicated CI emulator.
"""
from pathlib import Path
import hashlib
import json
import re
import shutil
import subprocess
import time
import xml.etree.ElementTree as ET

PACKAGE = 'app.saeon.trace.demo'
COMPONENT = PACKAGE + '/app.saeon.trace.MainActivity'
root = Path('verification')
output = root / 'release'
output.mkdir(parents=True, exist_ok=True)
apk = Path('app/build/outputs/apk/release/app-release.apk')


def adb(*args: str, timeout: int = 45) -> bytes:
    result = subprocess.run(['adb', *args], stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
    if result.returncode:
        raise RuntimeError(f'ADB failed ({result.returncode}): {args}: {result.stderr.decode(errors="replace")}')
    return result.stdout


if not apk.is_file():
    raise SystemExit('Release APK is missing; no release verification claimed.')
shutil.copyfile(apk, root / 'apk' / 'saeon-trace-release-demo.apk')
install = adb('install', '-r', str(apk)).decode()
(output / 'install.txt').write_text(install)
if 'Success' not in install:
    raise SystemExit('Release installation was not successful.')
adb('shell', 'am', 'force-stop', PACKAGE)
launch = adb('shell', 'am', 'start', '-W', '-n', COMPONENT).decode()
(output / 'cold-start.txt').write_text(launch)
if 'Status: ok' not in launch:
    raise SystemExit('Release did not cold-launch successfully.')
package_info = adb('shell', 'dumpsys', 'package', PACKAGE).decode()
(output / 'package.txt').write_text(package_info)
flags = re.findall(r'^\s*(?:flags|pkgFlags)=\[(.*?)\]', package_info, flags=re.M)
if not flags or any('DEBUGGABLE' in flag for flag in flags):
    raise SystemExit('Installed release application flags are missing or still debuggable.')
ready = False
for attempt in range(8):
    adb('shell', 'uiautomator', 'dump', '/sdcard/saeon-release.xml')
    raw = adb('exec-out', 'cat', '/sdcard/saeon-release.xml')
    (output / 'home.xml').write_bytes(raw)
    tree = ET.fromstring(raw)
    nodes = [node for node in tree.iter('node') if node.get('package') == PACKAGE]
    labels = {node.get('text', '') for node in nodes} | {node.get('content-desc', '') for node in nodes}
    if '새온은행' in labels and '홈' in labels and '안전' in labels:
        ready = True
        break
    time.sleep(0.5)
(output / 'home.png').write_bytes(adb('exec-out', 'screencap', '-p'))
if not ready:
    raise SystemExit('Release home UI did not become visible; screenshot and XML retained.')
installed = adb('shell', 'pm', 'path', PACKAGE).decode().strip()
paths = [line.removeprefix('package:') for line in installed.splitlines() if line.startswith('package:')]
if len(paths) != 1:
    raise SystemExit('Unexpected installed APK set; could not compare the exact APK.')
installed_hash = hashlib.sha256(adb('exec-out', 'cat', paths[0])).hexdigest()
expected_hash = hashlib.sha256(apk.read_bytes()).hexdigest()
if installed_hash != expected_hash:
    raise SystemExit('Installed release APK differs from the build output.')
summary = {
    'installation': 'PASS', 'cold_launch': 'PASS', 'native_home': 'PASS',
    'debuggable': False, 'installed_apk_matches_build': True,
    'apk_sha256': expected_hash,
    'scope': 'Release smoke test only. Full domain/device regression uses the debug APK.'
}
(output / 'release.json').write_text(json.dumps(summary, indent=2, ensure_ascii=False))
print(json.dumps(summary, indent=2, ensure_ascii=False))
