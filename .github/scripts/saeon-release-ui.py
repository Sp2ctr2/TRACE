#!/usr/bin/env python3
"""Black-box checks for the exact optimized APK, without app test hooks."""
from pathlib import Path
import hashlib
import json
import os
import re
import subprocess
import sys
import time
import traceback
import xml.etree.ElementTree as ET

source = Path(sys.argv[1])
out = Path('android-native/verification/release')
out.mkdir(parents=True, exist_ok=True)
adb_bin = str(Path(os.environ['ANDROID_HOME']) / 'platform-tools/adb')
package = 'app.saeon.trace.demo'
activity = package + '/app.saeon.trace.MainActivity'
apk = source / 'saeon-trace-release-demo.apk'
results = {'source_commit': (source / 'COMMIT.txt').read_text().strip(),
           'apk_sha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
           'variant': 'R8-optimized release demo; development signing',
           'checks': [], 'environment_events': [], 'passed': False}
log = []

def adb(*args, binary=False, timeout=40):
    p = subprocess.run([adb_bin, *map(str, args)], capture_output=True, timeout=timeout)
    if p.returncode:
        raise RuntimeError(f'adb {args}: {(p.stderr or p.stdout).decode(errors="replace")}')
    return p.stdout if binary else p.stdout.decode(errors='replace')

def norm(s): return ' '.join(s.split())

def bounds(node):
    values = list(map(int, re.findall(r'\d+', node.get('bounds', ''))))
    return values if len(values) == 4 else (0, 0, 0, 0)

def dump():
    for attempt in range(5):
        try:
            adb('shell', 'uiautomator', 'dump', '/sdcard/saeon-release-window.xml', timeout=25)
            xml = adb('exec-out', 'cat', '/sdcard/saeon-release-window.xml')
            root = ET.fromstring(xml)
            (out / 'latest.xml').write_text(xml)
            # API-35 Pixel Launcher can ANR during first-boot wm-size changes.
            # Recover ONLY that named third-party system launcher. Never dismiss
            # SAEON, System UI or an unidentified crash/ANR, and retain evidence.
            launcher = any(n.get('text') == "Pixel Launcher isn't responding" for n in root.iter('node'))
            if launcher:
                count = len(results['environment_events'])
                if count >= 2: raise AssertionError('Repeated Pixel Launcher ANR; environment is unstable')
                close = next((n for n in root.iter('node') if n.get('resource-id') == 'android:id/aerr_close'), None)
                if close is None: raise AssertionError('Cannot identify launcher-only ANR close control')
                name = f'environment_pixel_launcher_anr_{count+1}'
                (out/(name+'.xml')).write_text(xml)
                (out/(name+'.png')).write_bytes(adb('exec-out','screencap','-p',binary=True))
                results['environment_events'].append({'event':'Pixel Launcher ANR after emulator resize', 'action':'closed only the launcher dialog', 'evidence':name})
                x1,y1,x2,y2 = bounds(close)
                adb('shell','input','tap',(x1+x2)//2,(y1+y2)//2)
                time.sleep(1)
                continue
            return root
        except (RuntimeError, ET.ParseError, subprocess.TimeoutExpired):
            if attempt == 4: raise
            time.sleep(1)
    raise AssertionError('No stable Android accessibility hierarchy')

def matches(root, text, contains=False):
    wanted = norm(text)
    found = []
    for n in root.iter('node'):
        labels = [norm(n.get('text', '')), norm(n.get('content-desc', ''))]
        x1,y1,x2,y2 = bounds(n)
        if x2 > x1 and y2 > y1 and any(wanted in s if contains else wanted == s for s in labels):
            found.append(n)
    return sorted(found, key=lambda n: (bounds(n)[1], (bounds(n)[2]-bounds(n)[0])*(bounds(n)[3]-bounds(n)[1])))

def wait(text, contains=False):
    until=time.monotonic()+35
    while time.monotonic() < until:
        root=dump()
        if matches(root,text,contains): return root
        time.sleep(.5)
    raise AssertionError('Expected visible content: '+text)

def tap(text, contains=False, scroll=False):
    for attempt in range(7 if scroll else 3):
        root=dump(); found=matches(root,text,contains)
        if found:
            n=found[0]
            if n.get('enabled') == 'false': raise AssertionError('Disabled control: '+text)
            x1,y1,x2,y2=bounds(n)
            adb('shell','input','tap',(x1+x2)//2,(y1+y2)//2)
            log.append('Tap: '+text); time.sleep(.35); return
        if scroll: adb('shell','input','swipe',393,1300,393,550,400)
        time.sleep(.5)
    raise AssertionError('Control not found: '+text)

def capture(name):
    root=dump()
    (out/(name+'.xml')).write_text(ET.tostring(root,encoding='unicode'))
    data=adb('exec-out','screencap','-p',binary=True)
    if not data.startswith(b'\x89PNG\r\n\x1a\n'): raise AssertionError('Not a real Android PNG')
    (out/(name+'.png')).write_bytes(data)

def launch():
    adb('shell','am','force-stop',package)
    text=adb('shell','am','start','-W','-n',activity)
    if 'Error' in text: raise AssertionError(text)
    log.append(text); wait('새온 생활통장')

def scenario(label):
    launch(); tap('전체'); tap('앱 정보',scroll=True)
    for _ in range(5): tap('버전',scroll=True)
    wait('TRACE Demo Lab'); tap(label,scroll=True); tap('시작')
    wait('새온 생활통장'); wait('12,840,000원')

def transfer(name, amount):
    tap('송금'); tap(name,scroll=True); wait('얼마를 보낼까요?')
    tap('다음'); wait('보내기 전 확인'); tap(f'{amount:,}원 보내기')
    tap('시연 확인')

def check(name):
    results['checks'].append({'name':name,'status':'PASS'})
    print('PASS:',name,flush=True)

try:
    for setting in ('window_animation_scale','transition_animation_scale','animator_duration_scale'):
        adb('shell','settings','put','global',setting,0)
    adb('shell','settings','put','system','font_scale',1.0)
    adb('shell','wm','density',320); adb('shell','wm','size','786x1746')
    adb('shell','settings','put','global','airplane_mode_on',1)
    adb('shell','svc','wifi','disable'); adb('shell','svc','data','disable')
    installed=adb('install','-r',str(apk)); log.append(installed)
    if 'Success' not in installed: raise AssertionError('Release APK did not install')
    adb('shell','pm','clear',package)
    launch(); capture('01_release_home'); check('optimized APK installs and launches offline')
    transfer('이서연',32000)
    wait('보냈어요.',contains=True); capture('02_release_normal_complete')
    tap('확인'); wait('12,808,000원'); check('normal transfer through release UI debits 32,000 exactly once')
    scenario('기관 사칭'); transfer('김○○',3000000)
    root=wait('보내지 않아도 괜찮아요.',contains=True)
    for label in ('그래도 계속','무시하고 송금','위험 감수'):
        if matches(root,label,True): raise AssertionError('HOLD exposes bypass')
    capture('03_release_hold'); check('impersonation enters HOLD without a bypass control')
    launch(); wait('12,840,000원')
    tap('안전'); tap('김○○ · 3,000,000원',contains=True,scroll=True)
    wait('보내지 않아도 괜찮아요.',contains=True)
    capture('04_release_hold_restored'); check('force-stop preserves HOLD and unchanged balance')
    scenario('대출 상환'); transfer('박○○',8000000)
    wait('돈이 맞나요?',contains=True); tap('공식 상환 경로 확인')
    wait('받는 곳을 찾았어요.',contains=True); capture('05_release_official_route')
    tap('새 송금 내역 확인'); wait('보내기 전 확인')
    tap('8,000,000원 보내기'); tap('시연 확인')
    wait('상환을 마쳤어요.',contains=True); capture('06_release_loan_complete')
    tap('확인'); wait('4,840,000원'); check('verified loan route requires fresh review and authentication')
    scenario('공식 경로 조회 실패'); transfer('박○○',8000000)
    wait('돈이 맞나요?',contains=True); tap('공식 상환 경로 확인')
    wait('보내지 않습니다.',contains=True); capture('07_release_unknown')
    launch(); wait('12,840,000원'); check('unavailable route leaves release-demo balance unchanged')
    results['passed']=True
except Exception as error:
    results['error']=repr(error)
    log.append(traceback.format_exc())
    try: capture('failure_release_ui')
    except Exception as secondary: log.append(repr(secondary))
finally:
    (out/'release-verification.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
    (out/'release-ui.log').write_text('\n'.join(log)+'\n')
    try: (out/'crash-buffer.txt').write_text(adb('logcat','-b','crash','-d'))
    except Exception: pass
    print(json.dumps(results,ensure_ascii=False,indent=2),flush=True)
if not results['passed']: raise SystemExit(1)
