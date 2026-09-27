from pathlib import Path
import runpy
r=Path('android-native');u=r/'app/src/main/kotlin/app/saeon/trace/ui';d=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design'
p=u/'SaeonApp.kt';s=p.read_text()
a='BankAmbient(Modifier.matchParentSize())'
b='BankAmbient(Modifier.matchParentSize().drawWithContent { navBackdrop.record { this@drawWithContent.drawContent() };drawLayer(navBackdrop) })'
assert a in s;s=s.replace(a,b)
a='''                Box(Modifier.fillMaxSize().onGloballyPositioned { val o=it.positionInRoot();if(sourceOrigin!=o)sourceOrigin=o }.drawWithContent {
                    if(route in roots && !preferences.reducedTransparency){
                        navBackdrop.record { this@drawWithContent.drawContent() };drawLayer(navBackdrop)
                    } else drawContent()
                }) {'''
assert a in s;s=s.replace(a,'                Box(Modifier.fillMaxSize()) {')
s=s.replace('navBackdrop,sourceOrigin,Modifier.align','navBackdrop,Offset.Zero,Modifier.align').replace('    var sourceOrigin by remember { mutableStateOf(Offset.Zero) }\n','')
p.write_text(s)
p=d/'LiquidNavigation.kt';s=p.read_text().replace('The unmodified content layer is recorded before the dock; recursive self-sampling is impossible.','The reserved navigation background is recorded independently; the banking content stays uncached.').replace('// A subtle magnified second sample under the lens gives moving background parallax.','// A subtle magnified background sample under the moving lens, with no text distortion.')
p.write_text(s)
p=u/'screens/SettingsScreens.kt';s=p.read_text().replace('TraceSignature("새온은행 × TRACE");TaskGap(24);Headline("송금 앞의 맥락을\\n연결합니다.")','TraceSignature("TRACE");TaskGap(24);Headline("송금 전 맥락 확인")');p.write_text(s)
runpy.run_path('polish11/stable_material.py',run_name='__main__')
