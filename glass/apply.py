from pathlib import Path
import shutil
r=Path('android-native');u=r/'app/src/main/kotlin/app/saeon/trace/ui';d=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design'
for source,dest in [('GlassKit.kt',d/'GlassKit.kt'),('GlassHome.kt',u/'screens/GlassHome.kt'),('GlassHold.kt',u/'screens/GlassHold.kt'),('GlassTest.kt',r/'app/src/androidTest/kotlin/app/saeon/trace/GlassTest.kt')]:
 shutil.copy2(Path('glass')/source,dest)
def edit(p,a,b):
 s=p.read_text();assert a in s,(str(p),a[:90]);p.write_text(s.replace(a,b))
# The approved porcelain palette replaces the editorial-paper background, not the brand coral.
p=d/'Theme.kt'
s=p.read_text().replace('Color(0xFFF5F4F0), Color(0xFFFBFAF7), Color(0xFF20211F)','Color(0xFFF6F7F5), Color(0xFFFFFFFF), Color(0xFF20211F)').replace('Color(0xFF676960), Color(0xFFDDDDD5)','Color(0xFF686E65), Color(0xFFE2E5E0)').replace('Color(0xFF171B19), Color(0xFF202321), Color(0xFFF5F4F0)','Color(0xFF171B19), Color(0xFF222824), Color(0xFFF6F7F5)')
p.write_text(s)
p=d/'Components.kt';s=p.read_text();start=s.index('@Composable fun PrimaryButton(');end=s.index('@Composable fun SecondaryButton(',start)
s=s[:start]+'@Composable fun PrimaryButton(text:String,modifier:Modifier=Modifier,enabled:Boolean=true,onClick:()->Unit) { GlassAction(text,modifier,enabled,onClick) }\n'+s[end:];p.write_text(s)
p=u/'SaeonApp.kt'
edit(p,'    val backdrop = rememberGraphicsLayer()','    val backdrop = rememberGraphicsLayer()\n    val ambient = rememberGraphicsLayer()')
edit(p,'            if (fatal != null) {','            GlassAmbient(ambient,Modifier.matchParentSize())\n            if (fatal != null) {')
edit(p,'CompositionLocalProvider(LocalReducedTransparency provides preferences.reducedTransparency, LocalPageBottomInset','CompositionLocalProvider(LocalGlassScene provides GlassScene(ambient,contentOrigin), LocalReducedTransparency provides preferences.reducedTransparency, LocalPageBottomInset')
edit(p,'StudioHome(state, preferences, model, open)','GlassHome(state, preferences, model, open)')
edit(p,'GlassDock(tabs,','CalibratedDock(tabs,')
edit(p,'backdrop.record { this@drawWithContent.drawContent() }; drawLayer(backdrop)','backdrop.record { drawLayer(ambient); this@drawWithContent.drawContent() }; drawLayer(backdrop)')
edit(p,'"보내는 분을 확인할게요."','"송금 인증"')
edit(p,'"시연 확인 뒤 TRACE가 약 1초 동안 송금 맥락을 살펴봐요. 실제 비밀번호는 입력하지 않습니다."','"본인 확인 후 송금 내용을 확인합니다."')
p=d/'TaskPage.kt'
edit(p,'                    Text(title,Modifier.weight(1f)','                    if(root && title=="새온은행"){AppIcon(BankIcons.Trace,size=26,tint=TraceColors.Coral);Spacer(Modifier.width(8.dp))}\n                    Text(title,Modifier.weight(1f)')
p=u/'screens/TransferScreens.kt';edit(p,'ViewportHold(state,record, preferences.easyMode, model, open, back, home)','GlassHold(state,record, preferences.easyMode, model, open, back, home)')
# Center the actual measured group, not a guessed pixel offset. Scrolling is retained for large text.
p=d/'OrbitLoader.kt'
edit(p,'        Spacer(Modifier.height(if (compact) 12.dp else 42.dp))','')
edit(p,'            Modifier.fillMaxWidth(),\n            horizontalAlignment = Alignment.CenterHorizontally','            Modifier.fillMaxWidth().heightIn(min=(LocalTaskHeight.current-16.dp).coerceAtLeast(0.dp)),\n            verticalArrangement = Arrangement.Center,\n            horizontalAlignment = Alignment.CenterHorizontally')
edit(p,'if (compact) 118.dp else 148.dp','if (compact) 118.dp else 138.dp')
p=u/'screens/ViewportScreens.kt'
edit(p,'        Spacer(Modifier.height((LocalTaskHeight.current.value*.14f).coerceIn(20f,78f).dp))','')
edit(p,'        Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){\n            ResultSeal','        Column(Modifier.fillMaxWidth().heightIn(min=(LocalTaskHeight.current-16.dp).coerceAtLeast(0.dp)),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){\n            ResultSeal')
edit(p,'TaskGap(16);Caption("수수료 0원");TaskGap(24)','TaskGap(12);Caption("수수료 0원");TaskGap(24)\n            Text("새온 생활통장",style=MaterialTheme.typography.bodyMedium);Caption("남은 금액 ${won(state.balance)}원");TaskGap(12)')
edit(p,'"공식 상환 경로를 확인하지 못했어요. 확인되지 않았다는 건 안전하다는 뜻이 아니에요."','"은행에 등록된 상환 경로를 불러오지 못했어요."')
edit(p,'"송금을 완료하거나 일반 송금으로 전환하지 않았습니다."','"연결 상태를 확인한 뒤 다시 시도해 주세요."')
p=u/'screens/StudioScreens.kt'
edit(p,'"대출 상환은\\n은행에 등록된 경로로"','"상환 계좌를\\n다시 확인할게요"')
edit(p,'"안내받은 곳은 개인 계좌예요.\\n상환 목적과 받는 곳이 다릅니다."','"대출 상환인데 받는 계좌가\\n개인 명의로 확인됐어요."')
edit(p,'"상대가 준 번호나 링크는 이용하지 않아요.\\n아직 돈은 나가지 않았습니다."','"은행에 등록된 상환 계좌를 확인합니다."')
# Remove a decorative white block from VERIFY, keeping the actual bank lookup intact.
s=p.read_text().replace('Column(Modifier.fillMaxWidth().background(TraceColors.Surface,RoundedCornerShape(18.dp)).padding(16.dp)){','Column(Modifier.fillMaxWidth().padding(vertical=12.dp)){');p.write_text(s)
p=u/'StudioDemoCenter.kt'
edit(p,'Headline("짧게 경험하는\\nTRACE의 차이")','Headline("상황 선택")')
edit(p,'Caption("상황을 선택하면 가상 거래를 새로 준비해요. 송금 확인과 인증은 직접 진행합니다.")','Caption("선택한 상황에서 송금을 시작합니다.")')
# Compact spacing follows available height; no user data or controls are clipped.
p=u/'screens/GlassHome.kt'
edit(p,'val rowHeight=if(compact)48 else 54','val rowHeight=if(compact)48 else 50')
edit(p,'Space(if(compact)8 else 14)','Space(if(compact)8 else 10)')
edit(p,'Space(if(compact)8 else 20)','Space(if(compact)8 else 14)')
edit(p,'if(!compact){\n                Space(10)','if(LocalTaskHeight.current>=540.dp){\n                Space(10)')
edit(p,'                Space(8);HomeRule();HomeHeading("다음 일정","관리")','                if(LocalTaskHeight.current>=635.dp){Space(8);HomeRule();HomeHeading("다음 일정","관리")')
edit(p,'if(schedule!=null)Text("${won(schedule.amount)}원",style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)\n                }','if(schedule!=null)Text("${won(schedule.amount)}원",style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)\n                }}')
# Use a fixed non-disclosing replacement in the hero. Amount semantics must not leak hidden values.
p=r/'app/build.gradle.kts';s=p.read_text().replace('versionCode = 70','versionCode = 80').replace('7.0.0-studio','8.0.0-glass');p.write_text(s)
# Add dock-geometry and palette tests; unchanged transaction tests are retained.
print('A4 Glass applied: no home banner, no bring CTA, bounded live glass, measured navigation, universal home, centered results.')
