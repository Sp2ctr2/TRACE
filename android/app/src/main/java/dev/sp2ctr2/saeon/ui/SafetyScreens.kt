package dev.sp2ctr2.saeon.ui

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sp2ctr2.saeon.BankViewModel
import dev.sp2ctr2.saeon.data.AppOptions
import dev.sp2ctr2.saeon.domain.*

fun riskLabel(type: RiskType) = when(type) {
    RiskType.IMPERSONATION -> "기관 사칭 정황"
    RiskType.URGENCY -> "긴급 송금 요구"
    RiskType.SUSPICIOUS_LINK -> "미확인 링크"
    RiskType.FINANCIAL_INSTRUCTION -> "자금 이동 요청"
    RiskType.NEW_RECIPIENT -> "처음 보내는 계좌"
    RiskType.PURPOSE_MISMATCH -> "상환 경로 확인 필요"
}

@Composable fun SafetyScreen(state: BankState, options: AppOptions, go: (String)->Unit) {
    val d=state.draft
    val active=BankEngine.activeRisks(state,state.now())
    val waiting=pending(state)
    Screen("안전 센터",tag="safety_center",actions={GlyphButton("info","TRACE 정보"){go("about")}}) {
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){TraceMark();Copy("trace",size=22,color=TraceColors.Ink,weight=FontWeight.Bold)}
        Gap(24)
        Title(when {state.heldCase->"서두르지 않아도\n괜찮습니다";waiting->"확인을 기다리는\n송금이 있어요";active.isNotEmpty()->"최근 요청을\n한 번 더 살펴보세요";else->"평소엔 조용하게,\n필요할 땐 분명하게"},28)
        Gap(14);Copy(when {state.heldCase->"위험 맥락이 연결돼 송금을 멈췄어요. 안전한 다음 행동을 함께 확인하세요.";waiting->"확인 중인 거래는 아직 출금되지 않았습니다.";active.isNotEmpty()->"공유한 내용에서 찾은 신호를 실제 송금과 함께 확인합니다.";else->"현재 확인이 필요한 송금이 없습니다. 사용자가 공유한 내용만 기기 안에서 확인해요."})
        if(waiting && d!=null) {Gap(24);TransactionSummary(d,"확인이 필요한 송금");Gap(12);Primary("송금 상태 확인","safety_pending"){go("result")}}
        else if(state.heldCase) {Gap(20);Primary("안전하게 확인하기"){go("guide")}}
        Gap(28);Rule();Gap(14)
        SectionHeading("최근 맥락","전체 흐름"){go("timeline")}
        if(state.risks.isEmpty()) {Copy("아직 공유된 위험 신호가 없어요.",size=14);Gap(18)}
        state.risks.takeLast(3).forEach { e -> ListRow(riskLabel(e.type),dateLabel(e.at,"HH:mm")+" · "+if(e.source==EventSource.DEMO) "시연 신호" else "직접 공유한 내용",value=if(e.expiresAt<=state.now()) "만료" else null,onClick={go("timeline")}) }
        Gap(10);Rule();Gap(10)
        ListRow("받은 내용 직접 확인","문자나 메시지를 붙여넣어 주세요",icon="message",tag="safety_check",onClick={go("share")})
        ListRow("지금 해야 할 일","공식 경로로 독립적으로 확인하기",icon="shield",onClick={go("guide")})
        ListRow("개인정보 경계","어떤 정보가 기기에 남는지 확인",icon="lock",tag="safety_privacy",onClick={go("privacy")})
        ListRow("쉬운 모드",if(options.easy) "사용 중 · 크게, 간단하게" else "큰 글씨와 간단한 안내",icon="eye",onClick={go("settings/accessibility")})
    }
}

private data class TimelineItem(val at: Long,val title: String,val detail: String,val important: Boolean=false,val expired: Boolean=false)
@Composable fun TimelineScreen(state: BankState, back: ()->Unit) {
    val d=state.draft
    val items=buildList {
        state.risks.sortedBy{it.at}.forEach { add(TimelineItem(it.at,riskLabel(it.type),it.summary,expired=it.expiresAt<=state.now())) }
        if(d!=null) {
            add(TimelineItem(d.createdAt,if(d.recipient.saved) "저장된 수취인" else "처음 보내는 계좌",d.recipient.name+" · "+d.recipient.bank))
            add(TimelineItem(d.createdAt,"${money(d.amount)}원 송금 ${if(d.phase==Phase.COMPLETE) "완료" else "요청"}",when(d.phase){Phase.HOLD->"연결된 맥락에 따라 송금 보류";Phase.UNKNOWN->"공식 경로 미확인 · 송금 대기";Phase.COMPLETE->"확인한 새 거래만 완료";else->"현재 거래와 함께 확인"},important=true))
        }
    }
    Screen("위험 타임라인",tag="trace_timeline",onBack=back) {
        if(items.isNotEmpty()) {Eyebrow(dateLabel(items.first().at,"HH:mm")+" — "+dateLabel(items.last().at,"HH:mm"),true);Gap(18)}
        Title("흩어져 있던 신호가\n하나의 흐름으로",28);Gap(14)
        Copy("누가, 무엇을 요청했고\n어떤 송금으로 이어졌는지 확인합니다.",size=14)
        Gap(32)
        if(items.isEmpty()){QuietNotice("아직 연결된 흐름이 없습니다. 메시지를 직접 공유하면 이곳에서 신호를 확인할 수 있어요.")}
        items.forEachIndexed { i,t ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                Canvas(Modifier.width(12.dp).fillMaxHeight()) {
                    val x=size.width/2
                    if(i<items.lastIndex) drawLine(TraceColors.Line,Offset(x,8.dp.toPx()),Offset(x,size.height),strokeWidth=1.dp.toPx())
                    drawCircle(if(t.important) TraceColors.Action else TraceColors.Faint,3.dp.toPx(),Offset(x,7.dp.toPx()))
                }
                Column(Modifier.weight(1f).padding(bottom=28.dp)) {
                    Copy(dateLabel(t.at,"HH:mm")+if(t.expired) " · 만료된 신호" else "",size=12,color=if(t.important) TraceColors.Action else TraceColors.Muted)
                    Gap(8);Copy(t.title,size=18,color=if(t.important) TraceColors.Action else TraceColors.Ink,weight=FontWeight.SemiBold)
                    Gap(5);Copy(t.detail,size=14)
                }
            }
        }
        Gap(14);Rule();Gap(16);Copy("원문은 저장하지 않고 필요한 위험 신호만 남깁니다. 만료된 신호와 이미 보류된 거래의 상태는 별도로 관리합니다.",size=12)
    }
}

@Composable fun SafetyGuideScreen(state: BankState, go: (String)->Unit, back: ()->Unit) {
    val context=LocalContext.current
    var share by remember { mutableStateOf(false) }
    val summary=buildString {
        append("[TRACE 시연 상황 요약]\n")
        val d=state.draft
        if(d!=null) append("${money(d.amount)}원 송금 확인 중\n받는 분: ${d.recipient.name}\n상태: ${d.phase.name}\n")
        append("확인할 내용: "+state.risks.map{riskLabel(it.type)}.distinct().joinToString(", ")+"\n")
        append("상대가 전달한 번호나 링크가 아닌 독립적인 공식 경로로 확인이 필요합니다.\n실제 금융 거래나 경찰 신고가 아닙니다.")
    }
    Screen("안전하게 확인하기",tag="trace_safety_guide",onBack=back,footer={Primary("새온은행 고객지원","guide_support"){go("support")}}) {
        Eyebrow("지금 해야 할 일",true);Gap(18);Title("서두를 필요 없어요.\n순서대로 확인하세요",28)
        Gap(14);Copy(if(pending(state)) "아직 돈은 나가지 않았습니다.\n이 안내를 닫아도 자동 송금되지 않습니다." else "상대의 요청과 분리된 경로에서 직접 확인하세요.",color=TraceColors.Ink)
        Gap(28)
        val steps=listOf(
            "상대가 준 경로를 벗어나세요" to "전달받은 번호나 링크 대신, 직접 연 은행 앱의 고객지원 메뉴를 이용하세요.",
            "목적과 받는 곳을 함께 확인하세요" to "누구라고 말했는지뿐 아니라 실제 업무와 수취 계좌가 맞는지도 확인하세요.",
            "혼자 결정하지 않아도 됩니다" to "압박을 받고 있다면 대화를 중단하고 신뢰하는 사람에게 상황을 알려 함께 확인하세요."
        )
        steps.forEachIndexed { i,(title,body) -> Row(Modifier.padding(bottom=26.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Copy("0${i+1}",size=12,color=TraceColors.Action)
            Column(Modifier.weight(1f)){Copy(title,size=17,color=TraceColors.Ink,weight=FontWeight.SemiBold);Gap(8);Copy(body,size=15)}
        } }
        Rule();ListRow("상황 요약 공유","공유할 내용을 먼저 보여드립니다",icon="message",tag="guide_share",onClick={share=true})
        Gap(10);QuietNotice("새온은행 고객지원은 시연 기능입니다. 실제 은행 상담이나 경찰 신고를 실행하지 않습니다.","info")
    }
    if(share) TraceSheet("이 내용을 공유할까요?",{share=false}) {
        Copy(summary,size=14);Gap(24)
        Primary("공유할 앱 선택","guide_share_confirm") {
            share=false
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {type="text/plain";putExtra(Intent.EXTRA_TEXT,summary)},"상황 요약 공유"))
        }
        Copy("선택한 앱으로 요약이 전달됩니다. 대화 원문과 계좌 전체 번호는 포함하지 않습니다.",size=12)
    }
}

@Composable fun PrivacyScreen(state: BankState, vm: BankViewModel, back: ()->Unit) {
    var bank by rememberSaveable { mutableStateOf(false) }
    Screen("개인정보 경계",tag="privacy_boundary",onBack=back) {
        TraceMark(Modifier.size(26.dp));Gap(22);Title("지키기 위해,\n가져가지 않습니다",30)
        Gap(16);Copy("공유한 내용은 이 기기에서 분석합니다.\n대화 원문을 은행으로 보내지 않습니다.")
        Gap(28)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            FilterChip(selected=!bank,onClick={bank=false},label={Text("이 기기 안에서")},modifier=Modifier.testTag("privacy_device"),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=TraceColors.Soft,selectedLabelColor=TraceColors.Action))
            FilterChip(selected=bank,onClick={bank=true},label={Text("은행에 전달할 정보")},modifier=Modifier.testTag("privacy_bank"),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=TraceColors.Soft,selectedLabelColor=TraceColors.Action))
        }
        Gap(22);Rule();Gap(18)
        if(!bank) {
            ListRow("사용자가 공유한 내용","직접 입력하거나 공유하고, 확인을 누른 내용만 사용",icon="message")
            ListRow("기기 안의 규칙 분석","기관 사칭 표현, 긴급 요청, 금융 행동 요구를 구분",icon="shield")
            ListRow("구조화된 위험 신호","원문 대신 유형·시간·출처·제한된 수명만 보관",icon="receipt")
        } else {
            DetailRow("위험 이유","구조화된 reasonCodes")
            DetailRow("거래 연결","수취인 · 금액 · 거래 ID")
            DetailRow("확인 유효기간","제한된 시간과 일회성 거래")
            DetailRow("대화 원문","포함하지 않음",true)
            DetailRow("서명","기기 Keystore · ECDSA")
            Gap(14);QuietNotice("이번 APK는 네트워크 권한이 없습니다. 은행 역할도 이 기기 안의 시연 게이트웨이가 수행하며 외부 은행으로 전송하지 않습니다.","lock")
        }
        Gap(24);Rule();Gap(16)
        Copy("자동으로 듣거나 읽지 않습니다",size=17,color=TraceColors.Ink,weight=FontWeight.SemiBold);Gap(8)
        Copy("통화 녹음, 문자함 수집, 링크 접속 추적을 하지 않습니다. 입력 원문은 분석 완료 후 지우며 위험 신호는 15분 동안 평가에 사용합니다.",size=14)
        Gap(20);DetailRow("저장된 신호","${state.risks.size}건")
        Secondary("만료된 신호 정리","privacy_clear_expired"){vm.clearExpired()}
        Copy("신호를 정리해도 이미 보류된 거래는 자동으로 풀리지 않습니다.",size=12)
    }
}

@Composable fun ShareScreen(vm: BankViewModel, back: ()->Unit) {
    val incoming by vm.sharedText.collectAsStateWithLifecycle()
    val result by vm.analysisResult.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    // Deliberately not rememberSaveable: raw shared content never enters saved state or Room.
    var text by remember(incoming){mutableStateOf(incoming)}
    LaunchedEffect(result) { if(result!=null) text="" }
    Screen("받은 내용 확인",tag="shared_text_review",onBack={vm.clearShared();back()},footer={
        if(result==null) Primary("TRACE로 확인","share_analyze",text.isNotBlank() && !busy){vm.analyze(text)}
        else Primary("안전 센터로","share_to_safety"){vm.clearShared();vm.go("safety")}
    }) {
        if(result==null) {
            Title("이 내용을\n확인할까요?");Gap(14);Copy("상대가 보낸 메시지를 붙여넣거나 공유해 주세요. 확인을 누르기 전에는 분석하지 않습니다.")
            Gap(24);LabeledField(text,{text=it.take(LocalRiskAnalyzer.MAX_INPUT)},"공유할 내용","share_input",singleLine=false)
            Gap(10);Copy("${text.length} / 8,000자",size=12)
            Gap(18);QuietNotice("계좌 비밀번호, 인증번호 등 민감한 정보는 입력하지 마세요. 내용은 서버로 보내지 않습니다.","lock")
            Gap(14);Copy("기기 안의 규칙 기반 분석입니다. AI 모델의 범죄 판정이나 링크 악성 여부 검사가 아닙니다.",size=12)
        } else {
            Eyebrow("입력 원문 삭제됨",true);Gap(16)
            Title(if(result!!.isEmpty()) "찾은 위험 표현이\n없습니다" else "이런 신호가\n포함되어 있어요")
            Gap(14);Copy(if(result!!.isEmpty()) "위험 표현을 찾지 못했다고 안전이 보장되지는 않습니다. 받는 곳과 목적은 직접 확인하세요." else "이 신호를 실제 송금의 수취인·목적과 함께 확인합니다. 신호만으로 범죄 여부를 단정하지 않습니다.")
            Gap(24)
            result!!.forEach { ListRow(riskLabel(it.type),it.summary);Rule() }
            Gap(18);Secondary("다른 내용 확인","share_again"){vm.clearShared();text=""}
        }
    }
}
