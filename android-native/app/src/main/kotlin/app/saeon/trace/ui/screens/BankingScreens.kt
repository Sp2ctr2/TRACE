package app.saeon.trace.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.saeon.trace.core.*
import app.saeon.trace.data.BankPreferences
import app.saeon.trace.ui.BankViewModel
import app.saeon.trace.ui.design.*

@Composable
fun HomeScreen(state: BankState, preferences: BankPreferences, model: BankViewModel, open: (String) -> Unit) {
    Page(title = "새온은행", tag = "home", actions = {
        IconAction(BankIcons.Bell, "알림") { open("notifications") }
        IconAction(BankIcons.Profile, "내 정보") { open("profile") }
    }) {
        Space(8)
        SurfaceBox(tint = TraceColors.SurfaceRaised) {
            AccentRule(28)
            Space(18)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    MicroLabel("MAIN ACCOUNT")
                    Space(5)
                    Text("새온 생활통장", style = MaterialTheme.typography.titleMedium)
                }
                IconAction(BankIcons.Eye, if (preferences.hideBalance) "잔액 보이기" else "잔액 숨기기") {
                    model.preference { hideBalance(!preferences.hideBalance) }
                }
            }
            Space(16)
            if (preferences.hideBalance) {
                Text("잔액 숨김", Modifier.testTag("home_balance"), style = MaterialTheme.typography.displaySmall)
            } else {
                Money(state.balance, Modifier.testTag("home_balance"))
            }
            Space(7)
            Caption("110-***-0001")
        }

        Space(12)
        val quickActions = listOf(
            Triple("송금", BankIcons.Transfer, "transfer"),
            Triple("가져오기", BankIcons.Download, "bring"),
            Triple("내역", BankIcons.History, "history")
        )
        val largeQuickActions = LocalDensity.current.fontScale >= 1.5f
        if (largeQuickActions) {
            Column(Modifier.fillMaxWidth()) {
                quickActions.forEachIndexed { index, (label, icon, route) ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 62.dp)
                            .clickable(role = Role.Button) { open(route) }
                            .testTag(if (index == 0) "home_transfer" else "home_$route")
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(Modifier.size(36.dp).background(TraceColors.Soft, CircleShape), contentAlignment = Alignment.Center) {
                            AppIcon(icon, size = 18, tint = TraceColors.InkSoft)
                        }
                        Text(label, Modifier.weight(1f).testTag("home_action_label_$route"),
                            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        AppIcon(BankIcons.Chevron, size = 16, tint = TraceColors.Muted)
                    }
                    if (index < quickActions.lastIndex) Rule()
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickActions.forEachIndexed { index, (label, icon, route) ->
                    Column(
                        Modifier.weight(1f).heightIn(min = 74.dp)
                            .clickable(role = Role.Button) { open(route) }
                            .testTag(if (index == 0) "home_transfer" else "home_$route")
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(Modifier.size(36.dp).background(TraceColors.Soft, CircleShape), contentAlignment = Alignment.Center) {
                            AppIcon(icon, size = 18, tint = TraceColors.InkSoft)
                        }
                        Space(7)
                        Text(label, Modifier.testTag("home_action_label_$route"), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Space(10); Rule(); Space(18)
        SectionTitle("최근 거래", "전체 내역") { open("history") }
        state.receipts.take(3).forEachIndexed { index, receipt ->
            ReceiptRow(receipt) { open("receipt/${receipt.id}") }
            if (index < state.receipts.take(3).lastIndex) Rule()
        }

        if (state.recurringEnabled) {
            Space(22); Rule(); Space(18)
            SectionTitle("다가오는 자동이체")
            EditorialPanel {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("통신비", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Space(4); Caption("09월 25일 · 새온 생활통장")
                    }
                    Text("68,000원", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Space(24); Rule(); Space(16)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(role = Role.Button) { open("safety") },
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(BankIcons.Trace, null, Modifier.size(22.dp), tint = TraceColors.Coral)
            Column(Modifier.weight(1f)) {
                Text(if (state.pending.isEmpty()) "TRACE 안전 확인" else "확인이 필요한 송금 ${state.pending.size}건",
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Space(3)
                Caption(if (state.pending.isEmpty()) "최근 연결된 위험 정황 없음" else "아직 돈은 나가지 않았어요. 확인이 필요합니다.")
            }
            AppIcon(BankIcons.Chevron, size = 17, tint = TraceColors.Muted)
        }
        Space(14); SimulationNote()
    }
}

@Composable fun ReceiptRow(receipt: TransferReceipt, onClick: () -> Unit) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    val amount = "${if (receipt.direction == Direction.DEBIT) "−" else "+"}${won(receipt.amount)}원"
    val base = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
        .heightIn(min = if (largeText) 94.dp else 72.dp).padding(vertical = 12.dp)
    if (largeText) {
        Column(base, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(receipt.recipient.name, Modifier.testTag("receipt_name_${receipt.id}"),
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(amount, Modifier.testTag("receipt_amount_${receipt.id}"),
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                maxLines = 1, softWrap = false)
            Caption("${dateLabel(receipt.completedAt).substring(5)} · ${receipt.memo.ifEmpty { receipt.purpose.label }}")
        }
    } else {
        Row(base, verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(receipt.recipient.name, Modifier.testTag("receipt_name_${receipt.id}"),
                    style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Space(4)
                Caption("${dateLabel(receipt.completedAt).substring(5)} · ${receipt.memo.ifEmpty { receipt.purpose.label }}")
            }
            Text(amount, Modifier.testTag("receipt_amount_${receipt.id}"),
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End, maxLines = 1, softWrap = false)
        }
    }
}

@Composable fun AssetsScreen(state: BankState, open: (String) -> Unit) {
    Page(title = "내 자산", tag = "assets") {
        Space(10); MicroLabel("TOTAL ASSETS"); Space(8)
        Money(state.balance + state.savings)
        Space(8); Caption("대출 잔액은 보유 자산과 분리해서 보여드려요.")
        Space(28); Rule(); Space(14)

        SectionTitle("내 계좌")
        MenuRow("새온 생활통장", "110-***-0001", BankIcons.Bank, tag = "asset_primary_account", trailing = "${won(state.balance)}원") { open("account") }
        Rule()
        MenuRow("새온 모아적금", "220-***-0102", BankIcons.Assets, trailing = "${won(state.savings)}원") { open("savings") }

        Space(24); Rule(); Space(14)
        SectionTitle("카드·대출")
        MenuRow("새온 체크카드", "이번 달 이용", BankIcons.Card, trailing = "382,400원") { open("card") }
        Rule()
        MenuRow(Fixtures.LOAN_NAME, "남은 대출 원금", BankIcons.Bank, trailing = "${won(state.loanBalance)}원") { open("loan") }
        Space(22); SimulationNote()
    }
}

@Composable fun AccountScreen(state: BankState, open: (String) -> Unit, back: () -> Unit) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    Page(title = "새온 생활통장", tag = "account_detail", back = back) {
        Space(8); MicroLabel("AVAILABLE BALANCE"); Space(8); Money(state.balance); Space(8)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Caption("새온은행 · 110-***-0001", Modifier.weight(1f))
            IconAction(BankIcons.Copy, "가상 계좌 정보 복사") {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                    ClipData.newPlainText("새온 시연 계좌", "새온은행 · 110-***-0001 · 실제 입금 불가"))
                copied = true
            }
        }
        if (copied) Caption("마스킹된 시연 계좌 정보를 복사했어요.")
        Space(20)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { open("transfer") }, modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TraceColors.Ink, contentColor = TraceColors.White),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) { Text("보내기", style = MaterialTheme.typography.labelLarge) }
            OutlinedButton(
                onClick = { open("bring") }, modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, TraceColors.DividerStrong),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TraceColors.Ink)
            ) { Text("가져오기", style = MaterialTheme.typography.labelLarge) }
        }
        Space(26); Rule(); Space(14)
        SectionTitle("최근 거래", "전체 내역") { open("history") }
        state.receipts.take(6).forEachIndexed { index, receipt ->
            ReceiptRow(receipt) { open("receipt/${receipt.id}") }
            if (index < state.receipts.take(6).lastIndex) Rule()
        }
        Space(22); Rule(); Space(10)
        DetailRow("계좌 종류", "입출금 통장")
        DetailRow("송금 수수료", "0원")
        Space(12); SimulationNote()
    }
}

@Composable fun SavingsScreen(state: BankState, open: (String) -> Unit, back: () -> Unit) {
    Page(title = "새온 모아적금", tag = "savings_detail", back = back,
        footer = { PrimaryButton("생활통장으로 가져오기", enabled = state.savings > 0) { open("bring") } }) {
        Space(16); Caption("현재 모은 금액"); Space(8); Money(state.savings); Space(8); Caption("새온은행 220-***-0102")
        Space(30); Rule(); Space(12)
        DetailRow("계좌 유형", "가상 자유저축")
        DetailRow("시작일", "2026.03.20")
        DetailRow("저축 목표", "3,000,000원")
        DetailRow("출금 가능 금액", "${won(state.savings)}원")
        Space(24); Body("이 시연 계좌에서는 생활통장으로 돈을 가져올 수 있어요. 두 계좌의 합계는 바뀌지 않습니다.", subdued = true)
        Space(24); SimulationNote()
    }
}

@Composable fun CardScreen(state: BankState, open: (String) -> Unit, back: () -> Unit) {
    Page(title = "새온 체크카드", back = back, tag = "card_detail") {
        Space(16); Caption("9월 이용 금액"); Space(8); Money(382_400); Space(30); Rule(); Space(12)
        DetailRow("카드", "새온 데일리 체크 · 0824")
        DetailRow("결제 계좌", "새온 생활통장")
        DetailRow("이번 달 남은 혜택", "추가 청구 없음 · 시연")
        Space(24); SectionTitle("최근 이용 내역")
        state.receipts.filter { it.recipient.id == "cafe" }.forEach { ReceiptRow(it) { open("receipt/${it.id}") } }
        MenuRow("나머지 9월 이용", "식비·교통·생활", trailing = "376,900원")
        Space(20); Caption("카드 사용액은 준비된 월간 명세서입니다. 새 송금은 카드 이용액에 포함되지 않아요.")
    }
}

@Composable fun LoanScreen(state: BankState, model: BankViewModel, open: (String) -> Unit, back: () -> Unit) {
    Page(title = Fixtures.LOAN_NAME, tag = "loan_detail", back = back,
        footer = { PrimaryButton("공식 상환 경로 확인", enabled = state.loanBalance > 0) {
            model.act {
                model.repository.setDraft(TransferDraft(Fixtures.official, state.loanBalance.coerceAtMost(state.balance), Purpose.LOAN))
                model.repository.reviewDraft()
                open("transfer_state")
            }
        } }) {
        Space(16); Caption("남은 대출 원금"); Space(8); Money(state.loanBalance); Space(30); Rule(); Space(12)
        DetailRow("상품", Fixtures.LOAN_NAME)
        DetailRow("상환 기관", "새온은행")
        DetailRow("등록된 상환처", "새온은행 대출상환센터")
        DetailRow("상환 계좌", "200-***-3014")
        Space(24); Body(if (state.loanBalance == 0L) "등록된 대출의 가상 상환을 마쳤어요." else
            "상환은 은행에 등록된 경로에서만 진행하세요. 개인에게 안내받은 계좌와는 다를 수 있어요.", subdued = true)
        Space(24); Caption("공식 경로 확인 후 새 거래 내역과 인증을 다시 확인합니다.")
    }
}

@Composable fun BringScreen(state: BankState, model: BankViewModel, open: (String) -> Unit, back: () -> Unit) {
    var amount by rememberSaveable { mutableStateOf("100000") }
    val operationId = rememberSaveable { newId() }
    val number = amount.toLongOrNull() ?: 0
    val valid = number > 0 && number <= state.savings
    Page(title = "가져오기", tag = "bring", back = back,
        footer = { PrimaryButton("${won(number)}원 가져오기", enabled = valid) {
            model.act {
                val updated = model.repository.bring(number, operationId)
                updated.receipts.find { it.intentId == operationId }?.let { open("receipt/${it.id}") }
            }
        } }) {
        Headline("내 계좌 사이에서\n옮길 금액을 알려주세요."); Space(24)
        SurfaceBox { DetailRow("보내는 계좌", "새온 모아적금"); DetailRow("받는 계좌", "새온 생활통장") }
        Space(24); Field(amount, "가져올 금액", { amount = it.filter(Char::isDigit).take(9) }, keyboard = KeyboardType.Number)
        Space(14); Caption("가져올 수 있는 금액 ${won(state.savings)}원")
        if (!valid && number > 0) ErrorNote("모아적금 잔액보다 큰 금액이에요. 아직 잔액은 바뀌지 않았습니다.")
        Space(24); Body("내 계좌 사이의 가상 이동입니다. 두 계좌를 합한 보유 자산은 변하지 않아요.", subdued = true)
    }
}

@Composable fun HistoryScreen(state: BankState, open: (String) -> Unit, back: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var period by rememberSaveable { mutableIntStateOf(30) }
    val currentTime = state.simulatedNow
    val rows = state.receipts.filter { item ->
        (query.isBlank() || item.recipient.name.contains(query) || item.memo.contains(query) || won(item.amount).contains(query)) &&
            (filter == 0 || (filter == 1 && item.direction == Direction.CREDIT) || (filter == 2 && item.direction == Direction.DEBIT)) &&
            (period == 0 || currentTime - item.completedAt <= period * 86_400_000L)
    }
    Page(title = "거래 내역", tag = "history", back = back) {
        Money(state.balance, hero = false); Space(20)
        Field(query, "이름·금액·내용 검색", { query = it }, Modifier.testTag("history_search"))
        Space(14)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("전체", "입금", "출금").forEachIndexed { index, title ->
                FilterChip(selected = filter == index, onClick = { filter = index }, label = { Text(title) },
                    modifier = Modifier.heightIn(min = 48.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TraceColors.Ink, selectedLabelColor = TraceColors.White))
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Caption(if (period == 0) "전체 기간 · ${rows.size}건" else "최근 ${period}일 · ${rows.size}건", Modifier.weight(1f))
            QuietButton("기간 변경") { period = when (period) { 30 -> 7; 7 -> 0; else -> 30 } }
        }
        Rule(); Space(8)
        if (rows.isEmpty()) EmptyState("해당하는 거래가 없어요.", "검색어와 입출금 구분, 기간을 다시 확인해 주세요. 보류된 송금은 완료 내역에 표시되지 않아요.")
        rows.forEach { ReceiptRow(it) { open("receipt/${it.id}") }; Rule() }
        Space(24); Caption("완료된 가상 거래만 표시합니다. 보류·확인 중인 송금은 안전 센터에 있어요.")
        MenuRow("보류된 송금 보기", icon = BankIcons.Pause) { open("pending") }
    }
}

@Composable fun ReceiptScreen(receipt: TransferReceipt?, open: (String) -> Unit, back: () -> Unit) {
    val context = LocalContext.current
    if (receipt == null) {
        Page(title = "송금 내역", back = back, footer = { PrimaryButton("전체 내역 보기") { open("history") } }) {
            EmptyState("이 영수증을 찾지 못했어요.", "시연 데이터가 초기화되었을 수 있어요. 송금은 새로 실행하지 않았습니다. 전체 내역에서 확인해 주세요.")
        }
        return
    }
    Page(title = if (receipt.direction == Direction.CREDIT) "입금 확인" else "송금 확인", tag = "receipt", back = back,
        actions = { IconAction(BankIcons.Share, "시연 영수증 공유") {
            val text = "새온은행 시연 영수증\n${receipt.id}\n${receipt.recipient.name} · ${won(receipt.amount)}원\n${dateLabel(receipt.completedAt)}\n시연용 가상 거래 · 실제 자금 이동 없음"
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "시연 영수증 공유"))
        } }, footer = { PrimaryButton("확인", onClick = back) }) {
        Space(16); Money(receipt.amount); Space(12)
        Text(if (receipt.direction == Direction.CREDIT) "${receipt.recipient.name}에서 들어왔어요." else "${receipt.recipient.name}님에게 보냈어요.", style = MaterialTheme.typography.titleMedium)
        Space(28); Rule(); Space(10)
        DetailRow(if (receipt.direction == Direction.CREDIT) "보낸 곳" else "받는 분", receipt.recipient.name)
        DetailRow("계좌", "${receipt.recipient.bank}\n${receipt.recipient.account}")
        DetailRow("출금 계좌", receipt.fromAccount)
        DetailRow("금액", "${won(receipt.amount)}원", true)
        DetailRow("수수료", "0원")
        DetailRow("시간", dateLabel(receipt.completedAt))
        DetailRow("기록 번호", receipt.id)
        Space(20); SimulationNote()
        Space(12); Caption("이 화면은 실제 금융 거래를 증명하는 영수증이 아닙니다.")
    }
}
