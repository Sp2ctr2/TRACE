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

@Composable fun QuietHome(state: BankState, preferences: BankPreferences, model: BankViewModel, open: (String) -> Unit) {
    Page(title = "새온은행", tag = "home", actions = {
        QuietButton("시연", Modifier.testTag("home_demo_center")) { open("demo_center") }
        IconAction(BankIcons.Bell, "알림") { open("notifications") }
    }) {
        Space(12)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).heightIn(min = 48.dp).clickable(role = Role.Button) { open("account") },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("새온 생활통장", style = MaterialTheme.typography.titleSmall)
                AppIcon(BankIcons.Chevron, size = 14, tint = TraceColors.Muted)
            }
            IconAction(BankIcons.Eye, if (preferences.hideBalance) "잔액 보이기" else "잔액 숨기기") {
                model.preference { hideBalance(!preferences.hideBalance) }
            }
        }
        if (preferences.hideBalance) Text("잔액 숨김", Modifier.testTag("home_balance"), style = MaterialTheme.typography.displaySmall)
        else Money(state.balance, Modifier.testTag("home_balance"))
        Space(3); Caption("새온은행 110-***-0001")
        Space(22)
        if (preferences.easyMode) {
            PrimaryButton(if (preferences.childMode) "돈 보내기" else "송금하기", Modifier.testTag("home_transfer")) { open("transfer") }
            SecondaryButton("거래 내역 보기") { open("history") }
            Space(18); Rule(); Space(20)
            Body(if (preferences.childMode) "모르는 사람이 돈을 보내 달라고 했나요?\n먼저 보호자와 확인해요." else "서둘러 보내라는 요청을 받았다면,\n먼저 상대와 보내는 이유를 확인하세요.")
        } else {
            if (LocalDensity.current.fontScale >= 1.5f) {
                listOf("transfer" to "송금", "bring" to "가져오기", "history" to "내역").forEachIndexed { index, (route, label) ->
                    MenuRow(label, tag = if (index == 0) "home_transfer" else "home_$route") { open(route) }
                }
            } else {
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { open("transfer") }, modifier = Modifier.weight(1.25f).heightIn(min = 50.dp).testTag("home_transfer"),
                        shape = RoundedCornerShape(11.dp), elevation = ButtonDefaults.buttonElevation(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TraceColors.Deep, contentColor = TraceColors.White)) {
                        Text("송금", Modifier.testTag("home_action_label_transfer"), style = MaterialTheme.typography.labelLarge)
                    }
                    QuietButton("가져오기", Modifier.weight(1f).testTag("home_bring")) { open("bring") }
                    QuietButton("내역", Modifier.weight(0.8f).testTag("home_history")) { open("history") }
                }
                // Retain separate text nodes for financial-label wrapping assertions.
            }
            Space(28); Rule(); Space(14)
            SectionTitle("최근 거래", "전체 보기") { open("history") }
            state.receipts.take(3).forEach { receipt ->
                if (preferences.hideBalance) MenuRow(receipt.recipient.name, "금액 숨김") { open("receipt/${receipt.id}") }
                else ReceiptRow(receipt) { open("receipt/${receipt.id}") }
            }
        }
        Space(20); Rule(); Space(8)
        val active = state.events.any { it.active(model.repository.clock.now()) }
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button) { open("safety") }
            .padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(BankIcons.Trace, null, Modifier.size(22.dp), tint = TraceColors.Coral)
            Column(Modifier.weight(1f)) {
                Text(when { state.pending.isNotEmpty() -> "확인이 필요한 송금 ${state.pending.size}건"; active -> "확인할 위험 신호가 있어요"; else -> "TRACE 안전 확인" },
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Space(3); Caption(when { state.pending.isNotEmpty() -> "아직 돈은 나가지 않았습니다."; active -> "송금 앞의 흐름을 확인하세요."; else -> "최근 연결된 위험 정황 없음" })
            }
            AppIcon(BankIcons.Chevron, size = 16, tint = TraceColors.Muted)
        }
    }
}

@Composable fun QuietAssets(state: BankState, open: (String) -> Unit) {
    Page(title = "내 자산", tag = "assets") {
        Space(14); Caption("총 보유 자산"); Space(8); Money(state.balance + state.savings)
        Space(6); Caption("입출금과 적금의 합계예요.")
        Space(28); Rule(); Space(14); SectionTitle("내 계좌")
        MenuRow("새온 생활통장", "입출금 · 110-***-0001", tag = "asset_primary_account", trailing = "${won(state.balance)}원") { open("account") }
        MenuRow("새온 모아적금", "적금 · 220-***-0102", trailing = "${won(state.savings)}원") { open("savings") }
        Space(22); Rule(); Space(14); SectionTitle("카드와 대출")
        MenuRow("새온 체크카드", "이번 달 이용", trailing = "382,400원") { open("card") }
        MenuRow(Fixtures.LOAN_NAME, "남은 원금", trailing = "${won(state.loanBalance)}원") { open("loan") }
        Space(18); Caption("대출 원금은 보유 자산에 포함하지 않습니다.")
    }
}

@Composable fun QuietRecipients(state: BankState, preferences: BankPreferences, model: BankViewModel, open: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val featured = Fixtures.recipient(state.scenario)
    val recent = state.receipts.mapNotNull { receipt -> state.recipients.find { it.id == receipt.recipient.id } }.distinctBy { it.id }.take(3)
    val favorites = state.recipients.filter { it.id in preferences.favoriteIds && it !in recent && it != featured }
    fun select(recipient: Recipient) { model.startRecipient(recipient) { open("amount") } }
    Page(title = "송금", tag = "transfer_recipient") {
        Space(10); Headline("누구에게 보낼까요?"); Space(20)
        Field(query, "이름·은행·계좌 검색", { query = it }, Modifier.testTag("recipient_search"))
        Space(4); MenuRow("계좌번호로 보내기", tag = "recipient_account_entry") { open("recipient_entry") }
        Space(8); Rule(); Space(18)
        if (query.isNotBlank()) {
            val found = state.recipients.filter { it.name.contains(query) || it.bank.contains(query) || it.account.contains(query) }
            if (found.isEmpty()) EmptyState("찾는 계좌가 없어요.", "이름이나 계좌 끝자리를 다시 확인해 주세요.")
            found.forEach { recipient -> PayeeRow(recipient) { select(recipient) } }
        } else {
            // An unsafe fixture was never a completed payment. Do not label it as recent history.
            if (featured !in recent) {
                Caption("입력한 계좌"); Space(4); PayeeRow(featured) { select(featured) }
                Space(20); Rule(); Space(18)
            }
            Caption("최근 보낸 분"); Space(4)
            recent.forEach { recipient -> PayeeRow(recipient) { select(recipient) } }
            if (favorites.isNotEmpty()) {
                Space(22); Caption("자주 보내는 분"); Space(4)
                Column(Modifier.testTag("favorite_recipient_list")) { favorites.forEach { recipient -> PayeeRow(recipient) { select(recipient) } } }
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
        if (clean.length > 9) {
            model.showError("입력할 수 있는 금액을 넘었어요. 금액을 줄여 주세요. 아직 돈은 나가지 않았습니다.")
            return
        }
        digits = clean
        model.storeDraft(TransferDraft(draft.recipient, clean.toLongOrNull() ?: 0, purpose))
    }
    Page(title = "송금 금액", tag = "transfer_amount", back = back, footer = {
        PrimaryButton("다음", Modifier.testTag("amount_next"), enabled = amount > 0 && error == null && !interaction.busy) {
            model.review(TransferDraft(draft.recipient, amount, purpose)) { open("transfer_state") }
        }
    }) {
        Space(10); Text("${draft.recipient.name}님에게", style = MaterialTheme.typography.titleSmall)
        Space(5); Headline("얼마를 보낼까요?"); Space(8)
        Caption("${draft.recipient.bank} · ${draft.recipient.account}")
        Space(24)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Money(amount, Modifier.weight(1f).testTag("amount_value"))
            IconAction(BankIcons.Edit, "금액 직접 입력", Modifier.testTag("amount_edit")) { directDigits = digits; direct = true }
        }
        Space(5); Caption("출금 가능 ${won(state.balance)}원")
        if (amount > 0 && error != null) ErrorNote(error)
        Space(16)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(10_000L to "+1만", 100_000L to "+10만", 1_000_000L to "+100만").forEach { (value, label) ->
                QuietButton(label, Modifier.weight(1f), enabled = !interaction.busy) { update((amount + value).toString()) }
            }
        }
        Rule(); Space(8)
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("clear", "0", "backspace")).forEach { row ->
            Row(Modifier.fillMaxWidth()) { row.forEach { key ->
                TextButton(onClick = { when (key) { "clear" -> update("0"); "backspace" -> update(digits.dropLast(1)); else -> update(if (digits == "0") key else digits + key) } },
                    enabled = !interaction.busy, modifier = Modifier.weight(1f).heightIn(min = if (preferences.easyMode) 64.dp else 56.dp).testTag("key_$key")
                        .semantics { contentDescription = when (key) { "clear" -> "전체 지우기"; "backspace" -> "한 자리 지우기"; else -> key } },
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 2.dp), shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = TraceColors.Ink)) {
                    when (key) { "clear" -> Text("지우기", style = MaterialTheme.typography.labelMedium)
                        "backspace" -> AppIcon(BankIcons.Delete, size = 22)
                        else -> Text(key, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 27.sp, fontWeight = FontWeight.Medium)) }
                }
            } }
        }
        Space(8); Rule()
        MenuRow("송금 목적", trailing = purpose.label, tag = "transfer_purpose") { purposeSheet = true }
        if (preferences.childMode) Caption("대신 보내 달라는 부탁은 보호자와 먼저 확인해요.")
    }
    if (purposeSheet) ModalBottomSheet(onDismissRequest = { purposeSheet = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = TraceColors.Surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("어떤 돈인가요?", style = MaterialTheme.typography.headlineSmall); Space(10); Caption("선택하지 않아도 괜찮아요."); Space(12)
            Purpose.entries.forEach { option ->
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.RadioButton) {
                    purposeName = option.name; model.storeDraft(TransferDraft(draft.recipient, amount, option)); purposeSheet = false
                }.semantics { selected = option == purpose }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(option.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    if (option == purpose) AppIcon(BankIcons.Check, size = 18)
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
        Caption("시연용 가상 거래 · 실제 자금 이동 없음"); Space(8)
        PrimaryButton("${won(intent.amount)}원 보내기", Modifier.testTag("transfer_confirm"), enabled = error == null && !interaction.busy && record.stage == TransferStage.REVIEW) {
            model.requestAuthorization(intent.id)
        }
    }) {
        Space(26); Money(intent.amount); Space(10)
        Text(if (intent.recipient.kind == RecipientKind.INSTITUTION) "${intent.recipient.name}로" else "${intent.recipient.name}님에게",
            style = MaterialTheme.typography.headlineSmall)
        Space(4); Body(if (preferences.childMode) "이 금액을 보낼까요?" else "보낼까요?", subdued = true)
        Space(30); Rule(); Space(12)
        DetailRow("받는 계좌", "${intent.recipient.bank}\n${intent.recipient.account}")
        DetailRow("출금 계좌", "새온 생활통장")
        DetailRow("수수료", "0원")
        if (intent.purpose != Purpose.GENERAL) DetailRow("송금 목적", intent.purpose.label)
        Space(12); Rule(); Space(14)
        if (intent.officialRouteId != null) {
            Text("받는 계좌가 바뀌었습니다.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Space(5); Caption("이전 인증은 쓰지 않고, 새 거래로 확인해요.")
        } else QuietButton("금액·목적 수정", Modifier.testTag("review_edit"), enabled = record.stage == TransferStage.REVIEW && !interaction.busy) {
            model.editReview(intent.id) { open("amount") }
        }
        error?.let { ErrorNote(it) }
        record.error?.takeIf { it != error }?.let { ErrorNote(it) }
    }
}
