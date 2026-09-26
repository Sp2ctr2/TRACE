package app.saeon.trace.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.saeon.trace.core.*
import app.saeon.trace.data.BankPreferences
import app.saeon.trace.ui.BankViewModel
import app.saeon.trace.ui.design.*

@Composable
fun FinalHome(
    state: BankState,
    preferences: BankPreferences,
    model: BankViewModel,
    open: (String) -> Unit
) {
    TaskPage("새온은행", "home", root = true, actions = {
        IconAction(BankIcons.Bell, "알림") { open("notifications") }
        IconAction(BankIcons.Profile, "내 정보") { open("profile") }
    }) {
        val compact = LocalTaskHeight.current < 455.dp

        Column(
            Modifier.fillMaxWidth()
                .background(TraceColors.Surface, RoundedCornerShape(22.dp))
                .padding(horizontal = 18.dp, vertical = if (compact) 14.dp else 18.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.weight(1f).heightIn(min = 44.dp)
                        .clickable(role = Role.Button) { open("account") },
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("새온 생활통장", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Space(2)
                    Caption("110-***-0001")
                }
                IconAction(
                    BankIcons.Eye,
                    if (preferences.hideBalance) "잔액 보이기" else "잔액 숨기기"
                ) { model.preference { hideBalance(!preferences.hideBalance) } }
            }

            Space(if (compact) 4 else 8)
            if (preferences.hideBalance) {
                Text("잔액 숨김", style = MaterialTheme.typography.headlineLarge)
            } else {
                MotionMoney(state.balance, Modifier.fillMaxWidth().testTag("home_balance"))
            }

            Space(if (compact) 10 else 16)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1.55f)) {
                    PrimaryButton("송금", Modifier.testTag("home_transfer")) { open("transfer") }
                }
                Box(Modifier.weight(1f)) {
                    SecondaryButton("가져오기", Modifier.testTag("home_bring")) { open("bring") }
                }
            }
        }

        Space(if (compact) 10 else 16)

        Column(Modifier.fillMaxWidth()) {
            FinanceRow("이번 달 쓴 돈", "382,400원", BankIcons.Card) { open("card") }
            Rule()
            FinanceRow("모아둔 돈", "${won(state.savings)}원", BankIcons.Assets) { open("savings") }
        }

        Space(if (compact) 4 else 10)
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(TraceColors.CoralLight)
                .clickable(role = Role.Button) { open("safety") }
                .padding(horizontal = 14.dp, vertical = if (compact) 10.dp else 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            AppIcon(BankIcons.Trace, size = 26, tint = TraceColors.Coral)
            Column(Modifier.weight(1f)) {
                Text(
                    if (state.pending.isEmpty()) "보내기 전, 한 번 더." else "보류한 송금 ${state.pending.size}건",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (!compact) Caption(
                    if (state.pending.isEmpty()) "송금 앞의 맥락을 확인해요" else "아직 돈은 나가지 않았어요."
                )
            }
            AppIcon(BankIcons.Chevron, size = 15, tint = TraceColors.Muted)
        }

        Space(if (compact) 8 else 16)
        SectionTitle("최근 거래", "전체") { open("history") }

        state.receipts.take(if (compact) 1 else 3).forEach { receipt ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = if (compact) 52.dp else 58.dp)
                    .clickable(role = Role.Button) { open("receipt/${receipt.id}") },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(36.dp).background(TraceColors.Surface, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(
                        if (receipt.direction == Direction.CREDIT) BankIcons.Download else BankIcons.Card,
                        size = 18,
                        tint = TraceColors.Muted
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(receipt.recipient.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (!compact) Caption(receipt.memo.ifEmpty { receipt.purpose.label })
                }
                Text(
                    "${if (receipt.direction == Direction.DEBIT) "−" else "+"}${won(receipt.amount)}원",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun FinanceRow(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppIcon(icon, size = 19, tint = TraceColors.Muted)
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = TraceColors.Muted)
        Text(value, style = MaterialTheme.typography.titleSmall)
        AppIcon(BankIcons.Chevron, size = 14, tint = TraceColors.Muted)
    }
}

@Composable
fun FinalReview(
    state: BankState,
    record: TransferRecord,
    interaction: app.saeon.trace.ui.InteractionState,
    model: BankViewModel,
    open: (String) -> Unit,
    back: () -> Unit
) {
    val intent = record.intent
    val compact = LocalTaskHeight.current < 500.dp
    val amountError = runCatching {
        BankEngine.validateAmount(state, intent.amount, intent.purpose, model.repository.clock.now())
    }.exceptionOrNull()?.message

    TaskPage("송금 확인", "transfer_review", back = back, footer = {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AppIcon(BankIcons.Trace, size = 17, tint = TraceColors.Coral)
            Text(
                "보내기 전 맥락까지 확인해요",
                Modifier.padding(start = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                color = TraceColors.Muted
            )
        }
        PrimaryButton(
            "${won(intent.amount)}원 보내기",
            Modifier.testTag("transfer_confirm"),
            enabled = amountError == null && !interaction.busy && record.stage == TransferStage.REVIEW
        ) { model.requestAuthorization(intent.id) }
    }) {
        Spacer(Modifier.height(if (compact) 4.dp else 10.dp))

        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PersonBadge(intent.recipient.name, if (compact) 42 else 48, true)
            Space(if (compact) 8 else 12)
            RecipientTitle(intent.recipient.id, intent.recipient.name)
            Space(2)
            Box(Modifier.widthIn(max = 300.dp)) {
                MotionMoney(intent.amount, Modifier.fillMaxWidth(), intent.recipient.id)
            }
            Space(2)
            Text(
                "보낼까요?",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-.35).sp
                )
            )
        }

        Space(if (compact) 14 else 24)

        Column(
            Modifier.fillMaxWidth()
                .background(TraceColors.Surface, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = if (compact) 8.dp else 12.dp)
        ) {
            FinalDetail("받는 계좌", "${intent.recipient.bank}  ${intent.recipient.account}")
            Rule()
            FinalDetail("출금 계좌", "새온 생활통장  110-***-0001")
            Rule()
            FinalDetail("목적 · 수수료", "${intent.purpose.label} · 0원")
        }

        Space(if (compact) 8 else 12)
        QuietButton(
            "금액·목적 수정",
            Modifier.fillMaxWidth().testTag("review_edit"),
            enabled = record.stage == TransferStage.REVIEW && !interaction.busy
        ) { model.editReview(intent.id) { open("amount") } }

        if (intent.officialRouteId != null) {
            Space(8)
            Body("수취 계좌가 바뀌어 새 거래로 인증합니다. 이전 인증은 재사용하지 않아요.", subdued = true)
        }

        amountError?.let { ErrorNote(it) }
        record.error?.takeIf { it != amountError }?.let { ErrorNote(it) }
    }
}

@Composable
private fun FinalDetail(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(label, Modifier.width(76.dp), style = MaterialTheme.typography.bodySmall, color = TraceColors.Muted)
        Text(
            value,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = (-.18).sp),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Medium
        )
    }
}
