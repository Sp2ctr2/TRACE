#!/usr/bin/env python3
"""Viewport-first revision. Run after the committed v3.1 overlays. No engine/policy changes."""
from pathlib import Path
import re, json, shutil, hashlib, xml.etree.ElementTree as ET
r=Path('android-native'); d=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design'; u=r/'app/src/main/kotlin/app/saeon/trace/ui'
def edit(p,a,b):
 s=p.read_text();assert a in s,(str(p),a[:100]);p.write_text(s.replace(a,b))
for name,dest in [('TaskPage.kt',d),('ViewportScreens.kt',u/'screens')]:shutil.copy2(Path('viewport')/name,dest/name)
edit(r/'app/build.gradle.kts','versionCode = 31','versionCode = 40')
edit(r/'app/build.gradle.kts','3.1.0-refined-demo','4.0.0-viewport-demo')
p=u/'SaeonApp.kt'
for a,b in [('HomeScreen(state, preferences, model, open)','ViewportHome(state, preferences, model, open)'),('RecipientScreen(state, model, open)','ViewportRecipient(state, model, open)'),('AmountScreen(state, model, open, back)','ViewportAmount(state, model, open, back)')]:edit(p,a,b)
edit(p,'composable("recipient_entry")', 'composable("recipients_all") { RecipientScreen(state, model, open) }\n                        composable("comparison") { ComparisonScreen(model, open, back) }\n                        composable("recipient_entry")')
p=u/'screens/TransferScreens.kt'
for name in ['Review','Hold','Warn','Verify','Unknown','Complete']:
 edit(p,'-> '+name+'Screen(', '-> Viewport'+name+'(')
edit(p,'Page(title = "확인된 상환 경로", tag = "trace_official_route",', 'TaskPage(title = "확인된 상환 경로", tag = "trace_official_route",')
s=p.read_text();a=s.index('@Composable private fun OfficialRouteScreen');b=s.index('@Composable private fun UnknownScreen',a)
part=s[a:b].replace('Space(', 'TaskGap(')
part=part.replace('TaskGap(12); TraceSignature(); TaskGap(24);','TaskGap(8);')
part=part.replace('Headline("보내는 목적에 맞는\\n받는 곳을 찾았어요.")','TaskHeadline("목적에 맞는\\n받는 곳을 찾았어요", "상환 경로를 확인했어요")')
part=part.replace('TaskGap(18); Rule(); TaskGap(24)','TaskGap(12)')
part=part.replace('TaskGap(20); Caption("새온은행 Demo Gateway의 등록된 가상 응답입니다. 실제 은행 조회가 아닙니다.")','')
s=s[:a]+part+s[b:];p.write_text(s)
p=u/'screens/ViewportScreens.kt'
edit(p,'val draft=state.draft?:return', '''val draft=state.draft
    if(draft==null) {
        TaskPage("송금","transfer_amount",back=back,footer={PrimaryButton("받는 분 선택"){open("transfer")}}) {
            TaskHeadline("받는 분을 먼저 선택해 주세요.")
        }
        return
    }''')
p=d/'Theme.kt';edit(p,'Color(0xFFD03D28)','Color(0xFFD93B25)');edit(p,'Color(0xFF202622)','Color(0xFF202321)')
edit(p,'Color(0xFF3D4840), Color(0xFFFF8A75), Color(0xFFFF8A75)','Color(0xFF3D4840), Color(0xFFEF4A32), Color(0xFFFF8A75)')
p=d/'Components.kt'
edit(p,'min = 58.dp','min = 54.dp');edit(p,'RoundedCornerShape(20.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)','RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)')
edit(p,'copy(fontSize = 18.sp)','copy(fontSize = 17.sp)')
p=d/'BankExperience.kt';edit(p,'min=74.dp','min=64.dp');edit(p,'.height(62.dp)','.height(52.dp)');edit(p,'min=62.dp','min=52.dp');edit(p,'.padding(vertical=9.dp)','.padding(vertical=5.dp)')
p=u/'screens/SettingsScreens.kt'
edit(p,'Space(8);Headline("같은 송금,\\n다른 맥락.");Space(12)','Space(8);Headline("같은 송금,\\n다른 맥락.");Space(12)\n        MenuRow("같은 계좌·같은 금액 비교", "300만 원 정상 거래와 기관 사칭 요청", BankIcons.Trace, tag="comparison_open") { open("comparison") }')
source=Path('index.html').read_text()
m=re.search(r'<symbol\b[^>]*\bid="trace-mark"[^>]*>.*?</symbol>',source,re.S);assert m,'TRACE symbol missing'
element=ET.fromstring(m.group())
view=element.attrib.get('viewBox',element.attrib.get('viewbox'));assert view=='0 0 40 40',view
paths=[e.attrib['d'] for e in element if e.tag=='path'];assert len(paths)==2
assert all(e.tag=='path' and e.attrib.get('fill')=='currentColor' and e.attrib.get('stroke')=='none' for e in element)
svg='<svg xmlns="http://www.w3.org/2000/svg" viewBox="'+view+'" fill="currentColor">'+''.join('<path d="'+p+'"/>' for p in paths)+'</svg>'
a=Path('delivery');a.mkdir(exist_ok=True);(a/'TRACE-mark.svg').write_text(svg)
meta={'source':'Sp2ctr2/TRACE/index.html','symbol':'trace-mark','viewBox':view,'paths':paths,'line':source[:m.start()].count('\n')+1,'index_sha256':hashlib.sha256(source.encode()).hexdigest(),'svg_sha256':hashlib.sha256(svg.encode()).hexdigest()}
(a/'logo-source.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2))
p=d/'Icons.kt';s=p.read_text();start=s.index('    val Trace: ImageVector');end=s.index('.build()',start)+len('.build()')
code='    val Trace: ImageVector = ImageVector.Builder("TRACE", 40.dp, 40.dp, 40f, 40f)'+''.join('\n        .addPath(PathParser().parsePathString('+json.dumps(path)+').toNodes(), fill = SolidColor(Color.Black))' for path in paths)+'.build()'
p.write_text(s[:start]+code+s[end:])
assets=r/'trace-ui/src/main/assets';assets.mkdir(parents=True,exist_ok=True)
shutil.copy2(a/'TRACE-mark.svg',assets/'TRACE-mark.svg');shutil.copy2(a/'logo-source.json',assets/'logo-source.json')
if Path('viewport/ViewportTest.kt').exists():shutil.copy2('viewport/ViewportTest.kt',r/'app/src/androidTest/kotlin/app/saeon/trace/ViewportTest.kt')
print('Applied viewport UI; original logo paths:',paths)
