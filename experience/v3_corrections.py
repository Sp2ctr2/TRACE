from pathlib import Path
r=Path('android-native')
for p in r.rglob('*.kt'):
    if 'build' in p.parts: continue
    lines=p.read_text().splitlines();seen=set();out=[]
    for line in lines:
        if line.startswith('import '):
            if line in seen:continue
            seen.add(line)
        out.append(line)
    p.write_text('\n'.join(out)+'\n')
p=r/'app/src/main/kotlin/app/saeon/trace/ui/screens/SettingsScreens.kt'
s=p.read_text().replace('fun MoreScreen(open:', 'fun MoreScreen(preferences: BankPreferences, open:').replace('MenuRow("한지우님",','MenuRow("${preferences.displayName}님",')
p.write_text(s)
p=r/'app/src/main/kotlin/app/saeon/trace/ui/SaeonApp.kt'
s=p.read_text().replace('MoreScreen(open)','MoreScreen(preferences, open)');p.write_text(s)
print('Kotlin source imports normalized; profile binding corrected')
