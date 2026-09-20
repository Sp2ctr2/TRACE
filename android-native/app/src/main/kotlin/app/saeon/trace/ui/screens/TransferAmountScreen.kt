package app.saeon.trace.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import app.saeon.trace.ui.*
import app.saeon.trace.ui.design.*

/** Amount entry for an entirely fictional, offline ledger. This view has no
 * banking API or network access and does not itself commit even a demo debit. */
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
        if (normalized.length > 9) { model.showError("입력할 수 있는 금액을 넘었어요. 1억 원 이하로 입력해 주세요. 돈은 나가지 않았습니다."); return }
        digits = normalized
        model.storeDraft(TransferDraft(draft.recipient, normalized.toLongOrNull() ?: 0, purpose))
    }
    Page(title = "송금 금액", tag = "transfer_amount", back = back, footer = {
        PrimaryButton("다음", Modifier.testTag("amount_next"), enabled = valid && !interaction.busy) {
            model.review(localDraft) { open("transfer_state") }
        }
    }) {
        Text("${draft.recipient.name}님에게", style = MaterialTheme.typography.titleMedium)
        Space(4); Headline("얼마를 보낼까요?"); Space(8)
        Caption("${draft.recipient.bank} · ${draft.recipient.account}")
        Space(24)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Money(amount, Modifier.weight(1f).testTag("amount_value"))
            IconAction(BankIcons.Edit, "금액 직접 입력", Modifier.testTag("amount_edit")) { directDigits = digits; directInput = true }
        }
        Space(6); Caption("잔액 ${won(state.balance)}원")
        if (amount > 0 && error != null) ErrorNote(error)
        Space(16)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(10_000L to "+1만", 100_000L to "+10만", 1_000_000L to "+100만").forEach { (addition, label) ->
                QuietButton(label, Modifier.weight(1f), enabled = !interaction.busy) { update((amount + addition).toString()) }
            }
        }
        Space(10); Rule(); Space(10)
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("clear", "0", "backspace"))
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    val label = when (key) { "clear" -> "전체 지우기"; "backspace" -> "한 자리 지우기"; else -> key }
                    TextButton(onClick = {
                        when (key) { "clear" -> update("0"); "backspace" -> update(digits.dropLast(1)); else -> update(if (digits == "0") key else digits + key) }
                    }, enabled = !interaction.busy, modifier = Modifier.weight(1f).heightIn(min = 62.dp).testTag("key_$key")
                        .semantics { contentDescription = label }, shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = TraceColors.Ink)) {
                        when (key) {
                            "clear" -> Text("지우기", style = MaterialTheme.typography.labelMedium)
                            "backspace" -> AppIcon(BankIcons.Delete)
                            else -> Text(key, fontSize = 28.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
        Space(14); Rule()
        MenuRow("송금 목적", "선택 사항", trailing = purpose.label, tag = "transfer_purpose") { purposeSheet = true }
        if (!draft.recipient.known) Caption("처음 보내는 계좌예요. 받는 분과 금액을 한 번 더 확인해 주세요.")
    }
    if (purposeSheet) ModalBottomSheet(onDismissRequest = { purposeSheet = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = TraceColors.Surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("어떤 돈을 보내시나요?", style = MaterialTheme.typography.headlineSmall); Space(12)
            Caption("목적은 선택하지 않아도 괜찮아요. 대출 상환은 받는 경로를 함께 확인합니다."); Space(14)
            Purpose.entries.forEach { option ->
                Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).clickable(role = Role.RadioButton) {
                    purposeName = option.name; model.storeDraft(localDraft.copy(purpose = option)); purposeSheet = false
                }.semantics { selected = purpose == option }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(option.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    if (purpose == option) AppIcon(BankIcons.Check, tint = TraceColors.Deep, size = 20)
                }
            }
        }
    }
    if (directInput) AlertDialog(onDismissRequest = { directInput = false }, title = { Text("송금 금액") },
        text = { Column {
            Field(directDigits, "원", { directDigits = it.filter { char -> char in '0'..'9' }.trimStart('0').take(10) },
                Modifier.testTag("amount_direct_input"), keyboard = KeyboardType.Number)
            Space(12)
            if (directDigits.length > 9 || (directDigits.toLongOrNull() ?: 0) > 100_000_000L)
                ErrorNote("1억 원 이하로 입력해 주세요. 입력한 금액을 줄이거나 취소할 수 있어요. 돈은 나가지 않았습니다.")
            else Caption("${won(directDigits.toLongOrNull() ?: 0)}원")
        } },
        confirmButton = { QuietButton("입력 완료", Modifier.testTag("amount_input_done"),
            enabled = directDigits.length <= 9 && (directDigits.toLongOrNull() ?: 0L) <= 100_000_000L) {
                update(directDigits); directInput = false
            } },
        dismissButton = { QuietButton("취소") { directInput = false } }, containerColor = TraceColors.Surface)
}
