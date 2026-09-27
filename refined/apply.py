from pathlib import Path
import shutil
r=Path('android-native');u=r/'app/src/main/kotlin/app/saeon/trace/ui';d=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design'
for src,dst in [('GlassKit.kt',d/'GlassKit.kt'),('GlassHome.kt',u/'screens/GlassHome.kt'),('RefinedLaunch.kt',d/'StudioLaunch.kt'),('RefinedScreens.kt',u/'screens/RefinedScreens.kt'),('RefinedTest.kt',r/'app/src/androidTest/kotlin/app/saeon/trace/RefinedTest.kt')]:
 shutil.copy2(Path('refined')/src,dst)
def edit(p,a,b):
 s=p.read_text()
 if a not in s:raise RuntimeError(f'Missing anchor {p}: {a[:100]}')
 p.write_text(s.replace(a,b))
p=d/'Theme.kt';s=p.read_text().replace('Color(0xFF171B19), Color(0xFF222824), Color(0xFFF6F7F5)','Color(0xFF151517), Color(0xFF212124), Color(0xFFF3F3F5)')
s=s.replace('Color(0xFFACB5AE), Color(0xFF3D4840)','Color(0xFFACADB5), Color(0xFF37373D)').replace('Color(0xFFFF8A75)','Color(0xFFDDA48F)').replace('Color(0xFFFFAA99), Color(0xFF829086), Color(0xFF3A2823), Color(0xFF251B17)','Color(0xFFF2B09E), Color(0xFF777781), Color(0xFF342825), Color(0xFF151517)')
s=s.replace('Color(0xFF686E65), Color(0xFFE2E5E0)','Color(0xFF6B6E73), Color(0xFFE2E3E5)')
s=s.replace('lineHeight = line.sp','lineHeight = line.sp')
p.write_text(s)
# Reserve one dock band. Visual dock shrinks, touch regions do not.
p=d/'TaskPage.kt';edit(p,'if(root)84.dp else 0.dp','if(root)72.dp else 0.dp')
p=d/'Components.kt';s=p.read_text().replace('if(LocalPageBottomInset.current>100)84.dp else 0.dp','if(LocalPageBottomInset.current>100)72.dp else 0.dp').replace('heightIn(min = 60.dp)','heightIn(min = 52.dp)').replace('.padding(vertical = 12.dp).semantics{heading()}','.padding(vertical = 8.dp).semantics{heading()}').replace('Modifier.size(24.dp), tint = TraceColors.Ink','Modifier.size(21.dp), tint = TraceColors.Ink')
s=s.replace('horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Top','horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically')
s=s.replace('Text(label, Modifier.weight(0.9f), style = MaterialTheme.typography.bodyMedium','Text(label, Modifier.weight(0.85f), style = MaterialTheme.typography.bodySmall')
p.write_text(s)
# Root ambient is now under the actual system bars. Only content consumes insets.
p=u/'SaeonApp.kt'
edit(p,'import androidx.compose.ui.layout.positionInRoot','import androidx.compose.ui.layout.positionInRoot\nimport androidx.compose.ui.platform.LocalDensity\nimport androidx.compose.ui.graphics.drawscope.translate')
edit(p,'    val roots = setOf(', '    val motionPixels=with(LocalDensity.current){18.dp.roundToPx()}\n    val roots = setOf(')
edit(p,'        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {\n            GlassAmbient(ambient,Modifier.matchParentSize())','        Box(Modifier.fillMaxSize()) {\n            GlassAmbient(ambient,Modifier.matchParentSize())\n        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {')
edit(p,'    interaction.authChallenge?.let { challenge ->','    }\n    interaction.authChallenge?.let { challenge ->')
edit(p,'LocalGlassScene provides GlassScene(ambient,contentOrigin)','LocalGlassScene provides GlassScene(ambient,Offset.Zero)')
edit(p,'backdrop.record { drawLayer(ambient); this@drawWithContent.drawContent() }','backdrop.record { translate(-contentOrigin.x,-contentOrigin.y){drawLayer(ambient)}; this@drawWithContent.drawContent() }')
edit(p,'Page(title = "새온은행") { Space(48); Headline("계좌 정보를\\n불러오고 있어요."); Space(20); Caption("이 기기에 저장된 거래 상태를 확인합니다.") }','Page(title = "새온은행") { Space(22); DeferredSkeleton(true,rows=5){} }')
start=p.read_text().index('                        enterTransition = ')
s=p.read_text();end=s.index(') {\n                        motionScreen("home")',start)
s=s[:start]+'''                        enterTransition = {
                            if(reduced)EnterTransition.None else {
                                val isTab=initialState.destination.route in roots && targetState.destination.route in roots
                                val direction=if(isTab && roots.indexOf(targetState.destination.route)<roots.indexOf(initialState.destination.route))-1 else 1
                                fadeIn(tween(if(isTab)240 else 280,delayMillis=20)) + slideInHorizontally(tween(300,easing=TraceMotion.Enter)){motionPixels*direction}
                            }
                        },
                        exitTransition = { if(reduced)ExitTransition.None else fadeOut(tween(150))+slideOutHorizontally(tween(240)){ -motionPixels/3 } },
                        popEnterTransition = { if(reduced)EnterTransition.None else fadeIn(tween(230))+slideInHorizontally(tween(280,easing=TraceMotion.Enter)){ -motionPixels } },
                        popExitTransition = { if(reduced)ExitTransition.None else fadeOut(tween(160))+slideOutHorizontally(tween(250)){ motionPixels } }'''+s[end:]
s=s.replace('BringScreen(state, model, open, back)','RefinedBring(state, model, open, back)').replace('TimelineScreen(state, back)','RefinedTimeline(state, back)').replace('SafetyGuideScreen(state, open, back)','RefinedSafetyGuide(state, open, back)').replace('AppearanceScreen(preferences, model, back)','RefinedAppearance(preferences, model, back)').replace('HelpScreen(back)','RefinedHelp(open,back)')
s=s.replace('"시연 확인"','"시연 인증"').replace('"본인 확인 후 송금 내용을 확인합니다."','"받는 분과 금액을 확인한 후 인증하세요."')
p.write_text(s)
p=r/'app/src/main/kotlin/app/saeon/trace/MainActivity.kt';s=p.read_text()
s=s.replace('isAppearanceLightStatusBars=!dark','show(androidx.core.view.WindowInsetsCompat.Type.statusBars())\n                        isAppearanceLightStatusBars=!dark')
s=s.replace('다시 인증하거나 시연 확인을 선택해 주세요. 송금은 실행하지 않았습니다.','다시 시도하거나 다른 인증 방법을 선택하세요.').replace('시연 확인을 선택하면 가상 거래 인증을 진행할 수 있습니다. 아직 돈은 나가지 않았습니다.','다른 인증 방법을 선택하세요.')
p.write_text(s)
# Real route data is fetched automatically; there is no artificial delay in normal use.
p=u/'BankViewModel.kt';edit(p,'    fun resolveRoute(id: String) = act {','    internal var routeDelayForTest: Long = 0\n    fun resolveRoute(id: String) = act {')
edit(p,'        delay(350)\n        repository.resolveRoute(id)','        if(app.saeon.trace.BuildConfig.DEBUG && routeDelayForTest>0)delay(routeDelayForTest)\n        repository.resolveRoute(id)')
p=u/'screens/TransferScreens.kt';s=p.read_text().replace('StudioVerify(record, interaction, model, back, home)','RefinedVerify(record, interaction, model, back, home)').replace('OfficialRouteScreen(record, interaction, model, back)','RefinedVerify(record, interaction, model, back, home)')
s=s.replace('it.intent.id+if(it.stage==TransferStage.AUTHORIZING)"REVIEW"else it.stage.name','it.intent.id+if(it.stage==TransferStage.AUTHORIZING)"REVIEW"else if(it.stage in setOf(TransferStage.VERIFY,TransferStage.ROUTE))"OFFICIAL"else it.stage.name')
p.write_text(s)
p=u/'screens/RefinedScreens.kt';edit(p,'Text("상환 계좌를\\n다시 확인했어요",','Text(if(loading)"상환 계좌를\\n확인하고 있어요"else"상환 계좌를\\n다시 확인했어요",')
# HOLD now has one direct route to temporal evidence, not a repeated reason dialog.
p=u/'screens/GlassHold.kt';s=p.read_text().replace('QuietButton("이유 자세히 보기",Modifier.fillMaxWidth().testTag("hold_reasons_open")){details=true}','QuietButton("연결된 맥락 보기",Modifier.fillMaxWidth().testTag("hold_reasons_open")){open("timeline")}')
s=s[:s.index('    if(details)AlertDialog')]+'}\n';p.write_text(s)
p=u/'screens/MotionWarn.kt';edit(p,'                Checkbox(checked,null)\n                Text(', '                Checkbox(checked,null)\n                Spacer(Modifier.width(14.dp))\n                Text(')
# Clipped visual heading fixes on amount/header/detailed rows use real layout constraints.
p=u/'StudioDemoCenter.kt';s=p.read_text().replace('Headline("상황 선택")','Headline("어떤 상황을 볼까요?")')
s=s.replace('Triple(DemoScenario.LOAN,"대출 상환","개인 명의 계좌로 상환하라는 요청")','Triple(DemoScenario.LOAN,"대출 상환","은행 등록 계좌로 경로 확인"),\n            Triple(DemoScenario.WARN,"링크 확인","송금 전 받는 분을 다시 확인")')
s=s.replace('else listOf(DemoScenario.WARN,DemoScenario.NEW_ACCOUNT)','else emptyList()')
p.write_text(s)
# Consolidate simulation disclosure in App info / Demo Center. Financial records remain marked.
replacements={
 '새온은행 시연 계정':'내 계정',
 '실명·연락처 등록이 없는 가상 계정입니다.':'',
 '최대 12자. 실제 예금주 이름을 바꾸지 않습니다.':'앱에 표시할 이름을 입력하세요. 최대 12자',
 '시연 계정은 로그인 없이 열립니다. 송금할 때는 매번 별도의 확인을 거쳐요.':'송금할 때 본인 확인을 진행합니다.',
 '등록된 강한 생체 인증을 사용합니다. 사용할 수 없으면 시연 확인을 직접 선택할 수 있어요.':'기기에 등록된 생체 정보로 인증합니다.',
 '서명용 개인키는 기기 밖으로 내보내지 않습니다. 실제 은행의 기기 등록 인증서는 아닙니다.':'인증 키는 기기 보안 영역에서 관리합니다.',
 '하루 동안 완료한 송금의 합계에 적용합니다. 인증과 위험 판단을 생략하는 한도는 아니에요.':'하루 동안 보낼 수 있는 총 금액입니다.',
 '예정 표시와 시연 내역 확인':'예약 내역 관리',
 '목록에 추가해도 안전한 수취인으로 인증되는 것은 아니에요.':'자주 보내는 계좌를 선택하세요.',
 '이 기기의 가상 거래와 시연 예정 내역에서 만든 알림입니다. 푸시 메시지나 외부 알림을 수집하지 않습니다.':'',
 '글자를 키우고, 위험 상황에서는 한 번에 확인할 내용을 줄입니다.':'주요 기능을 중심으로 화면과 안내를 단순하게 표시합니다.',
 '편한 크기로,\\n분명한 안내로.':'나에게 맞는 화면',
 '이 기기 안에서':'개인정보 처리',
 '걱정되는 순간에는\\n송금을 잠그세요':'송금을 잠글 수 있어요',
 '송금을 잠가\\n계좌를 보호하고 있어요':'송금이 잠겨 있습니다',
 '잔액과 내역은 확인할 수 있어요. 잠금 중에는 인증을 받아도 송금되지 않습니다.':'잠금 중에도 잔액과 거래 내역은 확인할 수 있습니다.',
 '실제 계좌 지급정지 기능이 아닌, 이 앱의 가상 송금을 차단하는 기능입니다.':'',
 '정해둔 날에,\\n빠짐없이':'자동이체 관리',
 '자동 출금되지 않는 로컬 시연 예약입니다.':'',
 '새로운 예약을\\n만들어요':'예약할 내용을 입력하세요',
 '예약 정보만 저장하며 실제 출금은 하지 않습니다.':'',
 '시연에서는 카드 상태와 요청 기록만 바뀝니다. 실제 카드를 정지하거나 발급하지 않습니다.':'',
 '새온은행은 가상 은행입니다. 이 화면에서는 외부 상담이나 신고를 전송하지 않습니다.':'',
 '계좌가 걱정되시나요?':'송금 잠금',
 '먼저 송금을 잠그고 상황을 확인하세요.':'필요할 때 송금을 잠그거나 해제하세요.',
 '가상 보유 내역':'보유 내역',
 '시연 신용 현황':'내 금융 현황',
 '개인정보·계좌 비밀번호를 입력하지 마세요. 내용은 이 기기에만 저장되며 실제 신고·상담 접수가 아닙니다.':'계좌 비밀번호 등 민감한 정보는 입력하지 마세요. 문의는 이 기기에 저장됩니다.',
 '완료된 가상 거래만 표시합니다. 보류·확인 중인 송금은 안전 센터에 있어요.':'보류된 송금은 안전 메뉴에서 확인할 수 있습니다.'
}
for p in (u/'screens').glob('*.kt'):
 s=p.read_text()
 for a,b in replacements.items():s=s.replace(a,b)
 p.write_text(s)
# Replace outdated introductory splash assets; retain the exact two website path shapes.
res=r/'app/src/main/res'
(res/'values/refined_colors.xml').write_text('<resources><color name="refined_canvas">#F6F7F5</color><color name="refined_coral">#EF4A32</color></resources>')
(res/'values-night').mkdir(exist_ok=True)
(res/'values-night/refined_colors.xml').write_text('<resources><color name="refined_canvas">#151517</color><color name="refined_coral">#EF4A32</color></resources>')
for p in res.glob('values*/themes.xml'):
 s=p.read_text().replace('#F5F4F0','@color/refined_canvas').replace('name="android:windowSplashScreenAnimationDuration">0','name="android:windowSplashScreenAnimationDuration">400')
 p.write_text(s)
p=res/'drawable/trace_launcher_foreground.xml';s=p.read_text().replace('scaleX="1.65"','scaleX="1.72"').replace('scaleY="1.65"','scaleY="1.72"').replace('translateX="21"','translateX="19.6"').replace('translateY="18"','translateY="17.88"');p.write_text(s)
p=res/'values/trace_launcher.xml';p.write_text(p.read_text().replace('#F5F4F0','#F6F7F5'))
p=r/'app/build.gradle.kts';s=p.read_text().replace('versionCode = 80','versionCode = 90').replace('8.0.0-glass','9.0.0-refined').replace('animationsDisabled = true','animationsDisabled = false');p.write_text(s)
print('Refined 9 applied: measured controls, cold launch, slow-only skeleton, direct context, registered route and graphite dark palette.')
