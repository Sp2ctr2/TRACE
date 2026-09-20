package app.saeon.trace.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saeon.trace.core.*
import app.saeon.trace.data.BankPreferences
import app.saeon.trace.ui.*
import app.saeon.trace.ui.design.*

@Composable fun RecipientScreen(state: BankState, model: BankViewModel, open: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val preferences by model.preferences.collectAsStateWithLifecycle()
    val scenarioRecipient = Fixtures.recipient(state.scenario)
    val recent = (listOf(scenarioRecipient) + state.receipts.filter { it.recipient.kind == RecipientKind.PERSON }
        .mapNotNull { receipt -> state.recipients.find { it.id == receipt.recipient.id } }).distinctBy { it.id }.take(3)
    val favorites = state.recipients.filter { it.id in preferences.favoriteIds && recent.none { r -> r.id == it.id } }
    val matches = state.recipients.filter { query.isBlank() || it.name.contains(query) || it.account.contains(query) || it.bank.contains(query) }
    Page(title = "송금", tag = "transfer_recipient") {
        Space(8); MicroLabel("받는 분"); Space(10)
        Headline("누구에게 보낼까요?")
        Space(20)
        Field(query, "이름·은행·계좌 검색", { query = it }, Modifier.testTag("recipient_search"))
        Space(8)
        MenuRow("계좌번호로 보내기", "새로운 가상 계좌를 직접 입력해요.", BankIcons.Bank, tag = "recipient_account_entry") { open("recipient_entry") }
        Space(10); Rule(); Space(16)
        if (query.isNotBlank()) {
            SectionTitle("검색 결과")
            if (matches.isEmpty()) EmptyState("찾는 계좌가 없어요.", "이름이나 계좌 끝자리를 다시 입력해 주세요.")
            matches.forEach { recipient -> RecipientRow(recipient) { model.startRecipient(recipient) { open("amount") } } }
        } else {
            SectionTitle("최근 보낸 사람")
            recent.forEach { recipient -> RecipientRow(recipient) { model.startRecipient(recipient) { open("amount") } } }
            if (favorites.isNotEmpty()) {
                Space(20); Rule(); Space(14); SectionTitle("자주 보내는 사람")
                Column(Modifier.testTag("favorite_recipient_list")) {
                    favorites.forEach { recipient -> RecipientRow(recipient) { model.startRecipient(recipient) { open("amount") } } }
                }
            }
            val other = state.recipients.filter { candidate -> recent.none { it.id == candidate.id } && favorites.none { it.id == candidate.id } }
            if (other.isNotEmpty()) {
                Space(20); Rule(); Space(14); SectionTitle("확인한 계좌")
                other.forEach { recipient -> RecipientRow(recipient) { model.startRecipient(recipient) { open("amount") } } }
            }
        }
        Space(18); SimulationNote()
    }
}

@Composable private fun RecipientRow(recipient: Recipient, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 74.dp).clickable(role = Role.Button, onClick = onClick)
            .testTag("recipient_${recipient.id}").padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        RecipientGlyph(recipient.name)
        Column(Modifier.weight(1f)) {
            Text(recipient.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Space(3); Caption("${recipient.bank} · ${recipient.account}")
        }
        if (!recipient.known) MicroLabel("처음")
        AppIcon(BankIcons.Chevron, size = 17, tint = TraceColors.Muted)
    }
}

@Composable fun RecipientEntryScreen(state: BankState, model: BankViewModel, open: (String) -> Unit, back: () -> Unit) {
    var bankName by rememberSaveable { mutableStateOf("노을은행") }
    var lastFour by rememberSaveable { mutableStateOf("") }
    var checked by rememberSaveable { mutableStateOf(false) }
    val known = state.recipients.find { it.bank == bankName && it.account.endsWith(lastFour) && lastFour.length == 4 }
    val candidate = known ?: Recipient("virtual-${if (bankName == "노을은행") "noeul" else "saeon"}-$lastFour", "정○○", bankName, "110-***-$lastFour")
    Page(title = "계좌로 보내기", back = back, tag = "recipient_entry", footer = {
        PrimaryButton(if (checked) "이 계좌로 보내기" else "받는 분 확인", enabled = lastFour.length == 4) {
            if (!checked) checked = true else model.act {
                model.repository.addRecipient(candidate)
                model.repository.setDraft(TransferDraft(candidate))
                open("amount")
            }
        }
    }) {
        Headline("받는 계좌를\n알려주세요."); Space(24)
        Caption("시연에서는 가상 계좌의 끝 네 자리만 입력합니다.")
        Space(14)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("새온은행", "노을은행").forEach { name ->
                FilterChip(selected = bankName == name, onClick = { bankName = name; checked = false },
                    label = { Text(name) }, modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TraceColors.Ink, selectedLabelColor = TraceColors.White))
            }
        }
        Space(12)
        Field(lastFour, "가상 계좌 끝 4자리", { lastFour = it.filter(Char::isDigit).take(4); checked = false },
            Modifier.testTag("recipient_account_number"), keyboard = KeyboardType.Number)
        Space(26)
        if (checked) {
            Rule(); Space(20); Text("${candidate.name}님의 계좌예요.", style = MaterialTheme.typography.titleMedium)
            Space(12); DetailRow("은행", candidate.bank); DetailRow("계좌", candidate.account)
            Space(16); Body("예금주 이름이 확인되었다고 해서 송금 목적이나 상대의 요청까지 안전한 것은 아니에요.", subdued = true)
        }
        Space(24); SimulationNote()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AmountScreen(state: BankState, model: BankViewModel, open: (String) -> Unit, back: () -> Unit) {
    val draft = state.draft
    if (draft == null) {
        Page(title = "송금", back = back, footer = { PrimaryButton("받는 분 선택") { open("transfer") } }) {
            EmptyState("받는 분을 먼저 선택해 주세요.", "송금을 새로 실행하지 않았습니다. 계좌를 선택한 뒤 금액을 입력해 주세요.")
        }
        return
    }
    var digits by rememberSaveable(draft.recipient.id) { mutableStateOf(draft.amount.toString()) }
    var purposeName by rememberSaveable(draft.recipient.id) { mutableStateOf(draft.purpose.name) }
    var purposeSheet by rememberSaveable { mutableStateOf(false) }
    var directInput by rememberSaveable { mutableStateOf(false) }
    var directDigits by rememberSaveable { mutableStateOf("") }
    val interaction by model.interaction.collectAsStateWithLifecycle()
    val amount = digits.toLongOrNull() ?: 0L
    val purpose = Purpose.valueOf(purposeName)
    val localDraft = TransferDraft(draft.recipient, amount, purpose)
    val error = runCatching { BankEngine.validateAmount(state, amount, purpose, model.repository.clock.now()) }.exceptionOrNull()?.message
    val valid = error == null && amount > 0
    fun update(value: String) {
        val normalized = value.filter(Char::isDigit).trimStart('0').ifEmpty { "0" }
        if (normalized.length > 9) {
            model.showError("입력할 수 있는 금액을 넘었어요. 1억 원 이하로 입력해 주세요. 돈은 나가지 않았습니다.")
            return
        }
        digits = normalized
        model.storeDraft(TransferDraft(draft.recipient, normalized.toLongOrNull() ?: 0, purpose))
    }
    Page(title = "송금", tag = "transfer_amount", back = back, footer = {
        PrimaryButton("다음", Modifier.testTag("amount_next"), enabled = valid && !interaction.busy) {
            model.review(localDraft) { open("transfer_state") }
        }
    }) {
        MicroLabel("금액"); Space(12)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RecipientGlyph(draft.recipient.name)
            Column(Modifier.weight(1f)) {
                Text("${draft.recipient.name}님에게", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Space(2); Caption("${draft.recipient.bank} · ${draft.recipient.account}")
            }
        }
        Space(28)
        Headline("얼마를 보낼까요?")
        Space(16)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Money(amount, Modifier.weight(1f).testTag("amount_value"))
            IconAction(BankIcons.Edit, "금액 직접 입력", Modifier.testTag("amount_edit")) { directDigits = digits; directInput = true }
        }
        Space(6); Caption("보낼 수 있는 금액 ${won(state.balance)}원")
        if (amount > 0 && error != null) ErrorNote(error)
        Space(14)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(10_000L to "+1만", 100_000L to "+10만", 1_000_000L to "+100만").forEach { (addition, label) ->
                OutlinedButton(
                    onClick = { update((amount + addition).toString()) },
                    enabled = !interaction.busy,
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, TraceColors.Divider),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TraceColors.Ink)
                ) { Text(label, style = MaterialTheme.typography.labelMedium) }
            }
        }
        Space(14); Rule(); Space(8)
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("clear", "0", "backspace"))
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    val label = when (key) { "clear" -> "전체 지우기"; "backspace" -> "한 자리 지우기"; else -> key }
                    TextButton(
                        onClick = {
                            when (key) {
                                "clear" -> update("0")
                                "backspace" -> update(digits.dropLast(1))
                                else -> update(if (digits == "0") key else digits + key)
                            }
                        },
                        enabled = !interaction.busy,
                        modifier = Modifier.weight(1f).heightIn(min = 58.dp).testTag("key_$key").semantics { contentDescription = label },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = TraceColors.Ink)
                    ) {
                        when (key) {
                            "clear" -> Text("지우기", style = MaterialTheme.typography.labelMedium, color = TraceColors.Muted)
                            "backspace" -> AppIcon(BankIcons.Delete, size = 20)
                            else -> Text(key, fontSize = 26.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
        Space(10); Rule(); Space(4)
        MenuRow("송금 목적", "선택 사항", trailing = purpose.label, tag = "transfer_purpose") { purposeSheet = true }
        if (!draft.recipient.known) {
            Space(4); EditorialPanel { Caption("처음 보내는 계좌예요. 받는 분과 금액을 한 번 더 확인해 주세요.") }
        }
    }
    if (purposeSheet) ModalBottomSheet(
        onDismissRequest = { purposeSheet = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = TraceColors.Surface
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("어떤 돈을 보내시나요?", style = MaterialTheme.typography.headlineSmall); Space(10)
            Caption("선택하지 않아도 괜찮아요. 대출 상환은 받는 경로를 함께 확인합니다."); Space(14)
            Purpose.entries.forEach { option ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.RadioButton) {
                        purposeName = option.name
                        model.storeDraft(localDraft.copy(purpose = option))
                        purposeSheet = false
                    }.semantics { selected = purpose == option }.padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(option.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    if (purpose == option) AppIcon(BankIcons.Check, tint = TraceColors.Ink, size = 19)
                }
                if (option != Purpose.entries.last()) Rule()
            }
        }
    }
    if (directInput) AlertDialog(
        onDismissRequest = { directInput = false },
        title = { Text("송금 금액") },
        text = { Column {
            Field(directDigits, "원", { directDigits = it.filter(Char::isDigit).take(9) }, Modifier.testTag("amount_direct_input"), keyboard = KeyboardType.Number)
            Space(10); Caption("${won(directDigits.toLongOrNull() ?: 0)}원")
        } },
        confirmButton = { QuietButton("입력 완료") { update(directDigits); directInput = false } },
        dismissButton = { QuietButton("취소") { directInput = false } },
        containerColor = TraceColors.Surface
    )
}

@Composable fun TransferStateScreen(state: BankState, preferences: BankPreferences, interaction: InteractionState,
                                    model: BankViewModel, open: (String) -> Unit, back: () -> Unit, home: () -> Unit) {
    val record = state.current
    if (record == null) {
        Page(title = "송금", back = back, footer = { PrimaryButton("홈으로", onClick = home) }) {
            EmptyState("진행 중인 송금이 없어요.", "송금 완료 여부는 거래 내역에서 확인해 주세요. 이 화면에서 새로 송금하지 않았습니다.")
        }
        return
    }
    BackHandler(enabled = record.stage == TransferStage.EVALUATING) { model.cancelAuthorization(); back() }
    when (record.stage) {
        TransferStage.REVIEW, TransferStage.AUTHORIZING -> ReviewScreen(state, record, interaction, model, open, back)
        TransferStage.EVALUATING -> EvaluatingScreen { model.cancelAuthorization(); back() }
        TransferStage.HOLD -> HoldScreen(record, preferences.easyMode, model, open, back, home)
        TransferStage.WARN -> WarnScreen(record, interaction, model, back, home)
        TransferStage.VERIFY -> VerifyScreen(record, interaction, model, back, home)
        TransferStage.UNKNOWN -> UnknownScreen(record, interaction, model, back, home)
        TransferStage.ROUTE -> OfficialRouteScreen(record, interaction, model, back)
        TransferStage.COMPLETE -> CompleteScreen(state, record, open, home)
        TransferStage.CANCELLED, TransferStage.SUPERSEDED -> Page(title = "송금", back = back, footer = { PrimaryButton("홈으로", onClick = home) }) {
            EmptyState("종료된 송금입니다.", "이 거래를 다시 실행하지 않습니다. 현재 잔액과 거래 내역은 홈에서 확인해 주세요.")
        }
    }
}

@Composable private fun ReviewScreen(
    state: BankState,
    record: TransferRecord,
    interaction: InteractionState,
    model: BankViewModel,
    open: (String) -> Unit,
    back: () -> Unit
) {
    val intent = record.intent
    val amountError = runCatching {
        BankEngine.validateAmount(state, intent.amount, intent.purpose, model.repository.clock.now())
    }.exceptionOrNull()?.message
    Page(title = "송금", tag = "transfer_review", back = back, footer = {
        PrimaryButton(
            "${won(intent.amount)}원 보내기",
            Modifier.testTag("transfer_confirm"),
            enabled = amountError == null && !interaction.busy && record.stage == TransferStage.REVIEW
        ) { model.requestAuthorization(intent.id) }
    }) {
        MicroLabel("최종 확인"); Space(12)
        Money(intent.amount)
        Space(8)
        Text(
            if (intent.recipient.kind == RecipientKind.INSTITUTION) "${intent.recipient.name}로" else "${intent.recipient.name}님에게",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium
        )
        Space(24)
        SurfaceBox(tint = TraceColors.SurfaceRaised) {
            DetailRow("받는 계좌", "${intent.recipient.bank}\n${intent.recipient.account}")
            Rule()
            DetailRow("출금 계좌", "새온 생활통장\n110-***-0001")
            Rule()
            DetailRow("수수료", "0원")
            if (intent.purpose != Purpose.GENERAL) {
                Rule(); DetailRow("송금 목적", intent.purpose.label)
            }
        }
        Space(18)
        if (intent.officialRouteId != null) {
            EditorialPanel(accent = true) {
                TraceSignature(); Space(8)
                Body("받는 계좌가 바뀌어 새 송금으로 확인합니다.")
                Space(4); Caption("이전 거래의 인증은 사용하지 않아요.")
            }
        } else {
            Caption("예금주 이름이 맞아도 상대의 요청까지 안전하다는 뜻은 아니에요.")
            Space(5)
            QuietButton("금액·목적 수정", Modifier.testTag("review_edit"), enabled = record.stage == TransferStage.REVIEW && !interaction.busy) {
                model.editReview(intent.id) { open("amount") }
            }
        }
        amountError?.let { ErrorNote(it) }
        record.error?.takeIf { it != amountError }?.let { ErrorNote(it) }
        Space(18); SimulationNote()
    }
}

@Composable private fun EvaluatingScreen(cancel: () -> Unit) {
    Page(title = "송금 확인", tag = "trace_evaluating", footer = { SecondaryButton("취소하고 돌아가기", onClick = cancel) }) {
        Space(22); TraceSignature(); Space(24)
        MicroLabel("거래 맥락 확인", coral = true); Space(8)
        Headline("송금 앞의 맥락을\n확인하고 있어요.")
        Space(16)
        Caption("인증한 거래와 지금의 위험 정황이 같은지 짧게 확인합니다.")
        Space(28)
        EditorialPanel {
            NumberedReason(1, "요청의 목적")
            Rule(); NumberedReason(2, "최근 위험 신호")
            Rule(); NumberedReason(3, "수취인과 거래")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun HoldScreen(
    record: TransferRecord,
    easy: Boolean,
    model: BankViewModel,
    open: (String) -> Unit,
    back: () -> Unit,
    home: () -> Unit
) {
    var reasonsOpen by rememberSaveable { mutableStateOf(false) }
    val reasons = listOf(
        RiskType.IMPERSONATION,
        RiskType.URGENCY,
        RiskType.NEW_RECIPIENT,
        RiskType.SUSPICIOUS_LINK,
        RiskType.FINANCIAL_INSTRUCTION
    ).filter { it in record.reasons }
    Page(title = "송금 보류", tag = "trace_hold", back = back, footer = {
        PrimaryButton(
            if (easy) "공식 경로로 확인" else "안전하게 확인하기",
            Modifier.testTag("hold_safe_action"),
            accent = true
        ) { open("safety_guide") }
        SecondaryButton("송금 취소", Modifier.testTag("hold_cancel")) { model.cancelTransfer(record.intent.id, home) }
    }) {
        Space(6); TraceSignature(); Space(18); AccentRule(34); Space(16)
        if (easy) {
            MicroLabel("송금 보류", coral = true); Space(7)
            Headline("송금을 잠시\n멈췄습니다.", Modifier.testTag("easy_mode"))
            Space(18)
            Text("아직 돈은 나가지 않았습니다.", style = MaterialTheme.typography.titleMedium)
            Space(18)
            SurfaceBox(tint = TraceColors.CoralWash) {
                Money(record.intent.amount, hero = false)
                Space(7); Body("${record.intent.recipient.name}님에게 보내려던 돈")
            }
            Space(20)
            Body("상대가 기관을 사칭했을 가능성이 있어요. 상대가 준 번호나 링크가 아닌, 은행 앱의 공식 경로로 확인하세요.", subdued = true)
        } else {
            MicroLabel("송금 보류", coral = true); Space(7)
            Headline("잠깐,\n확인하고 보내볼까요?")
            Space(14)
            Text("아직 돈은 나가지 않았습니다.", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Space(7)
            Body("방금 전의 요청이 이 송금과 이어져 있어 잠시 멈췄어요.", subdued = true)
            Space(22)
            SurfaceBox(tint = TraceColors.CoralWash) {
                MicroLabel(if (record.intent.recipient.known) "저장된 수취인" else "처음 보내는 계좌", coral = true)
                Space(9); Money(record.intent.amount, hero = false)
                Space(7); Text("${record.intent.recipient.name}님에게", style = MaterialTheme.typography.titleSmall)
                Space(3); Caption("${record.intent.recipient.bank} · ${record.intent.recipient.account}")
            }
            Space(20)
            SectionTitle("이 송금을 멈춘 이유")
            reasons.take(3).forEachIndexed { index, reason ->
                NumberedReason(index + 1, reason.label, accent = index == 0)
                if (index < reasons.take(3).lastIndex) Rule()
            }
            Space(8)
            MenuRow("멈춘 이유 자세히", icon = BankIcons.Link, tag = "hold_reasons_open") { reasonsOpen = true }
            Rule()
            MenuRow("위험 신호의 시간 흐름", icon = BankIcons.History, tag = "hold_timeline_open") { open("timeline") }
            Space(10); Caption("이 화면을 닫아도 자동으로 송금되지 않습니다.")
        }
    }
    if (reasonsOpen) {
        ModalBottomSheet(
            onDismissRequest = { reasonsOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = TraceColors.Surface
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("trace_hold_reason")
                    .padding(horizontal = 20.dp).padding(bottom = 24.dp)
            ) {
                TraceSignature(); Space(16)
                Text("멈춘 이유", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                Space(9); Body("한 가지 신호가 아니라, 이 송금으로 이어진 흐름을 함께 봤어요.", subdued = true)
                Space(18)
                reasons.forEachIndexed { index, reason ->
                    NumberedReason(index + 1, reason.label, reason.explanation, accent = index == 0)
                    if (index < reasons.lastIndex) Rule()
                }
                Space(20)
                PrimaryButton("확인", Modifier.testTag("hold_reasons_close"), accent = true) { reasonsOpen = false }
            }
        }
    }
}

@Composable private fun WarnScreen(
    record: TransferRecord,
    interaction: InteractionState,
    model: BankViewModel,
    back: () -> Unit,
    home: () -> Unit
) {
    var checked by rememberSaveable(record.intent.id) { mutableStateOf(false) }
    Page(title = "보내기 전 확인", tag = "trace_warn", back = back, footer = {
        PrimaryButton(
            "확인한 내용으로 다시 보기",
            Modifier.testTag("warn_acknowledge"),
            enabled = checked && !interaction.busy
        ) { model.acknowledge(record.intent.id) }
        SecondaryButton("송금 취소") { model.cancelTransfer(record.intent.id, home) }
    }) {
        Space(6); TraceSignature(); Space(18)
        MicroLabel("한 번 더 확인", coral = true); Space(7)
        Headline("보내기 전에\n하나만 확인해 주세요.")
        Space(14)
        Text("아직 돈은 나가지 않았습니다.", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        Space(7)
        Body("최근 확인하지 않은 링크와 이 송금이 가까운 시간 안에 이어졌어요.", subdued = true)
        Space(22)
        SurfaceBox(tint = TraceColors.SurfaceRaised) {
            Money(record.intent.amount, hero = false)
            Space(7)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RecipientGlyph(record.intent.recipient.name, Modifier.size(34.dp))
                Body("${record.intent.recipient.name} · ${record.intent.recipient.bank}")
            }
        }
        Space(20)
        NumberedReason(1, "받는 분과 금액을 다른 경로로 확인하세요.", "상대가 보내준 링크가 아니라, 이미 알고 있던 연락처나 공식 앱을 사용하세요.")
        Space(8); Rule()
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp)
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = { checked = it })
                .testTag("warn_check")
                .semantics { stateDescription = if (checked) "확인함" else "확인하지 않음" }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked,
                onCheckedChange = null,
                modifier = Modifier.clearAndSetSemantics {},
                colors = CheckboxDefaults.colors(checkedColor = TraceColors.Ink, uncheckedColor = TraceColors.DividerStrong)
            )
            Text("다른 경로로 받는 분과 금액을 확인했어요.", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        }
        Space(8)
        Caption("확인한 뒤 송금 내역과 인증을 다시 진행합니다. 새 위험 신호가 생기면 다시 멈출 수 있어요.")
    }
}

@Composable private fun VerifyScreen(
    record: TransferRecord,
    interaction: InteractionState,
    model: BankViewModel,
    back: () -> Unit,
    home: () -> Unit
) {
    val person = record.intent.recipient.kind == RecipientKind.PERSON
    Page(title = "상환 경로 확인", tag = "trace_verify", back = back, footer = {
        PrimaryButton(
            if (interaction.routeLoading) "공식 경로를 확인하고 있어요." else "공식 상환 경로 확인",
            Modifier.testTag("verify_route"), enabled = !interaction.busy, accent = true
        ) { model.resolveRoute(record.intent.id) }
        SecondaryButton("송금 취소") { model.cancelTransfer(record.intent.id, home) }
    }) {
        Space(6); TraceSignature(); Space(18); AccentRule(34); Space(16)
        MicroLabel("독립 확인 필요", coral = true); Space(7)
        Headline(if (person) "대출을 갚는 돈이\n개인 계좌로 향하고 있어요." else "등록된 상환 경로를\n다시 확인할게요.")
        Space(14)
        Text("아직 돈은 나가지 않았습니다.", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        Space(7)
        Body(
            if (person) "대출 상환이라고 했지만 받는 곳은 개인 계좌예요. 상대가 준 경로와 분리해서 확인합니다."
            else "이 계좌가 현재 대출에 등록된 상환처인지 확인한 뒤 새 거래로 인증합니다.",
            subdued = true
        )
        Space(22)
        SurfaceBox(tint = TraceColors.CoralWash) {
            MicroLabel(if (person) "안내받은 개인 계좌" else "확인이 필요한 상환 계좌", coral = true)
            Space(8); Money(record.intent.amount, hero = false)
            Space(8); Text(record.intent.recipient.name, style = MaterialTheme.typography.titleSmall)
            Space(3); Caption("${record.intent.recipient.bank} · ${record.intent.recipient.account}")
        }
        Space(22)
        SectionTitle("왜 다시 확인하나요?")
        if (RiskType.LOAN_REPAYMENT_REQUEST in record.reasons) {
            NumberedReason(1, "앞선 요청의 목적", "기존 대출을 먼저 갚으라는 요청이 현재 송금과 이어졌어요.", accent = true)
        } else {
            NumberedReason(1, "보내는 목적", "대출 상환", accent = true)
        }
        Rule()
        NumberedReason(2, "받는 곳", if (person) "확인된 은행 상환 계좌가 아닌 개인 계좌" else "대출에 등록된 경로와 일치하는지 조회 필요")
        Space(16)
        Caption("상대가 알려준 번호나 링크는 사용하지 않습니다. 새온은행 앱 안의 가상 공식 경로를 사용합니다.")
    }
}

@Composable private fun OfficialRouteScreen(
    record: TransferRecord,
    interaction: InteractionState,
    model: BankViewModel,
    back: () -> Unit
) {
    val route = record.route
    Page(title = "확인된 상환 경로", tag = "trace_official_route", back = back, footer = {
        PrimaryButton("새 송금 내역 확인", Modifier.testTag("official_route_use"), enabled = route != null && !interaction.busy) {
            model.useRoute(record.intent.id)
        }
        QuietButton(
            if (interaction.routeLoading) "경로를 다시 확인하고 있어요." else "공식 경로 다시 확인",
            Modifier.fillMaxWidth().testTag("official_route_refresh"),
            enabled = !interaction.busy
        ) { model.resolveRoute(record.intent.id) }
    }) {
        Space(6); TraceSignature(); Space(18)
        MicroLabel("확인된 경로", coral = true); Space(7)
        Headline("보내는 목적에 맞는\n받는 곳을 찾았어요.")
        Space(14)
        Text("아직 송금하지 않았습니다.", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        Space(20)
        if (route != null) {
            SurfaceBox(tint = TraceColors.SurfaceRaised) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(38.dp).background(TraceColors.Soft, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        AppIcon(BankIcons.Bank, size = 20)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(route.recipient.name, style = MaterialTheme.typography.titleSmall)
                        Space(3); Caption("${route.recipient.bank} · ${route.recipient.account}")
                    }
                }
                Space(14); Rule(); Space(10)
                DetailRow("연결된 상품", route.productName)
                DetailRow("보낼 금액", "${won(record.intent.amount)}원", true)
            }
        }
        Space(20)
        EditorialPanel(accent = true) {
            Body("수취인이 바뀌었으므로 새로운 송금입니다.")
            Space(5); Caption("금액과 받는 곳을 다시 보고, 새로 인증해야 합니다.")
        }
        Space(18)
        DetailRow("이전 받는 분", record.intent.recipient.name)
        DetailRow("유지되는 금액", "${won(record.intent.amount)}원", true)
        Space(12); SimulationNote()
    }
}

@Composable private fun UnknownScreen(
    record: TransferRecord,
    interaction: InteractionState,
    model: BankViewModel,
    back: () -> Unit,
    home: () -> Unit
) {
    Page(title = "공식 경로 확인 불가", tag = "trace_unknown", back = back, footer = {
        PrimaryButton(
            if (interaction.routeLoading) "다시 확인하고 있어요." else "공식 경로 다시 확인",
            Modifier.testTag("unknown_retry"), enabled = !interaction.busy, accent = true
        ) { model.resolveRoute(record.intent.id) }
        SecondaryButton("송금 취소") { model.cancelTransfer(record.intent.id, home) }
    }) {
        Space(8); TraceSignature(); Space(18); AccentRule(34); Space(16)
        MicroLabel("확인 실패", coral = true); Space(7)
        Headline("확인할 수 없으면,\n보내지 않습니다.")
        Space(14)
        Text("아직 돈은 나가지 않았습니다.", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        Space(7)
        Body("공식 경로를 확인하지 못했어요. 확인되지 않았다는 건 안전하다는 뜻이 아닙니다.", subdued = true)
        Space(22)
        SurfaceBox(tint = TraceColors.CoralWash) {
            Money(record.intent.amount, hero = false)
            Space(7); Body("${record.intent.recipient.name}님에게 보내려던 돈")
        }
        Space(20)
        NumberedReason(1, "잔액은 바뀌지 않았어요.", accent = true)
        Rule(); NumberedReason(2, "송금 완료 내역을 만들지 않았어요.")
        Rule(); NumberedReason(3, "일반 송금으로 자동 전환하지 않아요.")
        Space(16)
        Caption("확인이 안 될 때는 앱을 닫아도 괜찮아요. 보류 상태는 안전 센터에서 다시 볼 수 있습니다.")
    }
}

@Composable private fun CompleteScreen(state: BankState, record: TransferRecord, open: (String) -> Unit, home: () -> Unit) {
    val receipt = state.receipts.find { it.intentId == record.intent.id && !it.seed }
    if (receipt == null) {
        Page(title = "송금 결과 확인", back = home, footer = { PrimaryButton("거래 내역 확인") { open("history") } }) {
            EmptyState("완료 내역을 다시 확인해 주세요.", "영수증을 읽지 못했어요. 다시 송금하지 말고 거래 내역을 확인해 주세요.")
        }
        return
    }
    Page(title = "", tag = "transfer_complete", footer = {
        PrimaryButton("확인", Modifier.testTag("complete_confirm"), onClick = home)
        SecondaryButton("송금 내역") { open("receipt/${receipt.id}") }
    }) {
        Space(28)
        Box(Modifier.size(44.dp).background(TraceColors.Ink, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            AppIcon(BankIcons.Check, size = 24, tint = TraceColors.White)
        }
        Space(26)
        Money(receipt.amount)
        Space(12)
        Text(
            if (record.intent.purpose == Purpose.LOAN) "공식 경로로\n상환을 마쳤어요."
            else "${receipt.recipient.name}님에게\n보냈어요.",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() }
        )
        Space(24)
        SurfaceBox(tint = TraceColors.SurfaceRaised) {
            DetailRow("받는 분", receipt.recipient.name)
            Rule(); DetailRow("받는 계좌", "${receipt.recipient.bank}\n${receipt.recipient.account}")
            Rule(); DetailRow("출금 계좌", receipt.fromAccount)
            Rule(); DetailRow("보낸 금액", "${won(receipt.amount)}원", true)
            Rule(); DetailRow("수수료", "0원")
            Rule(); DetailRow("시간", dateLabel(receipt.completedAt))
        }
        Space(18)
        Caption("${receipt.id} · 시연 기록")
        Space(8); SimulationNote()
    }
}

