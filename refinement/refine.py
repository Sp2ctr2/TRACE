#!/usr/bin/env python3
"""Refined presentation overlay, applied after the pinned V3 source overlays.
Isolated branch. No main website mutations. No payment-policy bypasses.
"""
from pathlib import Path
r=Path('android-native'); d=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design'; u=r/'app/src/main/kotlin/app/saeon/trace/ui'
def edit(p,a,b):
 s=p.read_text(); assert a in s,(p,a[:90]); p.write_text(s.replace(a,b))
edit(r/'app/build.gradle.kts','versionCode = 30','versionCode = 31')
edit(r/'app/build.gradle.kts','3.0.0-bank-demo','3.1.0-refined-demo')
edit(u/'BankViewModel.kt','delay(1000)','delay(1290)')
p=d/'BankExperience.kt'
s=p.read_text()
# Bank account is the sole strong-colored content surface. Glass stays in navigation.
a=s.index('@Composable fun AccountHero'); b=s.index('@Composable fun MiniMetric',a)
h=s[a:b].replace('Color(0xFF202723)','Color(0xFFD43D28)').replace('Color(0xFFE7EAE4)','Color.White').replace('Color(0xFFADB8AF)','Color(0xFFFFEAE5)').replace('Color(0xFFD9DDD7)','Color.White').replace('Color(0xFF39433C)','Color(0xFFB62E1C)').replace('containerColor=Color(0xFFEF4A32),contentColor=Color.White','containerColor=Color(0xFFFBFAF7),contentColor=Color(0xFF20211F)')
s=s[:a]+h+s[b:]
s=s.replace('tint.copy(alpha=if(dark).90f else .83f)','tint.copy(alpha=if(Build.VERSION.SDK_INT<31).97f else if(dark).77f else .69f)')
# Small screens and large font scale must size numbers using physical font scaling.
s=s.replace('val font=(maxWidth.value/(digits.length*.63f+1.8f)).coerceIn(23f,38f)','val scale=androidx.compose.ui.platform.LocalDensity.current.fontScale\n        val font=(maxWidth.value/(scale*(digits.length*.63f+1.8f))).coerceIn(16f,38f)')
a=s.index('@Composable fun ContextLoading')
s=s[:a]+r'''@Composable fun ContextLoading(cancel:()->Unit) {
    val reduced=LocalReducedMotion.current
    val sweep=remember{Animatable(0f)}
    var stage by remember{mutableIntStateOf(0)}
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(240);stage=1
        kotlinx.coroutines.delay(340);stage=2
        kotlinx.coroutines.delay(350);stage=3
    }
    LaunchedEffect(reduced){if(reduced)sweep.snapTo(1f)else sweep.animateTo(1f,tween(1100,easing=LinearEasing))}
    val coral=TraceColors.Coral;val line=TraceColors.Divider
    Box(Modifier.fillMaxSize().testTag("trace_evaluating").semantics{liveRegion=LiveRegionMode.Polite;contentDescription="TRACE 송금 맥락 확인 중"}) {
        Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(28.dp).offset(y=(-20).dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Box(Modifier.size(84.dp),contentAlignment=Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(line,style=Stroke(2.dp.toPx()))
                    if(!reduced)drawArc(coral,-90f,360f*sweep.value,false,style=Stroke(2.5.dp.toPx(),cap=StrokeCap.Round))
                }
                Icon(BankIcons.Trace,null,Modifier.size(42.dp),tint=coral)
            }
            Space(26)
            Text("송금 앞의 맥락을\n연결하고 있어요",style=MaterialTheme.typography.headlineSmall,textAlign=TextAlign.Center,modifier=Modifier.semantics{heading()})
            Space(28)
            listOf("요청의 목적과 위험 신호","시간과 수취인의 연결","송금 전 안전 확인").forEachIndexed { index,label ->
                Row(Modifier.widthIn(max=280.dp).fillMaxWidth().padding(vertical=9.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,color=if(stage>index)TraceColors.Ink else TraceColors.Muted)
                    Icon(if(stage>index)BankIcons.Check else BankIcons.More,null,Modifier.size(18.dp),tint=if(stage>index)coral else line)
                }
            }
            Space(16);Caption("아직 돈은 이동하지 않았어요.")
        }
        SecondaryButton("송금 취소",Modifier.align(Alignment.BottomCenter).padding(24.dp),onClick=cancel)
    }
}
'''
p.write_text(s)
edit(u/'screens/BankingScreens.kt','Page(title="새온은행",tag="home"','Page(title="TRACE",tag="home"')
p=u/'screens/TransferScreens.kt';s=p.read_text();a=s.index('@Composable private fun CompleteScreen')
s=s[:a]+r'''@Composable private fun CompleteScreen(state: BankState, record: TransferRecord, open: (String)->Unit, home:()->Unit) {
    val receipt=state.receipts.find{it.intentId==record.intent.id&&!it.seed}
    if(receipt==null){Page(title="거래 결과 확인",footer={PrimaryButton("거래 내역 확인"){open("history")}}){EmptyState("내역을 다시 확인해 주세요.","다시 송금하지 말고 완료 내역을 확인하세요.")};return}
    Page(tag="transfer_complete",footer={PrimaryButton("확인",Modifier.testTag("complete_confirm"),onClick=home)}) {
        Column(Modifier.fillMaxWidth().padding(top=48.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            ResultSeal(diameter=78);Space(30)
            Text("${receipt.recipient.name}님에게",style=MaterialTheme.typography.titleMedium,color=TraceColors.Muted)
            Space(8)
            Text("${won(receipt.amount)}원",style=MaterialTheme.typography.headlineLarge,modifier=Modifier.semantics{heading()})
            Space(5);Text(if(record.intent.purpose==Purpose.LOAN)"상환했어요"else"보냈어요",style=MaterialTheme.typography.headlineSmall)
            Space(18);Caption("수수료 0원")
        }
        Space(34)
        SurfaceBox {
            DetailRow("받는 계좌","${receipt.recipient.bank} ${receipt.recipient.account}")
            DetailRow("출금 계좌",receipt.fromAccount)
        }
        Space(8);QuietButton("송금 내역 보기",Modifier.fillMaxWidth().testTag("complete_receipt")){open("receipt/${receipt.id}")}
        Space(12);Caption("실제 자금 이동이 없는 시연 기록입니다.")
    }
}
''';p.write_text(s)
# Correct the test fixture, rather than changing the policy to satisfy the test:
# createReview only changes a draft; fresh() previously left NORMAL's empty context.
p=r/'app/src/androidTest/kotlin/app/saeon/trace/RedesignTest.kt'
edit(p,'fresh();runBlocking{graph.preferences.motion(true)};createReview(DemoScenario.IMPERSONATION)','fresh(DemoScenario.IMPERSONATION);runBlocking{graph.preferences.motion(true)};createReview(DemoScenario.IMPERSONATION)')
edit(p,'duration>=900','duration>=1200')
# wm overrides alter emulator output dimensions independently of rendering. Force
# activity recreation so the responsive pass actually receives the new density.
edit(p,'Thread.sleep(900);navigate("home")','Thread.sleep(1200);compose.activityRule.scenario.recreate();awaitReady();navigate("home")')
# Caption describes the fixture accurately: expiration is not semantic education inference.
p=r/'core/src/main/kotlin/app/saeon/trace/core/Fixtures.kt'
edit(p,'위험한 단어가 있어도 종료된 교육 내용을 현재 지시로 취급하지 않아요.','만료된 교육 예시 신호는 현재 송금에 적용하지 않아요. 이 사례는 신호 만료를 재현합니다.')
print('Refined native source applied: coral account, floating glass, 1290 ms gate, focused receipt.')
