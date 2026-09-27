from pathlib import Path
import shutil
r=Path('android-native');u=r/'app/src/main/kotlin/app/saeon/trace/ui';d=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design'
def edit(p,a,b):
 s=p.read_text()
 if a not in s:raise RuntimeError(f'Missing {p}: {a[:90]}')
 p.write_text(s.replace(a,b))
shutil.copy2('polish11/LiquidNavigation.kt',d/'LiquidNavigation.kt')
shutil.copy2('polish11/Acceptance11Test.kt',r/'app/src/androidTest/kotlin/app/saeon/trace/Acceptance11Test.kt')
p=d/'LiquidNavigation.kt';s=p.read_text().replace('    val outline=RoundedCornerShape(27.dp)','    val divider=TraceColors.Divider\n    val outline=RoundedCornerShape(27.dp)').replace('TraceColors.Divider.copy(alpha=.75f)','divider.copy(alpha=.75f)');p.write_text(s)
# Remove the former solid selection implementation, including its coral marker.
p=d/'NoGlassKit.kt';s=p.read_text();s=s[:s.index('/** Solid dock;')];p.write_text(s)
p=u/'SaeonApp.kt'
edit(p,'    val view=LocalView.current','    val view=LocalView.current\n    val navBackdrop=rememberGraphicsLayer()\n    var sourceOrigin by remember { mutableStateOf(Offset.Zero) }')
# Record only the content underneath the dock. System insets and the ambient stay unchanged.
edit(p,'                Box(Modifier.fillMaxSize()) {\n                    SharedTransitionLayout','                Box(Modifier.fillMaxSize().onGloballyPositioned { val o=it.positionInRoot();if(sourceOrigin!=o)sourceOrigin=o }.drawWithContent {\n                    if(route in roots && !preferences.reducedTransparency){\n                        navBackdrop.record { this@drawWithContent.drawContent() };drawLayer(navBackdrop)\n                    } else drawContent()\n                }) {\n                    SharedTransitionLayout')
edit(p,'BankDock(tabs,if(stage.unlocked&&route=="more")"presenter"else route,Modifier.align(Alignment.BottomCenter).padding(horizontal=18.dp,vertical=8.dp))','LiquidNavigation(tabs,if(stage.unlocked&&route=="more")"presenter"else route,navBackdrop,sourceOrigin,Modifier.align(Alignment.BottomCenter).padding(horizontal=18.dp,vertical=8.dp),reducedTransparency=preferences.reducedTransparency)')
# Brand changes apply to the app, not to the fictional bank account, route or receipt issuer.
for p in [u/'screens/NoGlassHome.kt',d/'TaskPage.kt',d/'StudioLaunch.kt',u/'SaeonApp.kt']:
 s=p.read_text().replace('TaskPage("새온은행","home"','TaskPage("TRACE","home"').replace('title=="새온은행"','title=="TRACE"').replace('Text("새온은행",','Text("TRACE",').replace('Page(title = "새온은행"','Page(title = "TRACE"').replace('contentDescription="새온은행 시작"','contentDescription="TRACE 시작"')
 p.write_text(s)
res=r/'app/src/main/res'
for p in res.glob('values*/strings.xml'):
 s=p.read_text();import re
 s=re.sub(r'(<string name="app_name">)[^<]*(</string>)',r'\1TRACE\2',s);p.write_text(s)
p=r/'app/src/main/AndroidManifest.xml';s=p.read_text().replace('android:label="새온은행"','android:label="TRACE"');p.write_text(s)
p=u/'screens/SettingsScreens.kt';s=p.read_text().replace('5.0.0 · Stage Motion','11.0.0 · TRACE').replace('5.0.0-stage-demo','11.0.0');p.write_text(s)
# Restore the accessibility material override, only for the navigation layer.
p=u/'screens/RefinedScreens.kt';s=p.read_text();a='OptionRow("움직임 줄이기",pref.reducedMotion,"이동과 확대 효과를 줄입니다."){model.preference{motion(it)}}'
if a in s:s=s.replace(a,a+'\n        OptionRow("하단 바 투명 효과 줄이기",pref.reducedTransparency,"내비게이션을 불투명하게 표시합니다."){model.preference{transparency(it)}}')
p.write_text(s)
# Late authorization callbacks may never authorize a newer transaction.
p=u/'BankViewModel.kt';edit(p,'    fun cancelAuthorization() {','    fun cancelAuthorization() {\n        pendingCredentialNonce=null\n        pendingBiometricNonce=null')
p=r/'app/build.gradle.kts';s=p.read_text().replace('versionCode = 100','versionCode = 110').replace('10.0.0-noglass','11.0.0');p.write_text(s)
# Tests retain every failure. Device screenshots are native; no HTML render is substituted.
p=r/'app/src/androidTest/kotlin/app/saeon/trace/UiHarness.kt'
s=p.read_text().replace('compose.waitForIdle(); Thread.sleep(380)','compose.waitForIdle(); Thread.sleep(260)');p.write_text(s)
print('TRACE 11: approved non-glass content, transparent navigation lens, no coral marker, unchanged transaction engine.')
