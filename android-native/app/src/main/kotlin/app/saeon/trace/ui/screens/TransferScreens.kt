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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saeon.trace.core.*
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
        Space(12); Headline("누구에게 보낼까요?"); Space(24)
        Field(query, "이름·은행·계좌 검색", { query = it }, Modifier.testTag("recipient_search"))
        Space(14)
        MenuRow("계좌번호로 보내기", "새로운 가상 계좌를 확인해요.", BankIcons.Bank, tag = "recipient_account_entry") { open("recipient_entry") }
        Rule(); Space(22)
        if (query.isNotBlank()) {
            SectionTitle("검색 결과")
            if (matches.isEmpty()) EmptyState("찾는 계좌가 없어요.", "이름이나 계좌 끝자리를 다시 입력해 주세요.")
            matches.forEach { recipient -> RecipientRow(recipient) { model.startRecipient(recipient) { open("amount") } } }
        } else {
            SectionTitle("최근 보낸 사람")
            recent.forEach { recipient -> RecipientRow(recipient) { model.startRecipient(recipient) { open("amount") } } }
            if (favorites.isNotEmpty()) {
                Space(24); Rule(); Space(16); SectionTitle("자주 보내는 사람")
                Column(Modifier.testTag("favorite_recipient_list")) {
                    favorites.forEach { recipient -> RecipientRow(recipient) { model.startRecipient(recipient) { open("amount") } } }
                }
            }
            val other = state.recipients.filter { candidate -> recent.none { it.id == candidate.id } && favorites.none { it.id == candidate.id } }
            if (other.isNotEmpty()) {
                Space(24); Rule(); Space(16); SectionTitle("확인한 계좌")
                other.forEach { recipient -> RecipientRow(recipient) { model.startRecipient(recipient) { open("amount") } } }
            }
        }
        Space(20); Caption("은행과 수취인은 모두 시연용 가상 데이터입니다.")
    }
}

@Composable private fun RecipientRow(recipient: Recipient, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 82.dp).clickable(role = Role.Button, onClick = onClick)
        .testTag("recipient_${recipient.id}").padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f)) {
            Text(recipient.name, style = MaterialTheme.typography.titleSmall)
            Space(5); Caption("${recipient.bank} · ${recipient.account}")
        }
        AppIcon(BankIcons.Chevron, size = 18, tint = TraceColors.Muted)
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
