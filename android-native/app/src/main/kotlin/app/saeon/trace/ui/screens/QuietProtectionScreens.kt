package app.saeon.trace.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
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
        TransferStage.WARN -> QuietWarn(record, interaction, model, back, home)
        TransferStage.VERIFY -> QuietVerify(record, interaction, model, back, home)
        TransferStage.UNKNOWN -> QuietUnknown(record, interaction, model, back, home)
        TransferStage.ROUTE -> QuietRoute(record, interaction, model, back)
        TransferStage.EVALUATING -> {
            val cancel: () -> Unit = { model.cancelAuthorization(); back() }
            BackHandler(onBack = cancel)
            Page(title = "송금 확인", tag = "trace_evaluating", footer = { SecondaryButton("취소하고 돌아가기", onClick = cancel) }) {
                Space(26); TraceSignature(); Space(26); Headline("송금 앞의 맥락을\n확인하고 있어요.")
                Space(18); Caption("인증한 거래와 현재의 정황을 함께 확인합니다.")
                Space(28); NumberedReason(1, "요청의 목적"); Rule(); NumberedReason(2, "최근 위험 신호"); Rule(); NumberedReason(3, "수취인과 거래")
            }
        }
        TransferStage.COMPLETE -> QuietComplete(state, record, open, home)
        else -> TransferStateScreen(state, preferences, interaction, model, open, back, home)
    }
}

@Composable private fun TransactionSummary(record: TransferRecord) {
    SurfaceBox {
        Caption(when { record.intent.officialRouteId != null -> "확인된 상환처"; record.intent.recipient.known -> "저장된 수취인"; else -> "처음 보내는 계좌" })
        Space(8); Money(record.intent.amount, hero = false); Space(8)
        Text("${record.intent.recipient.bank} · ${record.intent.recipient.name}", style = MaterialTheme.typography.bodyMedium)
        Space(4); Caption(record.intent.recipient.account)
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
        Space(8); TraceSignature(); Space(24)
        Headline(when { preferences.childMode -> "지금은 돈을\n보내지 마세요."; preferences.easyMode -> "송금을 잠시\n멈췄습니다."; else -> "잠깐,\n확인하고 보내볼까요?" },
            if (preferences.easyMode) Modifier.testTag("easy_mode") else Modifier)
        Space(16)
        Text(if (preferences.childMode) "아직 돈은 나가지 않았어요." else "아직 돈은 나가지 않았습니다.",
            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        if (!preferences.easyMode) { Space(6); Caption("방금 전의 요청이\n이 송금과 이어져 있어요.") }
        Space(24); TransactionSummary(record); Space(16)
        if (preferences.easyMode) {
            Space(8); Body(if (preferences.childMode) "기관 직원인 척하며 돈을 요구했을 수 있어요. 보호자나 선생님에게 보여 주세요."
                else "상대가 기관을 사칭했을 가능성이 있습니다. 상대가 준 번호나 링크는 쓰지 마세요.")
            Space(16); Caption("화면을 닫아도 자동으로 보내지 않습니다.")
        } else {
            reasons.take(3).forEachIndexed { index, reason ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { sheet = true }
                    .then(if (index == 0) Modifier.testTag("hold_reasons_open") else Modifier)
                    .padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("0${index + 1}", style = MaterialTheme.typography.labelSmall, color = TraceColors.Muted)
                    Text(reason.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    AppIcon(BankIcons.Check, size = 15, tint = TraceColors.CoralText)
                }
            }
            Space(14); Caption("화면을 닫아도 자동으로 송금되지 않습니다.")
        }
    }
    if (sheet) ModalBottomSheet(onDismissRequest = { sheet = false }, containerColor = TraceColors.Surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
            .padding(bottom = 24.dp).testTag("trace_hold_reason")) {
            Text("멈춘 이유", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Space(10); Caption("이 송금 앞에 이어진 정황입니다."); Space(14)
            reasons.forEachIndexed { index, reason -> NumberedReason(index + 1, reason.label, reason.explanation) }
            Space(8); Rule(); MenuRow("시간 흐름 보기", tag = "hold_timeline_open") { sheet = false; open("timeline") }
            Space(12); PrimaryButton("확인", Modifier.testTag("hold_reasons_close")) { sheet = false }
        }
    }
}

@Composable private fun QuietWarn(record: TransferRecord, interaction: InteractionState, model: BankViewModel, back: () -> Unit, home: () -> Unit) {
    var checked by rememberSaveable(record.intent.id) { mutableStateOf(false) }
    Page(title = "보내기 전 확인", tag = "trace_warn", back = back, footer = {
        PrimaryButton("확인한 내용으로 다시 보기", Modifier.testTag("warn_acknowledge"), enabled = checked && !interaction.busy) { model.acknowledge(record.intent.id) }
        SecondaryButton("송금 취소") { model.cancelTransfer(record.intent.id, home) }
    }) {
        Space(8); TraceSignature(); Space(24); Headline("보내기 전에\n하나만 확인해 주세요.")
        Space(16); Body("아직 돈은 나가지 않았습니다.")
        Space(8); Caption("확인하지 않은 링크와 송금 요청이\n가까운 시간 안에 이어졌어요.")
        Space(24); TransactionSummary(record); Space(24)
        Text("상대가 준 링크 말고,\n알고 있던 연락처로 확인하세요.", style = MaterialTheme.typography.titleSmall)
        Space(16); Rule()
        Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).toggleable(checked, role = Role.Checkbox, onValueChange = { checked = it })
            .testTag("warn_check").padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked, null, modifier = Modifier.clearAndSetSemantics {}, colors = CheckboxDefaults.colors(checkedColor = TraceColors.Ink))
            Text("다른 경로로 받는 분과 금액을 확인했어요.", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        }
        Space(8); Caption("확인 뒤 새로 인증합니다. 새 위험 신호가 있으면 다시 멈출 수 있어요.")
    }
}

@Composable private fun QuietVerify(record: TransferRecord, interaction: InteractionState, model: BankViewModel, back: () -> Unit, home: () -> Unit) {
    Page(title = "상환 경로 확인", tag = "trace_verify", back = back, footer = {
        PrimaryButton(if (interaction.routeLoading) "공식 경로를 확인하고 있어요." else "공식 상환 경로 확인", Modifier.testTag("verify_route"), enabled = !interaction.busy) { model.resolveRoute(record.intent.id) }
        SecondaryButton("송금 취소") { model.cancelTransfer(record.intent.id, home) }
    }) {
        Space(8); TraceSignature(); Space(24); Headline("대출을 갚는\n돈이 맞나요?")
        Space(16); Body("아직 돈은 나가지 않았습니다.")
        Space(8); Caption("안내받은 계좌는 공식 상환처로\n확인되지 않았어요.")
        Space(24); TransactionSummary(record); Space(24)
        DetailRow("요청의 목적", "대출 상환")
        DetailRow("받는 곳", if (record.intent.recipient.kind == RecipientKind.PERSON) "개인 계좌" else "상환처 확인 필요")
        Space(14); Rule(); Space(18)
        Caption("상대가 알려준 번호나 링크가 아닌,\n은행 앱에 등록된 경로에서 확인합니다.")
    }
}
@Composable private fun QuietUnknown(record: TransferRecord, interaction: InteractionState, model: BankViewModel, back: () -> Unit, home: () -> Unit) {
    Page(title = "공식 경로 확인", tag = "trace_unknown", back = back, footer = {
        PrimaryButton(if (interaction.routeLoading) "다시 확인하고 있어요." else "공식 경로 다시 확인", Modifier.testTag("unknown_retry"), enabled = !interaction.busy) { model.resolveRoute(record.intent.id) }
        SecondaryButton("송금 취소") { model.cancelTransfer(record.intent.id, home) }
    }) {
        Space(8); TraceSignature(); Space(24); Headline("확인할 수 없으면,\n보내지 않습니다.")
        Space(16); Body("아직 돈은 나가지 않았습니다.")
        Space(24); TransactionSummary(record); Space(24)
        Body("공식 경로를 확인하지 못했어요.\n확인되지 않았다는 건\n안전하다는 뜻이 아닙니다.", subdued = true)
        Space(20); Rule(); Space(18); Caption("잔액은 그대로입니다.\n완료 내역을 만들거나 일반 송금으로 전환하지 않습니다.")
    }
}
@Composable private fun QuietRoute(record: TransferRecord, interaction: InteractionState, model: BankViewModel, back: () -> Unit) {
    val route = record.route
    Page(title = "확인된 상환 경로", tag = "trace_official_route", back = back, footer = {
        PrimaryButton("새 송금 내역 확인", Modifier.testTag("official_route_use"), enabled = route != null && !interaction.busy) { model.useRoute(record.intent.id) }
        QuietButton("공식 경로 다시 확인", Modifier.fillMaxWidth().testTag("official_route_refresh"), enabled = !interaction.busy) { model.resolveRoute(record.intent.id) }
    }) {
        Space(8); TraceSignature(); Space(24); Headline("상환할 곳을\n확인했어요.")
        Space(16); Body("아직 송금하지 않았습니다."); Space(24)
        if (route != null) SurfaceBox {
            Caption(route.productName); Space(12)
            Text(route.recipient.name, style = MaterialTheme.typography.titleMedium)
            Space(8); Caption("${route.recipient.bank} · ${route.recipient.account}")
            Space(20); Money(record.intent.amount, hero = false)
        }
        Space(24); Text("받는 분이 바뀌면, 새로운 송금입니다.", style = MaterialTheme.typography.titleSmall)
        Space(8); Body("내역을 다시 보고 새로 인증해 주세요.\n이전 계좌로는 보내지 않습니다.", subdued = true)
        Space(20); Caption("은행 앱에 등록된 가상 경로의 시연 결과입니다.")
    }
}
@Composable private fun QuietComplete(state: BankState, record: TransferRecord, open: (String) -> Unit, home: () -> Unit) {
    val receipt = state.receipts.find { it.intentId == record.intent.id && !it.seed }
    if (receipt == null) {
        Page(title = "송금 결과", footer = { PrimaryButton("거래 내역 확인") { open("history") } }) {
            EmptyState("완료 내역을 다시 확인해 주세요.", "다시 보내지 말고 거래 내역을 확인해 주세요.")
        }
        return
    }
    Page(tag = "transfer_complete", footer = {
        PrimaryButton("확인", Modifier.testTag("complete_confirm"), onClick = home)
        SecondaryButton("송금 내역") { open("receipt/${receipt.id}") }
    }) {
        Space(30); AppIcon(BankIcons.Check, size = 32); Space(28); Money(receipt.amount)
        Space(12); Text(if (receipt.purpose == Purpose.LOAN) "공식 경로로\n상환을 마쳤어요." else "${receipt.recipient.name}님에게\n보냈어요.",
            style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Space(28); Rule(); Space(10)
        DetailRow("받는 분", receipt.recipient.name)
        DetailRow("받는 계좌", "${receipt.recipient.bank}\n${receipt.recipient.account}")
        DetailRow("출금 계좌", receipt.fromAccount)
        DetailRow("보낸 금액", "${won(receipt.amount)}원", true)
        DetailRow("수수료", "0원"); DetailRow("시간", dateLabel(receipt.completedAt))
        Space(22); Caption(receipt.id); Space(6); SimulationNote()
    }
}

@Composable fun QuietSafety(state: BankState, model: BankViewModel, open: (String) -> Unit) {
    val active = state.events.filter { it.active(model.repository.clock.now()) }.sortedBy { it.createdAt }
    Page(title = "안전 센터", tag = "safety_center") {
        Space(10); TraceSignature(); Space(22)
        Text(if (state.pending.isEmpty()) "확인을 기다리는 송금이 없어요." else "송금 ${state.pending.size}건이 확인을 기다려요.", style = MaterialTheme.typography.titleMedium)
        Space(8); Caption(if (state.pending.isEmpty()) "필요할 때만, 송금 앞의 맥락을 확인합니다." else "아직 돈은 나가지 않았습니다.")
        state.pending.take(1).forEach { record ->
            Space(8); MenuRow("${record.intent.recipient.name} · ${won(record.intent.amount)}원", "이유와 다음 행동 보기", tag = "pending_${record.intent.id}") {
                model.resume(record.intent.id) { open("transfer_state") }
            }
        }
        Space(24); Rule(); Space(16)
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
    var step by rememberSaveable(record?.intent?.id, preferences.readingMode) { mutableIntStateOf(0) }
    val child = preferences.childMode
    val titles = if (child) listOf("잠깐, 멈춰 주세요.", "믿을 수 있는\n어른에게 보여 주세요.", "함께 확인하기 전에는\n돈을 보내지 마세요.")
        else listOf("상대가 준 번호나\n링크는 쓰지 마세요.", "은행 앱을\n직접 열어 주세요.", "확인이 끝나기 전에는\n보내지 마세요.")
    val descriptions = if (child) listOf("서둘러 보내라고 해도, 지금은 보내지 않아도 돼요.",
        "평소 알고 지내는 보호자나 선생님에게 보여 주세요. 돈을 요구한 사람에게 확인하지 마세요.",
        "이 앱이 어른에게 연락하거나 허락을 받은 것은 아니에요. 직접 함께 확인해 주세요.")
        else listOf("통화 중이라면 먼저 끊어도 됩니다. 상대가 보내 준 연락처로 확인하지 마세요.",
            "평소 쓰던 은행 앱의 고객센터나 대출 상환 메뉴에서 직접 확인하세요.", "확인이 안 되면 송금을 취소하세요. 화면을 닫아도 돈은 자동으로 나가지 않습니다.")
    val goBack: () -> Unit = { if (step > 0) step -= 1 else back() }
    BackHandler(onBack = goBack)
    Page(title = if (child) "함께 확인하기" else "안전하게 확인하기", tag = "trace_safety_guide", back = goBack, footer = {
        PrimaryButton(if (step < 2) "다음" else if (child) "안전 센터로" else "은행 앱에서 확인하기", Modifier.testTag("safety_official_channel")) {
            if (step < 2) step++ else open(if (child) "safety" else "support")
        }
        if (record != null) SecondaryButton("송금 취소") { model.cancelTransfer(record.intent.id) { open("home") } }
    }) {
        Space(16)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { index -> Box(Modifier.weight(1f).height(2.dp).background(if (index <= step) TraceColors.Ink else TraceColors.Divider)) }
        }
        Space(20); Caption("${step + 1} / 3"); Space(24); Headline(titles[step]); Space(22)
        Body(descriptions[step]); Space(32); Rule(); Space(20)
        if (record != null) {
            Text(if (child) "아직 돈은 나가지 않았어요." else "아직 돈은 나가지 않았습니다.", style = MaterialTheme.typography.titleSmall)
            Space(12); Money(record.intent.amount, hero = false); Space(6); Caption("${record.intent.recipient.name}님에게 보내려던 돈")
        } else Caption("이 화면에서는 송금을 실행하지 않습니다.")
    }
}
