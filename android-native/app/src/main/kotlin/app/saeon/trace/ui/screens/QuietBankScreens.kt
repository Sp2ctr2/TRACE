package app.saeon.trace.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saeon.trace.core.*
import app.saeon.trace.data.*
import app.saeon.trace.ui.*
import app.saeon.trace.ui.design.*

/** Banking remains the shell; only intervention screens take over its hierarchy. */
@Composable fun QuietHome(state: BankState, preferences: BankPreferences, model: BankViewModel, open: (String) -> Unit) {
    Page(title = "새온은행", tag = "home", actions = {
        QuietButton("시연", Modifier.testTag("home_demo_center")) { open("demo_center") }
        IconAction(BankIcons.Bell, "알림") { open("notifications") }
    }) {
        Space(8)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(role = Role.Button) { open("account") }.heightIn(min = 56.dp),
                verticalArrangement = Arrangement.Center) {
                Text("새온 생활통장", style = MaterialTheme.typography.titleSmall)
                Space(5); Caption("새온은행 110-***-0001")
            }
            IconAction(BankIcons.Eye, if (preferences.hideBalance) "잔액 보이기" else "잔액 숨기기") {
                model.preference { hideBalance(!preferences.hideBalance) }
            }
        }
        Space(16)
        if (preferences.hideBalance) Text("잔액 숨김", Modifier.testTag("home_balance"), style = MaterialTheme.typography.headlineLarge)
        else Money(state.balance, Modifier.testTag("home_balance"))
        Space(24)
        if (preferences.easyMode) {
            PrimaryButton(if (preferences.childMode) "돈 보내기" else "송금하기", Modifier.testTag("home_transfer")) { open("transfer") }
            SecondaryButton("거래 내역 보기") { open("history") }
            Space(20); Rule(); Space(20)
            Body(if (preferences.childMode) "누가 돈을 보내라고 했나요?\n모르는 사람의 부탁은 어른과 먼저 확인해요." else "서둘러 보내라는 요청을 받았다면,\n먼저 상대와 보내는 이유를 확인하세요.")
        } else {
            if (LocalDensity.current.fontScale >= 1.5f) {
                listOf("transfer" to "송금", "bring" to "가져오기", "history" to "내역").forEachIndexed { index, (route, label) ->
                    MenuRow(label, tag = if (index == 0) "home_transfer" else "home_$route") { open(route) }
                }
            } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("transfer" to "송금", "bring" to "가져오기", "history" to "내역").forEachIndexed { index, (route, label) ->
                    TextButton(onClick = { open(route) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                        .testTag(if (index == 0) "home_transfer" else "home_$route"),
                        shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 14.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (index == 0) TraceColors.Deep else TraceColors.Paper,
                            contentColor = if (index == 0) TraceColors.White else TraceColors.Ink)) {
                        Text(label, Modifier.testTag("home_action_label_$route"), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            }
            Space(28); Rule(); Space(12)
            SectionTitle("최근 거래", "전체 내역") { open("history") }
            state.receipts.take(3).forEach { receipt ->
                if (preferences.hideBalance) {
                    MenuRow(receipt.recipient.name, "금액 숨김") { open("receipt/${receipt.id}") }
                } else ReceiptRow(receipt) { open("receipt/${receipt.id}") }
            }
        }
        Space(20); Rule()
        val active = state.events.any { it.active(model.repository.clock.now()) }
        Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(role = Role.Button) { open("safety") }
            .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(BankIcons.Trace, null, Modifier.size(24.dp), tint = TraceColors.Coral)
            Column(Modifier.weight(1f)) {
                Text(when { state.pending.isNotEmpty() -> "확인이 필요한 송금 ${state.pending.size}건"; active -> "확인할 위험 신호가 있어요"; else -> "TRACE 안전 확인" }, style = MaterialTheme.typography.bodyMedium)
                Space(3); Caption(if (state.pending.isNotEmpty()) "아직 돈은 나가지 않았습니다." else if (active) "안전 센터에서 흐름을 확인하세요." else "최근 연결된 위험 정황 없음")
            }
            AppIcon(BankIcons.Chevron, size = 16, tint = TraceColors.Muted)
        }
    }
}

@Composable fun QuietAssets(state: BankState, open: (String) -> Unit) {
    Page(title = "내 자산", tag = "assets") {
        Space(14); Caption("총 보유 자산"); Space(10); Money(state.balance + state.savings)
        Space(28); Rule(); Space(14)
        SectionTitle("계좌")
        MenuRow("새온 생활통장", "입출금", tag = "asset_primary_account", trailing = "${won(state.balance)}원") { open("account") }
        MenuRow("새온 모아적금", "적금", trailing = "${won(state.savings)}원") { open("savings") }
        Space(22); Rule(); Space(14); SectionTitle("카드·대출")
        MenuRow("새온 체크카드", "이번 달 이용 금액", trailing = "382,400원") { open("card") }
        MenuRow(Fixtures.LOAN_NAME, "남은 대출 원금", trailing = "${won(state.loanBalance)}원") { open("loan") }
        Space(18); Caption("대출은 보유 자산에 포함하지 않습니다.")
    }
}

@Composable fun QuietRecipients(state: BankState, preferences: BankPreferences, model: BankViewModel, open: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val featured = Fixtures.recipient(state.scenario)
    val recent = (listOf(featured) + state.receipts.mapNotNull { r -> state.recipients.find { it.id == r.recipient.id } })
        .distinctBy { it.id }.take(3)
    val favorites = state.recipients.filter { it.id in preferences.favoriteIds && it !in recent }
    fun select(recipient: Recipient) { model.startRecipient(recipient) { open("amount") } }
    Page(title = "송금", tag = "transfer_recipient") {
        Space(10); Headline(if (preferences.childMode) "누구에게 보낼까요?" else "받는 분을 선택해 주세요.")
        Space(24)
        Field(query, "이름·은행·계좌 검색", { query = it }, Modifier.testTag("recipient_search"))
        Space(8); MenuRow("계좌번호로 보내기", tag = "recipient_account_entry") { open("recipient_entry") }
        Space(12); Rule(); Space(16)
        if (query.isNotBlank()) {
            val found = state.recipients.filter { it.name.contains(query) || it.bank.contains(query) || it.account.contains(query) }
            if (found.isEmpty()) EmptyState("찾는 계좌가 없어요.", "이름이나 계좌 끝자리를 다시 확인해 주세요.")
            found.forEach { r -> PayeeRow(r) { select(r) } }
        } else {
            Caption("최근 보낸 사람"); Space(6)
            recent.forEach { r -> PayeeRow(r) { select(r) } }
            if (favorites.isNotEmpty()) {
                Space(20); Caption("자주 보내는 사람"); Space(6)
                Column(Modifier.testTag("favorite_recipient_list")) { favorites.forEach { r -> PayeeRow(r) { select(r) } } }
            }
        }
    }
}

@Composable private fun PayeeRow(recipient: Recipient, onClick: () -> Unit) {
    MenuRow(recipient.name, "${recipient.bank} · ${recipient.account}", tag = "recipient_${recipient.id}", onClick = onClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun QuietAmount(state: BankState, preferences: BankPreferences, model: BankViewModel, open: (String) -> Unit, back: () -> Unit) {
    val draft = state.draft
    if (draft == null) { AmountScreen(state, model, open, back); return }
    var digits by rememberSaveable(draft.recipient.id) { mutableStateOf(draft.amount.toString()) }
    var purposeName by rememberSaveable(draft.recipient.id) { mutableStateOf(draft.purpose.name) }
    var purposeSheet by rememberSaveable { mutableStateOf(false) }
    var direct by rememberSaveable { mutableStateOf(false) }
    var directDigits by rememberSaveable { mutableStateOf("") }
    val interaction by model.interaction.collectAsStateWithLifecycle()
    val amount = digits.toLongOrNull() ?: 0L
    val purpose = Purpose.valueOf(purposeName)
    val error = runCatching { BankEngine.validateAmount(state, amount, purpose, model.repository.clock.now()) }.exceptionOrNull()?.message
    fun update(value: String) {
        val clean = value.filter { it in '0'..'9' }.trimStart('0').ifEmpty { "0" }
        if (clean.length > 9) return
        digits = clean
        model.storeDraft(TransferDraft(draft.recipient, clean.toLongOrNull() ?: 0, purpose))
    }
    Page(title = "송금 금액", tag = "transfer_amount", back = back, footer = {
        PrimaryButton("다음", Modifier.testTag("amount_next"), enabled = amount > 0 && error == null && !interaction.busy) {
            model.review(TransferDraft(draft.recipient, amount, purpose)) { open("transfer_state") }
        }
    }) {
        Text("${draft.recipient.name}님에게", style = MaterialTheme.typography.titleSmall)
        Space(6); Headline("얼마를 보낼까요?"); Space(22)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Money(amount, Modifier.weight(1f).testTag("amount_value"))
            IconAction(BankIcons.Edit, "금액 직접 입력", Modifier.testTag("amount_edit")) { directDigits = digits; direct = true }
        }
        Space(6); Caption("잔액 ${won(state.balance)}원")
        if (amount > 0 && error != null) ErrorNote(error)
        Space(12)
        Row(Modifier.fillMaxWidth()) {
            listOf(10_000L to "+1만", 100_000L to "+10만", 1_000_000L to "+100만").forEach { (value, label) ->
                QuietButton(label, Modifier.weight(1f)) { update((amount + value).toString()) }
            }
        }
        Rule(); Space(6)
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("clear", "0", "backspace")).forEach { row ->
            Row(Modifier.fillMaxWidth()) { row.forEach { key ->
                TextButton(onClick = { when (key) { "clear" -> update("0"); "backspace" -> update(digits.dropLast(1)); else -> update(if (digits == "0") key else digits + key) } },
                    modifier = Modifier.weight(1f).heightIn(min = 58.dp).testTag("key_$key")
                        .semantics { contentDescription = when (key) { "clear" -> "전체 지우기"; "backspace" -> "한 자리 지우기"; else -> key } },
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 2.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = TraceColors.Ink)) {
                    when (key) { "clear" -> Text("지우기", style = MaterialTheme.typography.labelMedium)
                        "backspace" -> AppIcon(BankIcons.Delete, size = 22)
                        else -> Text(key, fontSize = 27.sp, fontWeight = FontWeight.Medium) }
                }
            } }
        }
        Space(10); Rule()
        MenuRow("송금 목적", trailing = purpose.label, tag = "transfer_purpose") { purposeSheet = true }
        if (preferences.childMode) Caption("누군가 대신 보내 달라고 했다면, 먼저 보호자와 확인해요.")
    }
    if (purposeSheet) ModalBottomSheet(onDismissRequest = { purposeSheet = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = TraceColors.Surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("어떤 돈인가요?", style = MaterialTheme.typography.headlineSmall); Space(12)
            Purpose.entries.forEach { p ->
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.RadioButton) {
                    purposeName = p.name; model.storeDraft(TransferDraft(draft.recipient, amount, p)); purposeSheet = false
                }.semantics { selected = p == purpose }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    if (p == purpose) AppIcon(BankIcons.Check, size = 20)
                }
            }
        }
    }
    if (direct) AlertDialog(onDismissRequest = { direct = false }, title = { Text("송금 금액") },
        text = { Field(directDigits, "원", { directDigits = it.filter { c -> c in '0'..'9' }.take(9) }, Modifier.testTag("amount_direct_input"), keyboard = KeyboardType.Number) },
        confirmButton = { QuietButton("입력 완료") { update(directDigits); direct = false } },
        dismissButton = { QuietButton("취소") { direct = false } }, containerColor = TraceColors.Surface)
}

@Composable fun QuietReview(state: BankState, record: TransferRecord, preferences: BankPreferences, interaction: InteractionState,
    model: BankViewModel, open: (String) -> Unit, back: () -> Unit) {
    val intent = record.intent
    val error = runCatching { BankEngine.validateAmount(state, intent.amount, intent.purpose, model.repository.clock.now()) }.exceptionOrNull()?.message
    Page(title = "보내기 전 확인", tag = "transfer_review", back = back, footer = {
        PrimaryButton("${won(intent.amount)}원 보내기", Modifier.testTag("transfer_confirm"), enabled = error == null && !interaction.busy && record.stage == TransferStage.REVIEW) {
            model.requestAuthorization(intent.id)
        }
    }) {
        Space(24)
        Text(if (intent.recipient.kind == RecipientKind.INSTITUTION) intent.recipient.name else "${intent.recipient.name}님에게", style = MaterialTheme.typography.headlineSmall)
        Space(14); Money(intent.amount); Space(8)
        Body(if (preferences.childMode) "이 금액을 보낼까요?" else "보낼까요?")
        Space(28); Rule(); Space(14)
        DetailRow("받는 계좌", "${intent.recipient.bank}\n${intent.recipient.account}")
        DetailRow("출금 계좌", "새온 생활통장")
        DetailRow("수수료", "0원")
        if (intent.purpose != Purpose.GENERAL) DetailRow("송금 목적", intent.purpose.label)
        Space(16); Rule(); Space(18)
        if (intent.officialRouteId != null) Caption("받는 계좌가 바뀌었습니다. 이전 인증은 사용하지 않고 새로 확인합니다.")
        else QuietButton("금액·목적 수정", Modifier.testTag("review_edit"), enabled = record.stage == TransferStage.REVIEW && !interaction.busy) {
            model.editReview(intent.id) { open("amount") }
        }
        error?.let { ErrorNote(it) }
        record.error?.takeIf { it != error }?.let { ErrorNote(it) }
        Space(14); SimulationNote()
    }
}
