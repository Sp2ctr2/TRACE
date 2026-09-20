package app.saeon.trace.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.saeon.trace.core.*
import app.saeon.trace.data.*
import app.saeon.trace.ui.*
import app.saeon.trace.ui.design.*

@Composable fun QuietTransfer(state: BankState, preferences: BankPreferences, interaction: InteractionState,
    model: BankViewModel, open: (String) -> Unit, back: () -> Unit, home: () -> Unit) {
    val record = state.current
    if (record == null) { TransferStateScreen(state, preferences, interaction, model, open, back, home); return }
    when (record.stage) {
        TransferStage.REVIEW, TransferStage.AUTHORIZING -> QuietReview(state, record, preferences, interaction, model, open, back)
        TransferStage.HOLD -> QuietHold(record, preferences, model, open, back, home)
        TransferStage.VERIFY -> QuietVerify(record, interaction, model, back, home)
        TransferStage.UNKNOWN -> QuietUnknown(record, interaction, model, back, home)
        TransferStage.ROUTE -> QuietRoute(record, interaction, model, back)
        else -> TransferStateScreen(state, preferences, interaction, model, open, back, home)
    }
}

@Composable private fun TransactionSummary(record: TransferRecord) {
    SurfaceBox {
        Caption(if (record.intent.recipient.known) "저장된 수취인" else "처음 보내는 계좌")
        Space(10); Money(record.intent.amount)
        Space(9); Caption("${record.intent.recipient.bank} · ${record.intent.recipient.name}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun QuietHold(record: TransferRecord, preferences: BankPreferences, model: BankViewModel,
    open: (String) -> Unit, back: () -> Unit, home: () -> Unit) {
    var sheet by rememberSaveable(record.intent.id) { mutableStateOf(false) }
    val reasons = listOf(RiskType.IMPERSONATION, RiskType.URGENCY, RiskType.NEW_RECIPIENT,
        RiskType.SUSPICIOUS_LINK, RiskType.FINANCIAL_INSTRUCTION).filter { it in record.reasons }
    Page(title = "송금 보류", tag = "trace_hold", back = back, footer = {
        PrimaryButton(when { preferences.childMode -> "믿을 수 있는 어른과 확인"; preferences.easyMode -> "공식 경로로 확인"; else -> "안전하게 확인하기" },
            Modifier.testTag("hold_safe_action")) { open("safety_guide") }
        SecondaryButton("송금 취소", Modifier.testTag("hold_cancel")) { model.cancelTransfer(record.intent.id, home) }
    }) {
        Space(4); TraceSignature(); Space(22)
        Headline(when { preferences.childMode -> "지금은 돈을\n보내지 마세요."; preferences.easyMode -> "송금을 잠시\n멈췄습니다."; else -> "잠깐,\n확인하고 보내볼까요?" },
            if (preferences.easyMode) Modifier.testTag("easy_mode") else Modifier)
        Space(14)
        Body(if (preferences.childMode) "아직 돈은 나가지 않았어요." else "아직 돈은 나가지 않았습니다.")
        if (!preferences.easyMode) { Space(5); Caption("방금 전의 요청이 이 송금과 이어져 있어요.") }
        Space(24); TransactionSummary(record); Space(14)
        if (preferences.easyMode) {
            Space(8)
            Body(if (preferences.childMode) "기관 직원인 척하며 돈을 보내라고 했을 수 있어요. 보호자나 선생님에게 이 화면을 보여 주세요." else "상대가 기관을 사칭했을 가능성이 있습니다. 상대가 준 번호나 링크로 확인하지 마세요.")
            Space(18); Caption("화면을 닫아도 자동으로 보내지 않습니다.")
        } else {
            reasons.take(3).forEachIndexed { index, reason ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { sheet = true }
                    .then(if (index == 0) Modifier.testTag("hold_reasons_open") else Modifier)
                    .padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("0${index + 1}", style = MaterialTheme.typography.labelSmall, color = TraceColors.Muted)
                    Text(reason.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    AppIcon(BankIcons.Check, size = 16, tint = TraceColors.CoralText)
                }
            }
            Space(12); Caption("이유를 누르면 연결된 정황을 볼 수 있어요.")
        }
    }
    if (sheet) ModalBottomSheet(onDismissRequest = { sheet = false }, containerColor = TraceColors.Surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
            .padding(bottom = 24.dp).testTag("trace_hold_reason")) {
            Text("멈춘 이유", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Space(12); Body("이 송금 앞에 다음 정황이 이어졌어요.", subdued = true); Space(14)
            reasons.forEachIndexed { index, reason -> NumberedReason(index + 1, reason.label, reason.explanation) }
            MenuRow("시간 흐름 보기", tag = "hold_timeline_open") { sheet = false; open("timeline") }
            Space(12); PrimaryButton("확인", Modifier.testTag("hold_reasons_close")) { sheet = false }
        }
    }
}

@Composable private fun QuietVerify(record: TransferRecord, interaction: InteractionState, model: BankViewModel, back: () -> Unit, home: () -> Unit) {
    Page(title = "상환 경로 확인", tag = "trace_verify", back = back, footer = {
        PrimaryButton(if (interaction.routeLoading) "공식 경로를 확인하고 있어요." else "공식 상환 경로 확인", Modifier.testTag("verify_route"), enabled = !interaction.busy) { model.resolveRoute(record.intent.id) }
        SecondaryButton("송금 취소") { model.cancelTransfer(record.intent.id, home) }
    }) {
        Space(4); TraceSignature(); Space(22); Headline("대출을 갚는\n돈이 맞나요?"); Space(14)
        Body("아직 돈은 나가지 않았습니다."); Space(6)
        Caption("안내받은 계좌는 공식 상환처로 확인되지 않았어요.")
        Space(24); TransactionSummary(record); Space(20)
        NumberedReason(1, "보내는 목적은 대출 상환")
        NumberedReason(2, if (record.intent.recipient.kind == RecipientKind.PERSON) "받는 곳은 개인 계좌" else "상환 계좌인지 다시 확인 필요")
        Space(16); Caption("상대가 알려준 경로가 아닌, 은행 앱에 등록된 상환 경로로 확인합니다.")
    }
}

@Composable private fun QuietUnknown(record: TransferRecord, interaction: InteractionState, model: BankViewModel, back: () -> Unit, home: () -> Unit) {
    Page(title = "공식 경로 확인", tag = "trace_unknown", back = back, footer = {
        PrimaryButton(if (interaction.routeLoading) "다시 확인하고 있어요." else "공식 경로 다시 확인", Modifier.testTag("unknown_retry"), enabled = !interaction.busy) { model.resolveRoute(record.intent.id) }
        SecondaryButton("송금 취소") { model.cancelTransfer(record.intent.id, home) }
    }) {
        Space(4); TraceSignature(); Space(22); Headline("확인할 수 없으면,\n보내지 않습니다."); Space(14)
        Body("아직 돈은 나가지 않았습니다."); Space(24); TransactionSummary(record); Space(22)
        Body("공식 경로를 확인하지 못했어요. 확인되지 않았다는 건 안전하다는 뜻이 아닙니다.", subdued = true)
        Space(18); Caption("잔액은 그대로이며, 송금 완료 내역도 만들지 않았습니다.")
    }
}

@Composable private fun QuietRoute(record: TransferRecord, interaction: InteractionState, model: BankViewModel, back: () -> Unit) {
    val route = record.route
    Page(title = "확인된 상환 경로", tag = "trace_official_route", back = back, footer = {
        PrimaryButton("새 송금 내역 확인", Modifier.testTag("official_route_use"), enabled = route != null && !interaction.busy) { model.useRoute(record.intent.id) }
        QuietButton("공식 경로 다시 확인", Modifier.fillMaxWidth().testTag("official_route_refresh"), enabled = !interaction.busy) { model.resolveRoute(record.intent.id) }
    }) {
        Space(4); TraceSignature(); Space(22); Headline("상환할 곳을\n확인했어요."); Space(14); Body("아직 송금하지 않았습니다.")
        Space(24)
        if (route != null) SurfaceBox {
            Caption(route.productName); Space(12)
            Text(route.recipient.name, style = MaterialTheme.typography.titleMedium)
            Space(8); Body("${route.recipient.bank}\n${route.recipient.account}"); Space(18); Money(record.intent.amount)
        }
        Space(22); Body("받는 분이 바뀌었으므로 새로운 송금입니다. 내역을 다시 보고 새로 인증해 주세요.")
        Space(18); Caption("등록된 가상 경로를 확인한 시연 결과입니다.")
    }
}

@Composable fun QuietSafety(state: BankState, model: BankViewModel, open: (String) -> Unit) {
    val now = model.repository.clock.now()
    val active = state.events.filter { it.active(now) }.sortedBy { it.createdAt }
    Page(title = "안전 센터", tag = "safety_center") {
        Space(8); TraceSignature(); Space(20)
        Text(if (state.pending.isEmpty()) "확인을 기다리는 송금이 없어요." else "송금 ${state.pending.size}건이 확인을 기다려요.", style = MaterialTheme.typography.titleMedium)
        if (state.pending.isNotEmpty()) {
            Space(8); Caption("아직 돈은 나가지 않았습니다.")
            state.pending.take(1).forEach { r ->
                MenuRow("${r.intent.recipient.name} · ${won(r.intent.amount)}원", "이유와 확인할 방법 보기", tag = "pending_${r.intent.id}") {
                    model.resume(r.intent.id) { open("transfer_state") }
                }
            }
        }
        Space(24); Rule(); Space(14)
        if (active.isNotEmpty()) {
            SectionTitle("최근 맥락", "전체 흐름") { open("timeline") }
            active.takeLast(3).forEach { event ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Caption(timeLabel(event.createdAt)); Text(event.type.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Space(20); Rule(); Space(8)
        }
        MenuRow("받은 내용 직접 확인", tag = "manual_check_open") { open("manual") }
        MenuRow("보류 내역") { open("pending") }
        MenuRow("안전하게 확인하는 방법") { open("safety_guide") }
        Space(14); Rule(); Space(8)
        MenuRow("개인정보와 데이터 경계") { open("privacy") }
        MenuRow("큰 글씨·어린이 화면") { open("accessibility") }
        MenuRow("시연 센터", tag = "safety_demo_center") { open("demo_center") }
    }
}

@Composable fun QuietGuide(state: BankState, preferences: BankPreferences, model: BankViewModel, open: (String) -> Unit, back: () -> Unit) {
    val record = state.current?.takeIf { it.stage in setOf(TransferStage.HOLD, TransferStage.WARN, TransferStage.VERIFY, TransferStage.UNKNOWN, TransferStage.ROUTE) }
    if (!preferences.easyMode) { SafetyGuideScreen(state, open, back); return }
    var step by rememberSaveable(record?.intent?.id, preferences.readingMode) { mutableIntStateOf(0) }
    val child = preferences.childMode
    val titles = if (child) listOf("잠깐, 멈춰 주세요.", "믿을 수 있는\n어른에게 보여 주세요.", "함께 확인하기 전에는\n돈을 보내지 마세요.")
        else listOf("상대가 준 번호나\n링크는 쓰지 마세요.", "은행 앱을\n직접 열어 주세요.", "확인이 끝나기 전에는\n보내지 마세요.")
    val descriptions = if (child) listOf("돈을 보내라고 재촉해도 괜찮아요. 지금은 보내지 않아도 돼요.",
        "평소 알고 지내는 보호자나 선생님에게 이 화면을 보여 주세요. 돈을 요구한 사람에게 확인하지 마세요.",
        "이 앱이 어른에게 연락하거나 허락을 받은 것은 아니에요. 직접 함께 확인해 주세요.")
        else listOf("통화 중이라면 먼저 전화를 끊어도 됩니다. 상대가 보내 준 연락처로 확인하지 마세요.",
            "평소 쓰던 은행 앱의 고객센터나 대출 상환 메뉴에서 직접 확인하세요.", "확인이 안 되면 송금을 취소하세요. 화면을 닫아도 돈은 자동으로 나가지 않습니다.")
    val goBack: () -> Unit = { if (step > 0) { step -= 1 } else back() }
    BackHandler(onBack = goBack)
    Page(title = if (child) "함께 확인하기" else "안전하게 확인하기", tag = "trace_safety_guide", back = goBack, footer = {
        PrimaryButton(if (step < 2) "다음" else if (child) "안전 센터로" else "은행 앱에서 확인하기", Modifier.testTag("safety_official_channel")) {
            if (step < 2) step++ else open(if (child) "safety" else "support")
        }
        if (record != null) SecondaryButton("송금 취소") { model.cancelTransfer(record.intent.id) { open("home") } }
    }) {
        Space(16); Caption("${step + 1} / 3"); Space(22); Headline(titles[step]); Space(24)
        Body(descriptions[step]); Space(28); Rule(); Space(20)
        if (record != null) Body(if (child) "아직 돈은 나가지 않았어요." else "아직 돈은 나가지 않았습니다.")
        else Caption("이 화면에서는 송금을 실행하지 않습니다.")
        if (record != null) { Space(14); Money(record.intent.amount, hero = false); Space(8); Caption("${record.intent.recipient.name}님에게 보내려던 돈") }
    }
}
