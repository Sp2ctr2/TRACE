#!/usr/bin/env python3
"""V3 visual rebuild; apply after apply_design.py and fixes.py on the pinned base."""
from pathlib import Path
import re
root=Path('android-native')
ui=root/'app/src/main/kotlin/app/saeon/trace/ui'
design=root/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design'
core=root/'core/src/main/kotlin/app/saeon/trace/core'
def edit(p,a,b):
 s=p.read_text();assert a in s,(str(p),a[:80]);p.write_text(s.replace(a,b))
def block(p,start,end,new):
 s=p.read_text();a=s.index(start);b=s.index(end,a);p.write_text(s[:a]+new+'\n\n'+s[b:])
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s)
edit(root/'app/build.gradle.kts','versionCode = 25','versionCode = 30')
edit(root/'app/build.gradle.kts','2.0.0-experience-demo','3.0.0-bank-demo')
# Keep system fonts; no redistributed font assets or remote rendering dependency.
edit(design/'Theme.kt','letterSpacing = 0.sp','letterSpacing = (-0.25).sp')
edit(design/'Theme.kt','Color(0xFFC93824)','Color(0xFFD03D28)')
edit(design/'Theme.kt','val LocalEasyMode =','val LocalPageBottomInset = staticCompositionLocalOf { 28 }\nval LocalEasyMode =')
edit(design/'Components.kt','bottom = 28.dp','bottom = LocalPageBottomInset.current.dp')
edit(design/'Components.kt','shape = RoundedCornerShape(12.dp), contentPadding','shape = RoundedCornerShape(20.dp), contentPadding')
edit(design/'Components.kt','modifier.fillMaxWidth().heightIn(min = 56.dp)','modifier.fillMaxWidth().heightIn(min = 58.dp)')
edit(design/'Components.kt','Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)','Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp), textAlign = TextAlign.Center)')
# Display components have no bank/scenario dependencies.
write(design/'BankExperience.kt',r'''package app.saeon.trace.ui.design

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.layer.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlin.math.*

@Composable fun PersonBadge(name: String, size: Int = 52, accent: Boolean = false) {
    Box(Modifier.size(size.dp).background(if(accent) TraceColors.CoralLight else TraceColors.Surface, CircleShape),contentAlignment=Alignment.Center) {
        Text(name.take(1),color=if(accent) TraceColors.CoralText else TraceColors.Ink,
            style=MaterialTheme.typography.titleMedium.copy(fontSize=(size*.36f).sp),fontWeight=FontWeight.SemiBold)
    }
}
@Composable fun AmountDisplay(value: Long, darkSurface: Boolean = false, hidden: Boolean = false) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val digits=won(value)
        val font=(maxWidth.value/(digits.length*.63f+1.8f)).coerceIn(23f,38f)
        Row(verticalAlignment=Alignment.Bottom) {
            Text(if(hidden) "잔액 숨김" else digits, color=if(darkSurface) Color(0xFFF5F4F0) else TraceColors.Ink,
                style=MaterialTheme.typography.displaySmall.copy(fontSize=font.sp,letterSpacing=(-1).sp,fontFeatureSettings="tnum"))
            if(!hidden) Text("원",Modifier.padding(start=4.dp,bottom=4.dp),color=if(darkSurface) Color(0xFFD9DDD7) else TraceColors.Ink,
                style=MaterialTheme.typography.titleMedium)
        }
    }
}
@Composable fun AccountHero(balance: Long, hidden: Boolean, onHide: () -> Unit, onSend: () -> Unit, onBring: () -> Unit, onDetail: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF202723),RoundedCornerShape(28.dp)).padding(22.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(role=Role.Button,onClick=onDetail).heightIn(min=48.dp)) {
                Text("새온 생활통장",color=Color(0xFFE7EAE4),style=MaterialTheme.typography.titleSmall)
                Text("110-***-0001",color=Color(0xFFADB8AF),style=MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick=onHide,modifier=Modifier.size(48.dp).semantics{contentDescription=if(hidden)"잔액 보이기" else "잔액 숨기기"}) {
                Icon(BankIcons.Eye,null,Modifier.size(21.dp),tint=Color(0xFFD9DDD7))
            }
        }
        Space(16)
        Box(Modifier.testTag("home_balance")) { AmountDisplay(balance,true,hidden) }
        Space(22)
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Button(onClick=onBring,modifier=Modifier.weight(1f).heightIn(min=52.dp).testTag("home_bring"),shape=RoundedCornerShape(17.dp),
                colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF39433C),contentColor=Color.White),contentPadding=PaddingValues(8.dp)) {
                Text("가져오기",style=MaterialTheme.typography.labelLarge)
            }
            Button(onClick=onSend,modifier=Modifier.weight(1.5f).heightIn(min=52.dp).testTag("home_transfer"),shape=RoundedCornerShape(17.dp),
                colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFEF4A32),contentColor=Color.White),contentPadding=PaddingValues(8.dp)) {
                Text("송금",style=MaterialTheme.typography.labelLarge.copy(fontSize=18.sp,fontWeight=FontWeight.Bold))
            }
        }
    }
}
@Composable fun MiniMetric(title:String, value:String, icon:ImageVector, modifier:Modifier=Modifier,onClick:()->Unit) {
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(TraceColors.Surface).clickable(role=Role.Button,onClick=onClick).padding(18.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Icon(icon,null,Modifier.size(19.dp),tint=TraceColors.Muted); Spacer(Modifier.weight(1f))
            Icon(BankIcons.Chevron,null,Modifier.size(14.dp),tint=TraceColors.Muted)
        }
        Space(12); Caption(title); Space(5)
        Text(value,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
    }
}

/** Android rendition of a floating glass navigation layer, not Apple's iOS API.
 * The source excludes this dock. Android 12+ blurs the recorded content behind it;
 * earlier devices retain a legible tinted/translucent material.
 */
@Composable fun GlassDock(tabs:List<Triple<String,String,ImageVector>>,route:String,backdrop:GraphicsLayer,contentOrigin:Offset,
    modifier:Modifier=Modifier,onSelect:(String)->Unit) {
    val reduced=LocalReducedMotion.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    val selectedIndex=tabs.indexOfFirst { it.first==route }.coerceAtLeast(0)
    val selected by animateFloatAsState(selectedIndex.toFloat(), if(reduced) snap() else spring(dampingRatio=.86f,stiffness=500f),label="glass-tab-position")
    val dark=TraceColors.Paper.luminance()<.3f
    val tint=if(dark) Color(0xFF29332C) else Color(0xFFF8F9F5)
    BoxWithConstraints(modifier.widthIn(max=560.dp).fillMaxWidth().heightIn(min=74.dp)
        .onGloballyPositioned { origin=it.positionInRoot()-contentOrigin }
        .shadow(14.dp,RoundedCornerShape(40.dp),ambientColor=Color.Black.copy(alpha=.09f),spotColor=Color.Black.copy(alpha=.12f))
        .clip(RoundedCornerShape(40.dp)).testTag("glass_dock")) {
        Canvas(Modifier.matchParentSize().graphicsLayer {
            if(Build.VERSION.SDK_INT>=31) renderEffect=BlurEffect(22.dp.toPx(),22.dp.toPx(),TileMode.Clamp)
        }) { translate(-origin.x,-origin.y) { drawLayer(backdrop) } }
        Box(Modifier.matchParentSize().background(tint.copy(alpha=if(dark).90f else .83f)))
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.White.copy(alpha=if(dark).08f else .36f),Color.Transparent))))
        val itemWidth=(maxWidth-12.dp)/tabs.size
        Box(Modifier.padding(6.dp).offset { IntOffset((itemWidth.toPx()*selected).roundToInt(),0) }
            .width(itemWidth).height(62.dp).background(if(dark)Color.White.copy(alpha=.13f)else Color.White.copy(alpha=.88f),RoundedCornerShape(32.dp))
            .border(.75.dp,if(dark)Color.White.copy(alpha=.15f)else Color.White,RoundedCornerShape(32.dp)))
        Row(Modifier.fillMaxWidth().padding(6.dp)) {
            tabs.forEach { (id,label,icon) ->
                Column(Modifier.weight(1f).heightIn(min=62.dp).clip(RoundedCornerShape(32.dp)).clickable(role=Role.Tab){onSelect(id)}
                    .testTag("nav_$id").semantics { this.selected=id==route;contentDescription=label }
                    .padding(vertical=9.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Icon(icon,null,Modifier.size(23.dp),tint=if(id==route)TraceColors.CoralText else TraceColors.Muted)
                    Text(label,style=MaterialTheme.typography.labelSmall.copy(fontSize=11.sp),fontWeight=if(id==route)FontWeight.Bold else FontWeight.Medium,
                        color=if(id==route)TraceColors.CoralText else TraceColors.Muted)
                }
            }
        }
        Box(Modifier.matchParentSize().border(.9.dp,Brush.verticalGradient(listOf(Color.White.copy(alpha=if(dark).23f else .95f),Color.Black.copy(alpha=.06f))),RoundedCornerShape(40.dp)))
    }
}

@Composable fun ResultSeal(kind:String="complete",size:Int=78) {
    val reduced=LocalReducedMotion.current
    val value=remember(kind){Animatable(if(reduced)1f else 0f)}
    LaunchedEffect(kind,reduced){if(reduced)value.snapTo(1f)else value.animateTo(1f,tween(420,easing=FastOutSlowInEasing))}
    val coral=TraceColors.Coral;val on=TraceColors.White;val pale=TraceColors.CoralLight;val ink=TraceColors.CoralText
    Box(Modifier.size(size.dp).graphicsLayer{scaleX=.88f+.12f*value.value;scaleY=scaleX},contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(if(kind=="complete")coral else pale)
            if(kind=="complete") {
                val points=listOf(Offset(size.width*.28f,size.height*.51f),Offset(size.width*.44f,size.height*.66f),Offset(size.width*.73f,size.height*.36f))
                val path=Path().apply { moveTo(points[0].x,points[0].y);lineTo(points[1].x,points[1].y);lineTo(points[2].x,points[2].y) }
                val measure=PathMeasure().apply { setPath(path,false) };val drawn=Path();measure.getSegment(0f,measure.length*value.value,drawn)
                drawPath(drawn,on,style=Stroke(4.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
            }
        }
        if(kind!="complete") Icon(if(kind=="hold")BankIcons.Pause else if(kind=="verify")BankIcons.Bank else BankIcons.Info,null,Modifier.size((size*.42f).dp),tint=ink)
    }
}

@Composable fun ContextLoading(cancel:()->Unit) {
    val reduced=LocalReducedMotion.current
    val sweep=remember{Animatable(0f)}
    LaunchedEffect(Unit){sweep.animateTo(1f,tween(1000,easing=LinearEasing))}
    val coral=TraceColors.Coral;val line=TraceColors.Divider
    Box(Modifier.fillMaxSize().testTag("trace_evaluating").semantics{liveRegion=LiveRegionMode.Polite;contentDescription="TRACE 송금 맥락 확인 중"}) {
        Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(28.dp).offset(y=(-28).dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Box(Modifier.size(90.dp),contentAlignment=Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(line,style=Stroke(2.dp.toPx()))
                    if(!reduced)drawArc(coral,-90f,360f*sweep.value,false,style=Stroke(2.5.dp.toPx(),cap=StrokeCap.Round))
                }
                Icon(BankIcons.Trace,null,Modifier.size(48.dp),tint=coral)
            }
            Space(28)
            Text("송금 맥락을\n확인하고 있어요",style=MaterialTheme.typography.headlineSmall,textAlign=TextAlign.Center,modifier=Modifier.semantics{heading()})
            Space(13); Text("요청의 목적과 받는 분을 함께 살펴봐요.",style=MaterialTheme.typography.bodyMedium,color=TraceColors.Muted,textAlign=TextAlign.Center)
        }
        SecondaryButton("송금 취소",Modifier.align(Alignment.BottomCenter).padding(24.dp),onClick=cancel)
    }
}
''')
# Twelve scenario families, including difficult benign cases. These are explicit
# injected demonstrations, not an estimate of classifier accuracy.
edit(core/'Models.kt','NEW_RECIPIENT("새로운 수취인"','''FAMILY_CLAIM("가족 사칭 정황", "가족이라고 소개했지만 기존에 알고 있던 연락 경로와 달랐어요."),
    REMOTE_ACCESS("원격 조작 정황", "기기 원격 조작이 송금 요청과 함께 나타났어요."),
    SECRECY("확인 차단 요구", "다른 사람에게 확인하지 못하도록 비밀을 요구했어요."),
    PROFIT_PROMISE("수익 보장 약속", "수익을 약속하며 지정 계좌로 돈을 보내라고 했어요."),
    NEW_RECIPIENT("새로운 수취인"''')
edit(core/'Models.kt','WARN("보내기 전 확인", "WARN"), EASY("쉬운 모드", "HOLD")','''WARN("중고거래 링크", "WARN"), EASY("큰 글자 보호", "HOLD"),
    FAMILY_FAKE("가족 사칭", "HOLD"), REMOTE("원격 조작", "HOLD"), INVESTMENT("투자금 추가 요구", "HOLD"),
    EDUCATION("피싱 예방 교육", "ALLOW"), NEW_ACCOUNT("정상 고액 송금", "ALLOW"), UNRELATED("관련 없는 링크", "ALLOW")''')
edit(core/'Policy.kt','val base = when {','''val base = when {
            RiskType.REMOTE_ACCESS in types && RiskType.FINANCIAL_INSTRUCTION in types -> PolicyDecision.HOLD
            RiskType.FAMILY_CLAIM in types && RiskType.FINANCIAL_INSTRUCTION in types &&
                (RiskType.URGENCY in types || RiskType.NEW_RECIPIENT in types) -> PolicyDecision.HOLD
            RiskType.PROFIT_PROMISE in types && RiskType.SECRECY in types && RiskType.FINANCIAL_INSTRUCTION in types -> PolicyDecision.HOLD''')
p=core/'Fixtures.kt'
edit(p,'2026-09-20T09:47:00','2026-09-25T09:41:00')
edit(p,'DemoScenario.NORMAL -> seoyeon','DemoScenario.NORMAL, DemoScenario.EDUCATION, DemoScenario.UNRELATED -> seoyeon')
edit(p,'DemoScenario.IMPERSONATION, DemoScenario.EASY, DemoScenario.WARN -> kim','DemoScenario.IMPERSONATION, DemoScenario.EASY, DemoScenario.WARN, DemoScenario.FAMILY_FAKE, DemoScenario.REMOTE, DemoScenario.INVESTMENT, DemoScenario.NEW_ACCOUNT -> kim')
edit(p,'DemoScenario.NORMAL -> 32_000','DemoScenario.NORMAL, DemoScenario.EDUCATION, DemoScenario.UNRELATED -> 32_000')
edit(p,'DemoScenario.WARN -> 120_000','DemoScenario.WARN -> 120_000\n        DemoScenario.FAMILY_FAKE -> 1_200_000\n        DemoScenario.REMOTE -> 2_000_000\n        DemoScenario.INVESTMENT -> 5_000_000\n        DemoScenario.NEW_ACCOUNT -> 3_000_000')
edit(p,'DemoScenario.NORMAL -> "어제 저녁값 32,000원 보내줄래?"','''DemoScenario.NORMAL -> "어제 저녁값 32,000원 보내줄래?"
        DemoScenario.FAMILY_FAKE -> "엄마 나 휴대폰 고장났어. 지금 이 계좌로 돈 좀 보내줘. 전화는 안 돼."
        DemoScenario.REMOTE -> "원격 지원은 계속 켜두세요. 안내하는 계좌로 돈을 옮겨야 복구됩니다."
        DemoScenario.INVESTMENT -> "수익을 출금하려면 지정 계좌로 보증금을 보내야 합니다. 다른 사람에게 말하지 마세요."
        DemoScenario.EDUCATION -> "예방 교육에서 본 예시: 검찰이라며 지금 안전계좌로 보내라는 요구를 조심하세요."
        DemoScenario.NEW_ACCOUNT -> "계약서와 받는 분을 직접 확인한 가구 구매 대금 300만 원을 보내요."
        DemoScenario.UNRELATED -> "과거 주문 링크는 다른 거래의 내용입니다. 지금은 친구에게 저녁값을 정산해요."''')
edit(p,'DemoScenario.NORMAL -> emptyList()','''DemoScenario.NORMAL, DemoScenario.NEW_ACCOUNT -> emptyList()
            DemoScenario.EDUCATION -> listOf(e(RiskType.IMPERSONATION, 90, "종료된 예방 교육의 인용문").copy(expiresAt=now-60_000),e(RiskType.FINANCIAL_INSTRUCTION,90,"교육에서 인용한 요구").copy(expiresAt=now-60_000))
            DemoScenario.UNRELATED -> listOf(e(RiskType.SUSPICIOUS_LINK,3,"다른 거래의 주문 링크").copy(recipientId="unrelated-merchant"))
            DemoScenario.FAMILY_FAKE -> listOf(e(RiskType.FAMILY_CLAIM,4,"새 번호로 가족이라고 소개"),e(RiskType.URGENCY,2,"전화는 안 된다며 지금 입금 요구"),e(RiskType.FINANCIAL_INSTRUCTION,1,"처음 보는 개인 계좌로 송금 요청"))
            DemoScenario.REMOTE -> listOf(e(RiskType.REMOTE_ACCESS,5,"원격 제어 연결 상태"),e(RiskType.SECRECY,3,"연결을 끊거나 다른 사람에게 묻지 말라고 안내"),e(RiskType.FINANCIAL_INSTRUCTION,1,"복구를 이유로 계좌 이체 요구"))
            DemoScenario.INVESTMENT -> listOf(e(RiskType.PROFIT_PROMISE,6,"높은 수익 출금을 약속"),e(RiskType.SECRECY,3,"비밀 유지를 요구"),e(RiskType.FINANCIAL_INSTRUCTION,1,"출금 전 보증금 입금 요구"))''')
with p.open('a') as f:f.write(r'''

fun scenarioNote(s: DemoScenario): String = when(s) {
    DemoScenario.NORMAL -> "알고 지낸 친구와의 정산은 조용하게 통과해요."
    DemoScenario.IMPERSONATION, DemoScenario.EASY -> "기관 사칭, 급한 요청, 새 계좌가 하나의 송금으로 이어져요."
    DemoScenario.LOAN -> "대출을 갚는 목적과 개인 수취 계좌가 맞지 않아요."
    DemoScenario.UNKNOWN -> "공식 경로를 조회하지 못하면 송금으로 넘어가지 않아요."
    DemoScenario.WARN -> "확인하지 않은 링크 뒤의 송금은 독립 경로로 다시 확인해요."
    DemoScenario.FAMILY_FAKE -> "새 번호의 가족 주장과 급한 송금 요청을 함께 봐요."
    DemoScenario.REMOTE -> "원격 조작과 송금 지시가 함께 나타난 상황이에요."
    DemoScenario.INVESTMENT -> "출금 전 추가 입금과 비밀 요구가 연결돼요."
    DemoScenario.EDUCATION -> "위험한 단어가 있어도 종료된 교육 내용을 현재 지시로 취급하지 않아요."
    DemoScenario.NEW_ACCOUNT -> "300만 원과 새 계좌라는 이유만으로 보류하지 않아요."
    DemoScenario.UNRELATED -> "다른 거래에 연결된 링크를 친구의 정산에 붙이지 않아요."
}
''')
# State changes are atomic with respect to the demo operation, never an approval switch.
p=ui/'BankViewModel.kt'
edit(p,'delay(950)','delay(1000)')
edit(p,'    fun retryStorage()', '''    fun startScenario(scenario: DemoScenario, done: () -> Unit) = act {
        repository.reset(scenario)
        graph.preferences.easy(scenario == DemoScenario.EASY)
        repository.setDraft(TransferDraft(Fixtures.recipient(scenario), Fixtures.amount(scenario), Fixtures.purpose(scenario)))
        repository.reviewDraft()
        done()
    }
    fun retryStorage()''')
# Shared profile and card controls are local preferences, not verified identity.
p=root/'app/src/main/kotlin/app/saeon/trace/data/Preferences.kt'
edit(p,'val themeMode: String = "light", val reducedMotion: Boolean = false','val themeMode: String = "light", val reducedMotion: Boolean = false, val displayName: String = "한지우", val cardFrozen: Boolean = false')
edit(p,'private val motion =','private val displayName = stringPreferencesKey("display_name")\n    private val cardFrozen = booleanPreferencesKey("card_frozen")\n    private val motion =')
edit(p,'it[theme] ?: "light", it[motion] ?: false)','it[theme] ?: "light", it[motion] ?: false, it[displayName] ?: "한지우", it[cardFrozen] ?: false)')
edit(p,'suspend fun theme(value: String)','suspend fun displayName(value: String) { require(value.trim().length in 1..12); store.edit { it[displayName] = value.trim() } }\n    suspend fun cardFrozen(value: Boolean) { store.edit { it[cardFrozen] = value } }\n    suspend fun theme(value: String)')
# Rebuild Home, with one primary account surface instead of a white report.
p=ui/'screens/BankingScreens.kt'
edit(p,'import androidx.compose.ui.unit.dp','import androidx.compose.ui.unit.dp\nimport androidx.compose.ui.unit.sp\nimport androidx.compose.ui.graphics.Color\nimport androidx.compose.foundation.shape.RoundedCornerShape\nimport androidx.compose.foundation.shape.CircleShape\nimport androidx.compose.ui.draw.clip')
block(p,'@Composable\nfun HomeScreen','@Composable fun ReceiptRow',r'''@Composable
fun HomeScreen(state: BankState, preferences: BankPreferences, model: BankViewModel, open: (String) -> Unit) {
    Page(title="새온은행",tag="home",actions={
        IconAction(BankIcons.Bell,"알림"){open("notifications")}
        IconAction(BankIcons.Profile,"내 정보"){open("profile")}
    }) {
        AccountHero(state.balance,preferences.hideBalance,{model.preference{hideBalance(!preferences.hideBalance)}},
            {open("transfer")},{open("bring")},{open("account")})
        Space(14)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            MiniMetric("이번 달 쓴 돈","382,400원",BankIcons.Card,Modifier.weight(1f)){open("card")}
            MiniMetric("모아둔 돈","${won(state.savings)}원",BankIcons.Assets,Modifier.weight(1f)){open("savings")}
        }
        Space(14)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(TraceColors.CoralLight)
            .clickable(role=Role.Button){open("safety")}.padding(16.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Icon(BankIcons.Trace,null,Modifier.size(30.dp),tint=TraceColors.Coral)
            Column(Modifier.weight(1f)) {
                Text(if(state.pending.isEmpty())"보내기 전, 한 번 더." else "보류한 송금 ${state.pending.size}건",style=MaterialTheme.typography.titleSmall)
                Text(if(state.pending.isEmpty())"TRACE가 송금의 맥락을 확인해요"else"아직 돈은 나가지 않았어요",style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
            }
            AppIcon(BankIcons.Chevron,size=16)
        }
        Space(23);SectionTitle("최근 거래","전체 보기"){open("history")}
        state.receipts.take(3).forEach{ReceiptRow(it){open("receipt/${it.id}")}}
        if(state.recurringEnabled) {
            Space(18);SectionTitle("다가오는 일정")
            SurfaceBox { MenuRow("통신비","9월 25일 자동이체",BankIcons.Calendar,trailing="68,000원"){open("recurring")} }
        }
        Space(18)
        MenuRow("시연 센터","같은 송금, 다른 맥락을 비교해 보세요",BankIcons.Trace,tag="home_demo"){open("demo_lab")}
        Space(8);SimulationNote()
    }
}''')
block(p,'@Composable fun ReceiptRow','@Composable fun AssetsScreen',r'''@Composable fun ReceiptRow(receipt:TransferReceipt,onClick:()->Unit) {
    val icon=if(receipt.recipient.id=="cafe")BankIcons.Card else if(receipt.direction==Direction.CREDIT)BankIcons.Download else BankIcons.Transfer
    Row(Modifier.fillMaxWidth().heightIn(min=78.dp).clickable(role=Role.Button,onClick=onClick).padding(vertical=12.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(13.dp)) {
        Box(Modifier.size(44.dp).background(TraceColors.Surface,CircleShape),contentAlignment=Alignment.Center){AppIcon(icon,size=20)}
        Column(Modifier.weight(1f)) {
            Text(receipt.recipient.name,style=MaterialTheme.typography.bodyLarge,fontWeight=FontWeight.Medium)
            Space(4);Caption(receipt.memo.ifEmpty{receipt.purpose.label})
        }
        Column(horizontalAlignment=Alignment.End) {
            Text("${if(receipt.direction==Direction.DEBIT)"−"else"+"}${won(receipt.amount)}원",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold)
            Space(4);Caption(dateLabel(receipt.completedAt).substring(5,10))
        }
    }
}''')
block(p,'@Composable fun AssetsScreen','@Composable fun AccountScreen',r'''@Composable fun AssetsScreen(state:BankState,open:(String)->Unit) {
    Page(title="내 자산",tag="assets") {
        Space(8);Caption("차곡차곡 모은 돈");Space(10);AmountDisplay(state.balance+state.savings);Space(8)
        Caption("입출금과 저축 계좌의 합계예요.");Space(28)
        SurfaceBox {
            MenuRow("새온 생활통장","110-***-0001",BankIcons.Bank,tag="asset_primary_account"){open("account")}
            AmountDisplay(state.balance);Space(16);PrimaryButton("송금"){open("transfer")}
        }
        Space(14);SurfaceBox {
            MenuRow("새온 모아적금","300만 원을 향해 모으는 중",BankIcons.Assets){open("savings")}
            AmountDisplay(state.savings);Space(16)
            LinearProgressIndicator(progress={ (state.savings/3000000f).coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),color=TraceColors.Coral,trackColor=TraceColors.Divider)
        }
        Space(24);SectionTitle("카드와 대출")
        MenuRow("새온 체크카드","이번 달 382,400원",BankIcons.Card){open("card")}
        MenuRow("생활안심대출","남은 원금 ${won(state.loanBalance)}원",BankIcons.Bank){open("loan")}
        Space(16);Caption("대출은 위 자산 합계에 더하지 않습니다.");Space(14);SimulationNote()
    }
}''')
# Replace static card report with a real local freeze switch and clear statement.
block(p,'@Composable fun CardScreen','@Composable fun LoanScreen',r'''@Composable fun CardScreen(state:BankState,preferences:BankPreferences,model:BankViewModel,open:(String)->Unit,back:()->Unit) {
    Page(title="새온 체크카드",back=back,tag="card_detail") {
        Column(Modifier.fillMaxWidth().background(Color(0xFF202723),RoundedCornerShape(26.dp)).padding(24.dp)) {
            Text("새온 데일리",color=Color.White,style=MaterialTheme.typography.titleMedium);Space(38)
            Icon(BankIcons.Card,null,Modifier.size(34.dp),tint=Color(0xFFBFCABD));Space(20)
            Text("••••  ••••  ••••  0824",color=Color.White,style=MaterialTheme.typography.titleMedium)
            Space(8);Text(if(preferences.cardFrozen)"일시 정지 중"else"새온 생활통장 연결",color=Color(0xFFBFCABD),style=MaterialTheme.typography.bodySmall)
        }
        Space(24);Caption("9월 이용 금액");Space(8);AmountDisplay(382400);Space(18)
        OptionRow("카드 일시 정지",preferences.cardFrozen,"이 기기에 저장된 시연 카드의 상태를 변경해요."){model.preference{cardFrozen(it)}}
        Space(16);SectionTitle("최근 이용 내역")
        state.receipts.filter{it.recipient.id=="cafe"}.forEach{ReceiptRow(it){open("receipt/${it.id}")}}
        DetailRow("나머지 월간 이용","376,900원");Space(20)
        Caption("가상 카드입니다. 실제 승인망이나 카드 정지 요청은 연결되지 않습니다.")
    }
}''')
# Cleaner review and result; keep receipt detail on a separate screen.
p=ui/'screens/TransferScreens.kt'
edit(p,'import androidx.compose.ui.unit.dp','import androidx.compose.ui.unit.dp\nimport androidx.compose.ui.unit.sp\nimport androidx.compose.ui.graphics.Color\nimport androidx.compose.foundation.shape.RoundedCornerShape\nimport androidx.compose.ui.draw.clip')
block(p,'@Composable private fun ReviewScreen','@Composable private fun EvaluatingScreen',r'''@Composable private fun ReviewScreen(state:BankState,record:TransferRecord,interaction:InteractionState,model:BankViewModel,open:(String)->Unit,back:()->Unit) {
    val intent=record.intent
    val amountError=runCatching{BankEngine.validateAmount(state,intent.amount,intent.purpose,model.repository.clock.now())}.exceptionOrNull()?.message
    Page(title="송금 확인",tag="transfer_review",back=back,footer={
        Row(Modifier.fillMaxWidth().padding(bottom=12.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {
            Icon(BankIcons.Trace,null,Modifier.size(19.dp),tint=TraceColors.Coral)
            Text("보내기 전 맥락까지 확인해요",Modifier.padding(start=5.dp),style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
        }
        PrimaryButton("${won(intent.amount)}원 보내기",Modifier.testTag("transfer_confirm"),enabled=amountError==null&&!interaction.busy&&record.stage==TransferStage.REVIEW){model.requestAuthorization(intent.id)}
    }) {
        Space(16)
        Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
            PersonBadge(intent.recipient.name,60,true);Space(18)
            Text("${intent.recipient.name}${if(intent.recipient.kind==RecipientKind.PERSON)"님에게"else"로"}",style=MaterialTheme.typography.titleMedium,textAlign=TextAlign.Center)
            Space(10);Text("${won(intent.amount)}원",style=MaterialTheme.typography.displaySmall.copy(fontSize=34.sp,letterSpacing=(-.8).sp))
            Space(4);Text("보낼까요?",style=MaterialTheme.typography.headlineSmall)
        }
        Space(30)
        SurfaceBox {
            DetailRow("받는 계좌","${intent.recipient.bank}\n${intent.recipient.account}")
            DetailRow("출금 계좌","새온 생활통장")
            DetailRow("수수료","무료",true)
            if(intent.purpose!=Purpose.GENERAL)DetailRow("목적",intent.purpose.label)
        }
        Space(8)
        QuietButton("금액·목적 수정",Modifier.fillMaxWidth().testTag("review_edit"),enabled=record.stage==TransferStage.REVIEW&&!interaction.busy){model.editReview(intent.id){open("amount")}}
        if(intent.officialRouteId!=null){Space(10);Caption("공식 경로로 새로 만든 송금이에요. 이전 인증은 재사용하지 않아요.")}
        amountError?.let{ErrorNote(it)};record.error?.takeIf{it!=amountError}?.let{ErrorNote(it)}
    }
}''')
block(p,'@Composable private fun EvaluatingScreen','@OptIn(ExperimentalMaterial3Api::class)',r'''@Composable private fun EvaluatingScreen(recipient:String,cancel:()->Unit) { ContextLoading(cancel) }''')
# Remove the delivered-line diagram from the normal result, not just its color.
s=p.read_text();a=s.index('@Composable private fun CompleteScreen')
s=s[:a]+r'''@Composable private fun CompleteScreen(state:BankState,record:TransferRecord,open:(String)->Unit,home:()->Unit) {
    val receipt=state.receipts.find{it.intentId==record.intent.id&&!it.seed}
    if(receipt==null){Page(title="송금 결과 확인",back=home,footer={PrimaryButton("거래 내역 확인"){open("history")}}){EmptyState("완료 내역을 확인해 주세요.","다시 보내지 말고 거래 내역을 먼저 확인하세요.")};return}
    Page(tag="transfer_complete",footer={PrimaryButton("확인",Modifier.testTag("complete_confirm"),onClick=home)}) {
        Space(44)
        Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
            ResultSeal();Space(28)
            Text(if(record.intent.purpose==Purpose.LOAN)"공식 상환처로\n${won(receipt.amount)}원을 보냈어요"else"${receipt.recipient.name}님에게\n${won(receipt.amount)}원을 보냈어요",
                style=MaterialTheme.typography.headlineSmall,textAlign=TextAlign.Center,modifier=Modifier.semantics{heading()})
            Space(12);Text("수수료 없이 잘 도착했어요",style=MaterialTheme.typography.bodyMedium,color=TraceColors.Muted)
        }
        Space(36)
        SurfaceBox {
            DetailRow("받는 계좌","${receipt.recipient.bank}\n${receipt.recipient.account}")
            DetailRow("출금 계좌","새온 생활통장")
        }
        Space(12);QuietButton("송금 내역 보기",Modifier.fillMaxWidth().testTag("complete_receipt")){open("receipt/${receipt.id}")}
        Space(24);Text("실제 자금 이동이 없는 시연 기록입니다.",Modifier.fillMaxWidth(),textAlign=TextAlign.Center,style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
    }
}
''';p.write_text(s)
# Preserve evidence sheets, but correct generic institution-only language for new scenarios.
edit(p,'기관 사칭과 급한 송금 요청이 이 거래와 연결돼, 잠시 멈췄어요.','앞선 외부 요청과 지금의 송금이 연결돼, 잠시 멈췄어요.')
edit(p,'잠깐, 확인하고\\n보내볼까요?','이 송금,\\n잠시 멈췄어요')
# Settings: expose the presentation center, with a distinct story preview and comparison.
p=ui/'screens/SettingsScreens.kt'
edit(p,'import androidx.compose.ui.unit.dp','import androidx.compose.ui.unit.dp\nimport androidx.compose.ui.unit.sp\nimport androidx.compose.ui.text.style.TextAlign\nimport androidx.compose.foundation.shape.RoundedCornerShape\nimport androidx.compose.ui.draw.clip')
edit(p,'MenuRow("앱 정보",','MenuRow("시연 센터", "12가지 맥락과 거래의 차이", BankIcons.Trace, tag = "demo_center_open") { open("demo_lab") }\n        MenuRow("앱 정보",')
block(p,'@Composable fun ProfileScreen','@Composable fun SecurityScreen',r'''@Composable fun ProfileScreen(preferences:BankPreferences,model:BankViewModel,back:()->Unit) {
    var name by rememberSaveable(preferences.displayName){mutableStateOf(preferences.displayName)}
    var saved by remember{mutableStateOf(false)}
    Page(title="내 정보",tag="profile",back=back,footer={PrimaryButton("이름 저장",enabled=name.trim().length in 1..12){model.preference{displayName(name)};saved=true}}) {
        Space(20);PersonBadge(preferences.displayName,64,true);Space(20);Headline("${preferences.displayName}님");Space(12)
        Caption("새온은행 시연 계정");Space(30)
        Field(name,"화면에 표시할 이름",{name=it.take(12);saved=false},Modifier.testTag("profile_name"))
        Space(12);Caption(if(saved)"표시 이름을 저장했어요."else"최대 12자. 실제 예금주 이름을 바꾸지 않습니다.")
        Space(28);SurfaceBox {DetailRow("주 거래 계좌","새온 생활통장");DetailRow("가입일","2026.03.20")}
        Space(22);Caption("실명·연락처 등록이 없는 가상 계정입니다.")
    }
}''')
block(p,'@Composable fun DemoLabScreen','@Composable fun AppearanceScreen',r'''@Composable fun DemoLabScreen(state:BankState,model:BankViewModel,open:(String)->Unit,home:()->Unit,back:()->Unit) {
    var filter by rememberSaveable{mutableStateOf("전체")}
    var resetting by remember{mutableStateOf(false)}
    Page(title="시연 센터",tag="demo_lab",back=back) {
        Space(8);Headline("같은 송금,\n다른 맥락.");Space(12)
        Body("돈을 보낸 이유가 달라지면,\nTRACE의 판단도 달라집니다.",subdued=true);Space(24)
        SurfaceBox {
            Text("발표용 핵심 흐름",style=MaterialTheme.typography.titleSmall);Space(6);Caption("정상 정산에서 시작해 사칭 보류, 공식 상환으로 이어가세요.")
            Space(12)
            listOf(DemoScenario.NORMAL,DemoScenario.IMPERSONATION,DemoScenario.LOAN).forEachIndexed{i,s->
                MenuRow("${i+1}. ${s.label}","${won(Fixtures.amount(s))}원",BankIcons.Arrow,tag="showcase_${s.name}"){open("scenario/${s.name}")}
            }
        }
        Space(22)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("전체","정상","위험","확인").forEach{label->FilterChip(filter==label,{filter=label},label={Text(label)},modifier=Modifier.heightIn(min=48.dp))}
        }
        Space(12)
        val items=DemoScenario.entries.filter{filter=="전체"||when(filter){"정상"->it.expected=="ALLOW";"위험"->it.expected=="HOLD";else->it.expected!="ALLOW"&&it.expected!="HOLD"}}
        items.forEach{s->
            Column(Modifier.fillMaxWidth().padding(bottom=12.dp).clip(RoundedCornerShape(23.dp)).background(TraceColors.Surface)
                .clickable(role=Role.Button){open("scenario/${s.name}")}.testTag("demo_${s.name}").padding(18.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text(s.label,Modifier.weight(1f),style=MaterialTheme.typography.titleSmall)
                    Text(when{ s.expected=="ALLOW"->"통과";s.expected=="HOLD"->"보류";s.expected=="WARN"->"재확인";else->"경로 확인"},
                        color=TraceColors.CoralText,style=MaterialTheme.typography.labelMedium)
                }
                Space(7);Caption(scenarioNote(s));Space(12)
                Text("${won(Fixtures.amount(s))}원",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold)
            }
        }
        Space(12);PrimaryButton("시연 데이터 초기화",Modifier.testTag("demo_reset")){resetting=true}
        SecondaryButton("15분 경과 재현"){model.act{model.repository.expireSignalsForDemo()}}
        Caption("시연 입력을 바꿉니다. 판단을 강제로 선택하지 않습니다. 실제 통화·문자 수집이나 탐지율 측정은 아닙니다.")
    }
    if(resetting)AlertDialog(onDismissRequest={resetting=false},title={Text("시연 기록을 초기화할까요?")},text={Text("가상 잔액과 거래 기록을 처음 상태로 돌립니다. 실제 금융 데이터는 없습니다.")},
        confirmButton={QuietButton("초기화",Modifier.testTag("demo_start")){model.reset(DemoScenario.NORMAL){resetting=false;home()}}},dismissButton={QuietButton("취소"){resetting=false}},containerColor=TraceColors.Surface)
}

@Composable fun ScenarioStoryScreen(scenario:DemoScenario,model:BankViewModel,open:(String)->Unit,back:()->Unit) {
    var contextView by rememberSaveable{mutableStateOf(true)}
    Page(title=scenario.label,tag="scenario_story",back=back,footer={
        PrimaryButton("이 상황에서 송금하기",Modifier.testTag("scenario_begin")){model.startScenario(scenario){open("transfer_state")}}
    }) {
        Space(10);Text("송금 전에 있었던 일",style=MaterialTheme.typography.headlineSmall);Space(12)
        Caption("이 화면의 통화와 문자는 시연용으로 준비한 입력입니다.");Space(22)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(!contextView,{contextView=false},label={Text("거래 정보만")},modifier=Modifier.weight(1f).heightIn(min=48.dp))
            FilterChip(contextView,{contextView=true},label={Text("앞선 맥락까지")},modifier=Modifier.weight(1f).heightIn(min=48.dp))
        }
        Space(12)
        SurfaceBox {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                PersonBadge(Fixtures.recipient(scenario).name,46,true)
                Column{Text(Fixtures.recipient(scenario).name,style=MaterialTheme.typography.titleSmall);Caption(Fixtures.recipient(scenario).bank)}
            }
            Space(18);AmountDisplay(Fixtures.amount(scenario));Space(10)
            Caption(if(Fixtures.recipient(scenario).known)"이전에 보낸 계좌"else"처음 보내는 계좌")
        }
        if(contextView) {
            Space(18)
            Column(Modifier.fillMaxWidth().background(TraceColors.CoralLight,RoundedCornerShape(22.dp)).padding(20.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically){AppIcon(BankIcons.Message,tint=TraceColors.CoralText,size=20);Text("앞선 요청",Modifier.padding(start=8.dp),style=MaterialTheme.typography.labelMedium,color=TraceColors.CoralText)}
                Space(12);Body(Fixtures.message(scenario))
            }
            Space(18)
            val events=Fixtures.events(scenario,Fixtures.epoch)
            events.forEach{e->
                Row(Modifier.fillMaxWidth().padding(vertical=11.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    Caption(timeLabel(e.createdAt),Modifier.width(48.dp));Column(Modifier.weight(1f)){Text(e.summary,style=MaterialTheme.typography.bodyMedium);Space(3)
                    if(!e.active(Fixtures.epoch))Caption("종료된 맥락")else if(e.recipientId!=null)Caption("다른 거래에 연결된 맥락")}
                }
            }
            Space(12);Body(scenarioNote(scenario),subdued=true)
        } else {
            Space(22);Body("금액과 계좌만으로는\n왜 보내는지 알 수 없어요.");Space(12)
            Caption("거래 정보와 맥락 정보의 차이를 비교합니다. 다른 은행의 실제 승인 결과를 재현하는 것은 아닙니다.")
        }
        Space(24);Caption("시작하면 이전 시연 기록을 초기화하고 새 거래를 만듭니다. 결과는 주입된 신호와 거래를 결합한 로컬 시연 정책이 결정합니다.")
    }
}
''')
# Root navigation is a glass overlay, not a full-width slab consuming the layout.
p=ui/'SaeonApp.kt'
s=p.read_text()
s=s.replace('import androidx.compose.ui.unit.dp','''import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot''')
s=s.replace('val reduced = LocalReducedMotion.current','val reduced = LocalReducedMotion.current\n    val backdrop = rememberGraphicsLayer()\n    var contentOrigin by remember { mutableStateOf(Offset.Zero) }')
s=s.replace('Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding())','Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding())')
s=s.replace('Box(Modifier.weight(1f)) {','''CompositionLocalProvider(LocalPageBottomInset provides if(route in roots) 112 else 28) {
                Box(Modifier.fillMaxSize().onGloballyPositioned { contentOrigin=it.positionInRoot() }.drawWithContent {
                    backdrop.record { this@drawWithContent.drawContent() }
                    drawLayer(backdrop)
                }) {''')
a=s.index('                if (route in roots) {');b=s.index('            }\n        }\n    }\n    interaction.authChallenge',a)
s=s[:a]+'''                }
                if (route in roots) {
                    val tabs = listOf(Triple("home","홈",BankIcons.Home),Triple("assets","자산",BankIcons.Assets),
                        Triple("transfer","송금",BankIcons.Transfer),Triple("safety","안전",BankIcons.Shield),Triple("more","전체",BankIcons.More))
                    GlassDock(tabs,route,backdrop,contentOrigin,Modifier.align(Alignment.BottomCenter).padding(horizontal=18.dp,vertical=10.dp)) { destination ->
                        nav.navigate(destination) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState=true }
                            launchSingleTop=true; restoreState=true
                        }
                    }
                }
'''+s[b:]
s=s.replace('CardScreen(state, open, back)','CardScreen(state, preferences, model, open, back)')
s=s.replace('ProfileScreen(back)','ProfileScreen(preferences, model, back)')
s=s.replace('DemoLabScreen(state, model, home, back)','DemoLabScreen(state, model, open, home, back)')
s=s.replace('composable("demo_lab") {','composable("scenario/{id}") { target -> ScenarioStoryScreen(runCatching { DemoScenario.valueOf(target.arguments?.getString("id").orEmpty()) }.getOrDefault(DemoScenario.NORMAL), model, open, back) }\n                        composable("demo_lab") {')
s=s.replace('이 앱은 가상 거래를 시연합니다. 인증을 마치면 거래 앞의 위험 정황을 별도로 확인해요.','시연 확인 뒤 TRACE가 약 1초 동안 송금 맥락을 살펴봐요. 실제 비밀번호는 입력하지 않습니다.')
p.write_text(s)
# Each added scenario receives a core test, independent of screen-selection controls.
write(root/'core/src/test/kotlin/app/saeon/trace/core/ScenarioMatrixTest.kt',r'''package app.saeon.trace.core
import org.junit.Test
import org.junit.Assert.*
class ScenarioMatrixTest {
    @Test fun everyScenarioIsDerivedFromItsInputs() {
        for(s in DemoScenario.entries) {
            val now=Fixtures.epoch
            val intent=TransactionIntent("case-${s.name}",Fixtures.amount(s),Fixtures.recipient(s),Fixtures.purpose(s),now)
            val actual=BankPolicy.evaluate(TransferRecord(intent),RiskContext(Fixtures.events(s,now)),now).decision
            val expected=when { s.expected=="HOLD" -> PolicyDecision.HOLD; s.expected=="WARN" -> PolicyDecision.WARN; s.expected.startsWith("VERIFY") -> PolicyDecision.VERIFY; else -> PolicyDecision.ALLOW }
            assertEquals(s.name,expected,actual)
        }
    }
    @Test fun equalAmountAndNewRecipientDoNotDetermineTheDecision() {
        fun result(s:DemoScenario):PolicyDecision { val n=Fixtures.epoch;return BankPolicy.evaluate(TransferRecord(TransactionIntent(s.name,3_000_000,Fixtures.kim,Purpose.GENERAL,n)),RiskContext(Fixtures.events(s,n)),n).decision }
        assertEquals(PolicyDecision.ALLOW,result(DemoScenario.NEW_ACCOUNT))
        assertEquals(PolicyDecision.HOLD,result(DemoScenario.IMPERSONATION))
    }
    @Test fun historicalEducationAndUnrelatedContextAreNotActiveRequests() {
        for(s in listOf(DemoScenario.EDUCATION,DemoScenario.UNRELATED)) {
            val n=Fixtures.epoch;val i=TransactionIntent(s.name,32000,Fixtures.seoyeon,Purpose.SETTLEMENT,n)
            assertTrue(RiskContext(Fixtures.events(s,n)).relevant(i,n).isEmpty())
        }
    }
}
''')
write(root/'app/src/androidTest/kotlin/app/saeon/trace/RedesignTest.kt',r'''package app.saeon.trace
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.saeon.trace.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File
@RunWith(AndroidJUnit4::class)
class RedesignTest:UiHarness() {
    @Test fun completePresentationAndThemeMatrix() {
        fresh();capture("v3_home",audit=false)
        compose.onNodeWithTag("home_transfer").assertIsDisplayed()
        compose.onNodeWithTag("glass_dock").assertIsDisplayed()
        for(route in listOf("assets","transfer","safety","more","demo_lab","scenario/IMPERSONATION","scenario/EDUCATION","appearance","card","profile")) {
            navigate(route);capture("v3_${route.replace('/','_')}",audit=false)
        }
        createReview(DemoScenario.NORMAL);capture("v3_review",audit=false)
        tap("transfer_confirm");tap("auth_confirm");waitScreen("transfer_complete")
        assertEquals(Fixtures.START_BALANCE-32000,state.balance);capture("v3_complete",audit=false)
        for(s in listOf(DemoScenario.IMPERSONATION,DemoScenario.LOAN,DemoScenario.WARN,DemoScenario.FAMILY_FAKE,DemoScenario.REMOTE,DemoScenario.INVESTMENT,DemoScenario.NEW_ACCOUNT,DemoScenario.EDUCATION,DemoScenario.UNRELATED)) {
            evaluated(s);capture("v3_${s.name}",audit=false)
            if(s.expected=="HOLD")assertEquals(Fixtures.START_BALANCE,state.balance)
            if(s.expected=="ALLOW")assertEquals(Fixtures.START_BALANCE-Fixtures.amount(s),state.balance)
        }
        fresh();runBlocking{graph.preferences.theme("dark")};compose.waitUntil(10000){compose.activity.model.preferences.value.themeMode=="dark"}
        navigate("home");Thread.sleep(500);capture("v3_dark_home",audit=false)
        navigate("demo_lab");capture("v3_dark_lab",audit=false)
        createReview(DemoScenario.NORMAL);capture("v3_dark_review",audit=false)
    }
    @Test fun exactGateAndNoPrematureDebit() {
        fresh();createReview(DemoScenario.NORMAL)
        val challenge=runBlocking{repository.prepare(state.currentTransferId!!)}
        compose.runOnIdle { compose.activity.model.cancelAuthorization() }
        compose.waitForIdle()
        tap("transfer_confirm");waitScreen("auth_confirm")
        // Wall-time observations use the repository, not the Compose auto-advance clock.
        compose.mainClock.autoAdvance=false
        compose.onNodeWithTag("auth_confirm").performClick()
        val begin=System.nanoTime()
        while(state.current?.stage!=TransferStage.EVALUATING && (System.nanoTime()-begin)<2_000_000_000L)Thread.sleep(5)
        val entered=System.nanoTime();val balanceAtGate=state.balance
        Thread.sleep(350);assertEquals(Fixtures.START_BALANCE,state.balance)
        while(state.current?.stage!=TransferStage.COMPLETE && (System.nanoTime()-entered)<5_000_000_000L)Thread.sleep(5)
        val duration=(System.nanoTime()-entered)/1_000_000
        assertEquals(Fixtures.START_BALANCE,balanceAtGate)
        assertTrue("Gate ended too early: $duration",duration>=900)
        assertTrue("Gate did not finish: $duration",duration<4500)
        File(output,"gate_timing.txt").writeText("gate_ms=$duration\nbalance_before=$balanceAtGate\nbalance_after=${state.balance}\n")
        compose.mainClock.autoAdvance=true;compose.waitForIdle()
    }
    @Test fun presentationCenterStartsRealInputAndControlsPersist() {
        fresh();navigate("scenario/REMOTE");tap("scenario_begin");waitScreen("transfer_review")
        assertEquals(DemoScenario.REMOTE,state.scenario)
        confirmThroughUi("trace_hold");assertEquals(Fixtures.START_BALANCE,state.balance)
        navigate("card")
        compose.onNodeWithText("卡").assertDoesNotExist()
        runBlocking{graph.preferences.cardFrozen(true);graph.preferences.displayName("새온사용자")}
        compose.waitUntil(10000){compose.activity.model.preferences.value.cardFrozen}
        navigate("card");compose.onNodeWithText("일시 정지 중").assertExists()
        navigate("profile");compose.onNodeWithText("새온사용자님").assertExists()
        fresh();runBlocking{graph.preferences.motion(true)};createReview(DemoScenario.IMPERSONATION)
        confirmThroughUi("trace_hold");assertEquals(Fixtures.START_BALANCE,state.balance)
    }
}
''')
# Existing targeted test referenced the discarded illustration; assert the result instead.
p=root/'app/src/androidTest/kotlin/app/saeon/trace/ExperienceTest.kt'
edit(p,'compose.onNodeWithTag("transfer_journey").assertExists()','compose.onNodeWithTag("transfer_complete").assertExists()')
print('V3 source prepared: bank hierarchy, glass dock, 1000ms context gate, compact result and 12 input-driven scenarios.')
