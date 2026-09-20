package app.saeon.trace.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import app.saeon.trace.core.*
import app.saeon.trace.ui.*
import app.saeon.trace.ui.design.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun HoldScreen(record: TransferRecord, easy: Boolean, model: BankViewModel,
                                   open: (String) -> Unit, back: () -> Unit, home: () -> Unit) {
    var reasonsOpen by rememberSaveable(record.intent.id) { mutableStateOf(false) }
    val recipient = record.intent.recipient
    val payeePhrase = recipient.name + if (recipient.kind == RecipientKind.INSTITUTION) "로" else "님에게"
    val reasons = listOf(RiskType.IMPERSONATION, RiskType.URGENCY, RiskType.NEW_RECIPIENT, RiskType.SUSPICIOUS_LINK, RiskType.FINANCIAL_INSTRUCTION)
        .filter { it in record.reasons }
    Page(title = "송금 보류", tag = "trace_hold", back = back, footer = {
        PrimaryButton(if (easy) "공식 경로로 확인" else "안전하게 확인하기", Modifier.testTag("hold_safe_action")) { open("safety_guide") }
        SecondaryButton("송금 취소", Modifier.testTag("hold_cancel")) { model.cancelTransfer(record.intent.id, home) }
    }) {
        Space(8); TraceSignature(); Space(24)
        if (easy) {
            Headline("송금을 잠시\n멈췄습니다.", Modifier.testTag("easy_mode")); Space(24)
            Text("아직 돈은 나가지 않았습니다.", style = MaterialTheme.typography.titleMedium)
            Space(24); Body("상대가 기관을 사칭했을 가능성이 있습니다.")
            Space(28); Money(record.intent.amount, hero = false); Space(8); Body("$payeePhrase 보내려던 돈")
            Space(24); Body("상대가 준 번호나 링크가 아닌, 은행 앱의 공식 경로로 확인하세요.", subdued = true)
        } else {
            Headline("잠깐,\n보내지 않아도 괜찮아요."); Space(14)
            Body("아직 돈은 나가지 않았습니다.")
            Space(8)
            Body(if (RiskType.URGENCY in record.reasons) "기관 사칭과 급한 송금 요청이 이 거래와 연결돼, 잠시 멈췄어요."
                else "기관 사칭 정황과 송금 요청이 이 거래와 연결돼, 잠시 멈췄어요.", subdued = true)
            Space(24)
            SurfaceBox {
                Caption(when { recipient.known -> "저장된 수취인"; recipient.kind == RecipientKind.INSTITUTION -> "기관 수취 계좌"; else -> "처음 보내는 계좌" })
                Space(8); Money(record.intent.amount, hero = false)
                Space(6); Text(payeePhrase, style = MaterialTheme.typography.titleSmall)
                Space(6); Caption("${recipient.bank} · ${recipient.account}")
            }
            Space(18)
            reasons.take(3).forEachIndexed { index, reason -> NumberedReason(index + 1, reason.label, accent = index == 0) }
            Rule()
            MenuRow("멈춘 이유", icon = BankIcons.Link, tag = "hold_reasons_open") { reasonsOpen = true }
            MenuRow("위험 신호가 이어진 흐름", icon = BankIcons.History, tag = "hold_timeline_open") { open("timeline") }
            Space(12); Caption("이 화면을 닫아도 자동으로 송금되지 않습니다.")
        }
    }
    if (reasonsOpen) ModalBottomSheet(onDismissRequest = { reasonsOpen = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = TraceColors.Surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("trace_hold_reason").padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("멈춘 이유", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Space(12); Body("한 가지 신호가 아니라, 이 송금으로 이어진 흐름을 함께 봤어요.", subdued = true)
            Space(18)
            reasons.forEachIndexed { index, reason -> NumberedReason(index + 1, reason.label, reason.explanation); if (index < reasons.lastIndex) Rule() }
            Space(20); PrimaryButton("확인", Modifier.testTag("hold_reasons_close")) { reasonsOpen = false }
        }
    }
}
