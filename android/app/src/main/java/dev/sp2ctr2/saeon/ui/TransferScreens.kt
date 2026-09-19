package dev.sp2ctr2.saeon.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sp2ctr2.saeon.BankViewModel
import dev.sp2ctr2.saeon.data.AppOptions
import dev.sp2ctr2.saeon.domain.*

@Composable fun AmountScreen(state: BankState, vm: BankViewModel, back: ()->Unit) {
    val d=state.draft
    if(d==null){Screen("송금",onBack=back){Title("받는 분을 먼저 선택해 주세요",24)};return}
    var digits by rememberSaveable(d.id){mutableStateOf(d.amount.takeIf{it>0}?.toString().orEmpty())}
    var purpose by rememberSaveable(d.id){mutableStateOf(d.purpose)}
    var purposeSheet by remember { mutableStateOf(false) }
    val amount=digits.toLongOrNull() ?: 0L
    val invalid=BankEngine.validationError(state,d.copy(amount=amount,purpose=purpose))
    Screen("송금 금액",tag="transfer_amount",onBack=back,footer={
        Primary("다음","transfer_continue",invalid==null){vm.prepareReview(amount,purpose)}
    }) {
        Eyebrow(d.recipient.displayAccount)
        Gap(14);Title("${d.recipient.name}님에게\n얼마를 보낼까요?",27)
        Gap(28);Amount(amount,"amount_value",42)
        Gap(10);Copy("출금 가능 ${money(state.balance)}원",size=13)
        if(invalid!=null && amount>0){Gap(6);Copy(invalid,size=13,color=TraceColors.Action)}
        Gap(18)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf(10_000L to "+1만",100_000L to "+10만",1_000_000L to "+100만").forEach { (increment,label) ->
                TextButton(onClick={digits=(amount+increment).coerceAtMost(999_999_999).toString()},modifier=Modifier.weight(1f).heightIn(min=48.dp).background(TraceColors.Paper,RoundedCornerShape(8.dp)),colors=ButtonDefaults.textButtonColors(contentColor=TraceColors.Ink)){Text(label,fontSize=14.sp)}
            }
        }
        Gap(12)
        val keys=listOf(listOf("1","2","3"),listOf("4","5","6"),listOf("7","8","9"),listOf("clear","0","delete"))
        keys.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    val label=when(key){"clear"->"전체 지우기";"delete"->"마지막 숫자 지우기";else->key}
                    TextButton(onClick={digits=when(key){"clear"->"";"delete"->digits.dropLast(1);else->(digits+key).trimStart('0').take(9)}},modifier=Modifier.weight(1f).heightIn(min=58.dp).testTag("digit_$key").semantics{contentDescription=label},colors=ButtonDefaults.textButtonColors(contentColor=TraceColors.Ink)) {
                        if(key=="delete") Glyph("delete",size=23)
                        else Text(if(key=="clear") "전체 지우기" else key,fontSize=if(key=="clear") 12.sp else 26.sp,fontWeight=FontWeight.Medium)
                    }
                }
            }
        }
        Gap(6);Rule()
        ListRow("송금 목적",value=purposeLabel(purpose),tag="transfer_purpose",onClick={purposeSheet=true})
    }
    if(purposeSheet) TraceSheet("어떤 돈인가요?",{purposeSheet=false}) {
        Copy("목적과 실제 받는 곳이 같은지 함께 확인합니다.",size=14);Gap(12)
        Purpose.entries.forEach { p -> ListRow(purposeLabel(p),value=if(p==purpose) "선택됨" else null,onClick={purpose=p;purposeSheet=false}) }
    }
}

@Composable fun ReviewScreen(state: BankState, options: AppOptions, busy: Boolean, vm: BankViewModel, authenticate: ((()->Unit),(String)->Unit)->Unit, canAuthenticate: ()->Boolean, back: ()->Unit) {
    val d=state.draft
    var authRequest by remember { mutableStateOf<Pair<String,String>?>(null) }
    if(d==null){Screen("송금 확인",onBack=back){Title("송금 정보를 찾지 못했어요",24)};return}
    val valid=d.phase==Phase.REVIEW && BankEngine.validationError(state,d)==null
    Screen("송금 확인",tag="transfer_review",onBack=back,footer={
        Primary("${money(d.amount)}원 보내기","transfer_confirm",valid && !busy){authRequest=d.id to BankEngine.binding(state,d)}
        Copy("시연용 가상 거래 · 실제 자금 이동 없음",Modifier.fillMaxWidth(),size=11,align=androidx.compose.ui.text.style.TextAlign.Center)
    }) {
        Gap(14)
        Eyebrow(if(d.previousRecipient!=null) "새로운 상환 거래" else "받는 분과 금액을 확인해 주세요")
        Gap(18);Amount(d.amount,"review_amount",42)
        Gap(12);Title(if(d.recipient==Fixtures.official) d.recipient.name else "${d.recipient.name}님에게",24)
        Gap(32);Rule();Gap(12)
        DetailRow("받는 계좌",d.recipient.displayAccount)
        DetailRow("출금 계좌","새온 생활통장")
        DetailRow("송금 목적",purposeLabel(d.purpose))
        DetailRow("수수료","0원")
        Gap(18)
        if(d.previousRecipient!=null) QuietNotice("받는 곳이 ${d.previousRecipient}님에서 공식 상환 계좌로 바뀌었어요. 이전 확인은 재사용하지 않습니다.","bank",true)
        else if(!d.recipient.saved) QuietNotice("처음 보내는 계좌예요. 이름과 계좌를 다시 확인해 주세요.","profile")
        else QuietNotice("예금주가 같아도 송금 요청의 목적까지 확인해 주세요.","shield")
        Gap(16)
        if(d.route==null) Secondary("금액 또는 목적 수정","review_edit"){vm.editDraft()}
    }
    authRequest?.let { request ->
        TraceSheet("이 송금을 확인할까요?",{authRequest=null},"auth_sheet") {
            Amount(d.amount,size=32);Gap(10);Copy(d.recipient.name+" · "+d.recipient.bank,size=15,color=TraceColors.Ink)
            Gap(20);QuietNotice("새온은행은 대회용 가상 은행입니다. 확인 후 TRACE 정책을 평가하며, 실제 계좌로 송금하지 않습니다.")
            Gap(24)
            if(options.biometric && canAuthenticate()) {
                Primary("기기 인증으로 확인","auth_biometric") {
                    authRequest=null
                    authenticate({vm.confirm(request.first,request.second)},{vm.error.value=it})
                }
                Secondary("시연 확인으로 진행","auth_demo"){authRequest=null;vm.confirm(request.first,request.second)}
            } else Primary("시연 확인","auth_demo"){authRequest=null;vm.confirm(request.first,request.second)}
            Secondary("취소","auth_cancel"){authRequest=null}
        }
    }
}

@Composable fun TransactionSummary(d: TransferDraft, label: String="보내려던 금액") {
    Column(Modifier.fillMaxWidth().border(BorderStroke(1.dp,TraceColors.Line),RoundedCornerShape(14.dp)).background(TraceColors.Surface,RoundedCornerShape(14.dp)).padding(20.dp)) {
        Eyebrow(label);Gap(10);Amount(d.amount,size=34);Gap(10)
        Copy(d.recipient.name+" · "+d.recipient.bank,size=13)
        Copy(d.recipient.account,size=12)
    }
}

@Composable fun ReasonRows(state: BankState, includeDetails: Boolean=false) {
    val titles=when {
        state.heldCase || state.scenario==Scenario.IMPERSONATION -> listOf("기관 사칭 정황" to "기관·안전계좌를 언급하며 자금 이동을 요구한 정황이 있어요.","즉시 송금 요구" to "독립적으로 확인할 시간 없이 결정을 재촉했어요.","새로운 수취인" to "이 요청 뒤에 처음 보내는 계좌로 송금하려 했어요.")
        state.scenario in setOf(Scenario.LOAN,Scenario.LOOKUP_FAILURE) -> listOf("상환 목적과 받는 곳 불일치" to "대출을 갚는다고 했지만, 처음 안내받은 곳은 개인 계좌예요.")
        else -> listOf("확인되지 않은 링크" to "사용자가 공유한 내용에 링크가 있었어요. 실제 링크 접속 여부는 수집하지 않습니다.")
    }
    titles.forEachIndexed { i,(title,body) ->
        Row(Modifier.fillMaxWidth().padding(vertical=if(includeDetails) 16.dp else 10.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.Top) {
            Copy((i+1).toString().padStart(2,'0'),size=11,color=TraceColors.Faint)
            Column(Modifier.weight(1f)) { Copy(title,size=15,color=TraceColors.Ink,weight=FontWeight.Medium);if(includeDetails){Gap(6);Copy(body,size=14)} }
            if(!includeDetails) Glyph("check",TraceColors.Action,17)
        }
        if(includeDetails && i<titles.lastIndex) Rule()
    }
}

@Composable fun ResultScreen(state: BankState, options: AppOptions, busy: Boolean, vm: BankViewModel, go: (String)->Unit, back: ()->Unit) {
    val d=state.draft
    var reasons by remember { mutableStateOf(false) }
    var cancel by remember { mutableStateOf(false) }
    val haptic=LocalHapticFeedback.current
    LaunchedEffect(d?.phase) { if(options.haptics && d?.phase in setOf(Phase.HOLD,Phase.COMPLETE)) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    if(d==null) { Screen("송금",onBack=back){Title("진행 중인 송금이 없어요",24);Gap(24);Primary("홈으로"){go("home")}};return }
    val phase=d.phase
    val tag=when(phase){Phase.HOLD->"trace_hold";Phase.WARN->"trace_warn";Phase.VERIFY->"trace_verify";Phase.ROUTE->"trace_official_route";Phase.UNKNOWN->"trace_unknown";Phase.COMPLETE->"transfer_complete";Phase.EVALUATING->"trace_evaluating";else->"transfer_result"}
    val footer: @Composable ColumnScope.()->Unit = {
        when(phase) {
            Phase.HOLD -> {Primary("안전하게 확인하기","trace_safety_action"){go("guide")};Secondary("송금 취소","hold_cancel"){cancel=true}}
            Phase.WARN -> {Primary("받는 분과 금액 다시 확인","warn_review"){vm.acknowledge()};Secondary("송금 취소"){cancel=true}}
            Phase.VERIFY -> {Primary("공식 상환 경로 확인","route_lookup",!busy){vm.routeLookup()};Secondary("조회가 안 된다면?","route_fail"){vm.routeLookup(true)}}
            Phase.UNKNOWN -> {Primary("다시 확인하기","route_retry",!busy){vm.routeLookup()};Secondary("은행의 독립된 경로로 확인"){go("guide")}}
            Phase.ROUTE -> {Primary("새 송금 내역 확인","route_accept",!busy){vm.acceptRoute()};Secondary("송금 취소"){cancel=true}}
            Phase.COMPLETE -> {Primary("확인","complete_home"){go("home")};Secondary("송금 내역","complete_history"){go("history")}}
            Phase.ERROR -> {Primary("내역 다시 확인","result_retry"){vm.editDraft()};Secondary("홈으로"){go("home")}}
            Phase.REVIEW,Phase.AMOUNT -> Primary("거래 다시 확인"){go(if(phase==Phase.AMOUNT) "amount" else "review")}
            Phase.CANCELLED -> Primary("홈으로"){go("home")}
            Phase.EVALUATING -> Copy("거래 정보를 확인하는 동안 송금은 실행되지 않습니다.",size=12)
        }
    }
    Screen(if(phase==Phase.COMPLETE) "송금 완료" else "TRACE 안전 확인",tag=tag,onBack={go("home")},footer=footer) {
        when(phase) {
            Phase.HOLD -> {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){TraceMark(Modifier.size(20.dp));Eyebrow("송금 보류",true)}
                Gap(18);Title(if(options.easy) "송금을 잠시\n멈췄습니다" else "잠깐,\n확인하고 보내볼까요?",29)
                Gap(12);Copy("아직 돈은 나가지 않았습니다.",color=TraceColors.Ink,weight=FontWeight.Medium)
                Gap(6);Copy("방금 전의 요청이\n이 송금과 이어져 있어요.",size=14)
                Gap(24);TransactionSummary(d,"처음 보내는 계좌")
                Gap(18)
                if(!options.easy) ReasonRows(state)
                ListRow("어떤 이유가 연결됐나요?",tag="trace_hold_reason",onClick={reasons=true})
                Copy("이 화면을 닫아도 송금되지 않습니다.",size=12)
            }
            Phase.WARN -> {
                Eyebrow("추가 확인",true);Gap(18)
                Title("보내기 전에\n하나만 확인해 주세요")
                Gap(14);Copy("공유된 미확인 링크와 이 송금이 가까운 시간에 이어졌어요. 받는 분과 요청의 목적을 다시 살펴보세요.")
                Gap(24);TransactionSummary(d);Gap(24)
                QuietNotice("한 번 더 확인할 수 있는 상태입니다. 기관 사칭 등 여러 위험이 연결된 송금 보류와는 다릅니다.","info")
                Gap(14);ListRow("연결된 흐름 보기",tag="warn_timeline",onClick={go("timeline")})
            }
            Phase.VERIFY,Phase.UNKNOWN -> {
                Eyebrow(if(phase==Phase.UNKNOWN) "공식 경로 미확인" else "송금 목적 확인",true);Gap(20)
                Title(if(phase==Phase.UNKNOWN) "확인할 수 없으면,\n보내지 않습니다" else "대출을 갚는 돈이\n맞나요?")
                Gap(14);Copy(if(phase==Phase.UNKNOWN) "공식 상환 경로를 확인하지 못했어요. 확인되지 않았다는 건 안전하다는 뜻이 아닙니다." else "안내받은 곳은 개인 계좌예요.\n은행의 공식 상환 경로와 확인이 필요해요.")
                Gap(24);TransactionSummary(d,"안내받은 개인 계좌")
                Gap(24);Rule();Gap(12)
                DetailRow("송금 목적","대출 상환")
                DetailRow("확인된 상환 계좌","아직 확인되지 않음",true)
                Gap(16);QuietNotice("돈은 나가지 않았습니다. 일반 송금으로 자동 전환하지 않습니다.","pause")
            }
            Phase.ROUTE -> {
                Eyebrow("등록된 상환 경로",true);Gap(18);Title("보내는 목적에 맞는\n받는 곳을 찾았어요")
                Gap(14);Copy("새온은행의 가상 등록 대출에서 확인한 상환 경로입니다.")
                Gap(26)
                Column(Modifier.fillMaxWidth().background(TraceColors.Paper,RoundedCornerShape(14.dp)).padding(20.dp)) {
                    Glyph("bank",TraceColors.Action,26);Gap(16);Copy(Fixtures.official.name,size=17,color=TraceColors.Ink,weight=FontWeight.SemiBold)
                    Gap(8);Copy(Fixtures.official.displayAccount,size=13);Gap(14);Rule();Gap(8);Copy("생활안심대출 · 등록된 상환 계좌",size=12)
                }
                Gap(18);DetailRow("이전 받는 분",d.recipient.name+" · 개인 계좌")
                DetailRow("확인할 금액",money(d.amount)+"원")
                Gap(20);QuietNotice("아직 송금하지 않았어요. 새로운 수취인과 금액을 다시 확인한 뒤 새 거래로 진행합니다.","shield",true)
                Gap(14);Copy("은행 응답은 기기 안의 시연 데이터입니다.",size=12)
            }
            Phase.COMPLETE -> {
                val r=state.receipts.firstOrNull{it.transactionId==d.id}
                Gap(14);Glyph("check",TraceColors.Action,40);Gap(28)
                Title(if(d.purpose==Purpose.REPAYMENT) "공식 경로로\n상환을 마쳤어요" else "${d.recipient.name}님에게\n보냈어요")
                Gap(22);Amount(d.amount,"complete_amount",42)
                Gap(26);Rule();Gap(12)
                DetailRow("받는 분",d.recipient.name)
                DetailRow("받는 계좌",d.recipient.displayAccount)
                DetailRow("남은 잔액",money(state.balance)+"원")
                if(r!=null){DetailRow("거래 일시",dateLabel(r.at));DetailRow("거래 번호",r.id)}
                Gap(22);QuietNotice("시연용 가상 거래입니다. 실제 자금은 이동하지 않습니다.","receipt")
            }
            Phase.EVALUATING -> {
                Gap(28);TraceMark(Modifier.size(32.dp));Gap(24);Title("송금 앞의 맥락을\n확인하고 있어요")
                Gap(24)
                listOf("요청의 목적","최근 위험 신호","수취인과 거래").forEachIndexed { i,t -> ListRow(t,subtitle="0${i+1}");Rule() }
                Gap(20);Copy("원문이 아닌 필요한 신호만 확인합니다.",size=13)
            }
            Phase.ERROR -> {Eyebrow("거래 미실행",true);Gap(18);Title("확인을 마치지\n못했어요");Gap(16);Copy(d.error ?: "거래 정보를 다시 확인해 주세요.")}
            Phase.CANCELLED -> {Title("송금을 취소했어요");Gap(16);Copy("돈은 나가지 않았습니다. 위험 맥락은 별도로 유지됩니다.")}
            else -> {Title("송금 내역을\n다시 확인해 주세요");Gap(16);Copy("앱이 중단됐거나 화면을 다시 열었어요. 자동으로 송금하지 않았습니다.")}
        }
    }
    if(reasons) TraceSheet("멈춘 이유",{reasons=false},"hold_reason_sheet") {
        Copy("따로 있던 단서가 지금의 송금과 연결됐어요.");Gap(12);ReasonRows(state,true);Gap(20)
        Primary("전체 흐름 보기","reason_timeline",neutral=true){reasons=false;go("timeline")}
        Secondary("보류 화면으로 돌아가기"){reasons=false}
    }
    if(cancel) TraceSheet("이 송금을 취소할까요?",{cancel=false},"cancel_sheet") {
        Copy("돈은 나가지 않습니다. 이미 연결된 위험 맥락은 지워지지 않습니다.");Gap(24)
        Primary("송금 취소 확인","cancel_execute"){cancel=false;vm.cancelTransfer()}
        Secondary("돌아가기"){cancel=false}
    }
}
