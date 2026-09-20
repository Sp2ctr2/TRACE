package app.saeon.trace.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import app.saeon.trace.core.*
import app.saeon.trace.data.BankPreferences
import app.saeon.trace.ui.*
import app.saeon.trace.ui.design.*

@Composable fun TransferStateScreen(state: BankState, preferences: BankPreferences, interaction: InteractionState,
                                   model: BankViewModel, open: (String) -> Unit, back: () -> Unit, home: () -> Unit) {
    val record = state.current
    if (record == null) {
        Page(title = "송금", back = back, footer = { PrimaryButton("받는 분 선택") { open("transfer") } }) {
            EmptyState("진행 중인 송금이 없어요.", "보류된 송금은 안전 센터에서, 완료된 송금은 거래 내역에서 볼 수 있어요.")
        }
        return
    }
    BackHandler(enabled = record.stage == TransferStage.EVALUATING) { model.cancelAuthorization() }
    when (record.stage) {
        TransferStage.REVIEW, TransferStage.AUTHORIZING -> ReviewScreen(record, interaction, model, open, back)
        TransferStage.EVALUATING -> EvaluatingScreen { model.cancelAuthorization() }
        TransferStage.HOLD -> HoldScreen(record, preferences.easyMode, model, open, back, home)
        TransferStage.WARN -> WarnScreen(record, interaction, model, back, home)
        TransferStage.VERIFY -> VerifyScreen(record, interaction, model, back, home)
        TransferStage.ROUTE -> OfficialRouteScreen(record, interaction, model, back)
        TransferStage.UNKNOWN -> UnknownScreen(record, interaction, model, back, home)
        TransferStage.COMPLETE -> CompleteScreen(state, record, open, home)
        TransferStage.CANCELLED, TransferStage.SUPERSEDED -> Page(title = "송금", back = back, footer = { PrimaryButton("홈으로", onClick = home) }) {
            EmptyState("종료된 송금입니다.", "이 거래를 다시 실행하지 않습니다. 현재 잔액과 거래 내역은 홈에서 확인해 주세요.")
        }
    }
}

@Composable private fun ReviewScreen(record: TransferRecord, interaction: InteractionState, model: BankViewModel,
                                     open: (String) -> Unit, back: () -> Unit) {
    val intent = record.intent
    Page(title = "보내기 전 확인", tag = "transfer_review", back = back, footer = {
        PrimaryButton("${won(intent.amount)}원 보내기", Modifier.testTag("transfer_confirm"),
            enabled = !interaction.busy && record.stage == TransferStage.REVIEW) { model.requestAuthorization(intent.id) }
    }) {
        Space(18); Money(intent.amount); Space(14)
        Text(if (intent.recipient.kind == RecipientKind.INSTITUTION) "${intent.recipient.name}로" else "${intent.recipient.name}님에게",
            style = MaterialTheme.typography.headlineSmall)
        Space(30); Rule(); Space(12)
        DetailRow("받는 계좌", "${intent.recipient.bank}\n${intent.recipient.account}")
        DetailRow("출금 계좌", "새온 생활통장\n110-***-0001")
        DetailRow("수수료", "0원")
        if (intent.purpose != Purpose.GENERAL) DetailRow("송금 목적", intent.purpose.label)
        Space(12); Rule(); Space(20)
        if (intent.officialRouteId != null) {
            SurfaceBox {
                TraceSignature(); Space(10)
                Body("수취 계좌가 바뀌어 새 거래로 확인합니다. 이전 인증은 사용하지 않아요.")
            }
        } else {
            Caption("예금주 확인은 시연 데이터입니다. 이름이 일치해도 상대의 요청이 안전하다는 뜻은 아니에요.")
            Space(12)
            QuietButton("금액·목적 수정", Modifier.testTag("review_edit"), enabled = record.stage == TransferStage.REVIEW && !interaction.busy) {
                model.editReview(intent.id) { open("amount") }
            }
        }
        record.error?.let { ErrorNote(it) }
        Space(24); SimulationNote()
    }
}

@Composable private fun EvaluatingScreen(cancel: () -> Unit) {
    Page(title = "송금 확인", tag = "trace_evaluating", footer = { SecondaryButton("취소하고 돌아가기", onClick = cancel) }) {
        Space(36); TraceSignature(); Space(28)
        Headline("송금 앞의 맥락을\n확인하고 있어요."); Space(36)
        NumberedReason(1, "요청의 목적"); Rule()
        NumberedReason(2, "최근 위험 신호"); Rule()
        NumberedReason(3, "수취인과 거래"); Space(28)
        Caption("인증한 거래와 현재 정황이 같은지 확인합니다.")
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
        Space(36); AppIcon(BankIcons.Check, size = 42); Space(30)
        Money(receipt.amount); Space(16)
        Text(if (record.intent.purpose == Purpose.LOAN) "공식 경로로\n상환을 마쳤어요." else "${receipt.recipient.name}님에게\n보냈어요.",
            style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Space(28); Rule(); Space(10)
        DetailRow("받는 분", receipt.recipient.name)
        DetailRow("받는 계좌", "${receipt.recipient.bank}\n${receipt.recipient.account}")
        DetailRow("출금 계좌", receipt.fromAccount)
        DetailRow("보낸 금액", "${won(receipt.amount)}원", true)
        DetailRow("수수료", "0원")
        DetailRow("시간", dateLabel(receipt.completedAt))
        Space(20); Caption("${receipt.id} · 시연 기록")
        Space(10); SimulationNote()
    }
}
