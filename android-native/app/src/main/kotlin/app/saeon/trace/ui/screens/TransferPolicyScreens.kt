package app.saeon.trace.ui.screens

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
import androidx.compose.ui.unit.dp
import app.saeon.trace.core.*
import app.saeon.trace.ui.*
import app.saeon.trace.ui.design.*

/** Policy explanations for the local demonstration; every displayed institution
 * and transfer is fictional, and official-route lookup uses an offline fixture. */
@Composable internal fun WarnScreen(record: TransferRecord, interaction: InteractionState, model: BankViewModel, back: () -> Unit, home: () -> Unit) {
    var checked by rememberSaveable(record.intent.id) { mutableStateOf(false) }
    Page(title = "보내기 전 확인", tag = "trace_warn", back = back, footer = {
        PrimaryButton("확인한 내용으로 다시 보기", Modifier.testTag("warn_acknowledge"), enabled = checked && !interaction.busy) { model.acknowledge(record.intent.id) }
        SecondaryButton("송금 취소") { model.cancelTransfer(record.intent.id, home) }
    }) {
        Space(12); TraceSignature(); Space(24); Headline("보내기 전에\n하나만 확인해 주세요.")
        Space(16); Body("아직 돈은 나가지 않았습니다.")
        Space(10)
        Body(if (RiskType.SUSPICIOUS_LINK in record.reasons) "최근 확인하지 않은 링크와 이 송금이 가까운 시간 안에 이어졌어요."
            else "서둘러 보내라는 요청 뒤에 처음 보내는 계좌가 입력됐어요.", subdued = true)
        Space(28); SurfaceBox { Money(record.intent.amount, hero = false); Space(8); Body("${record.intent.recipient.name} · ${record.intent.recipient.bank}") }
        Space(24); NumberedReason(1, "받는 분과 금액을 확인하세요.", "상대가 보내준 링크가 아닌, 이미 알고 있던 연락처나 공식 앱에서 확인하세요.")
        Space(12); Rule()
        Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).toggleable(value = checked, role = Role.Checkbox) { checked = it }
            .testTag("warn_check").semantics { stateDescription = if (checked) "확인함" else "확인하지 않음" }.padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Checkbox(checked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = TraceColors.Ink))
            Text("다른 경로로 받는 분과 금액을 확인했어요.", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        }
        Space(12); Caption("확인 뒤 송금 내역과 인증을 다시 진행합니다. 새 위험 신호가 있으면 보류될 수 있어요.")
    }
}

@Composable internal fun VerifyScreen(record: TransferRecord, interaction: InteractionState, model: BankViewModel, back: () -> Unit, home: () -> Unit) {
    val person = record.intent.recipient.kind == RecipientKind.PERSON
    Page(title = "상환 경로 확인", tag = "trace_verify", back = back, footer = {
        PrimaryButton(if (interaction.routeLoading) "공식 경로를 확인하고 있어요." else "공식 상환 경로 확인", Modifier.testTag("verify_route"), enabled = !interaction.busy) { model.resolveRoute(record.intent.id) }
        SecondaryButton("송금 취소") { model.cancelTransfer(record.intent.id, home) }
    }) {
        Space(12); TraceSignature(); Space(24); Headline(if (person) "대출을 갚는\n돈이 맞나요?" else "등록된 상환 경로를\n다시 확인할게요.")
        Space(16); Body("아직 돈은 나가지 않았습니다.")
        Space(10); Body(if (person) "안내받은 계좌는 개인 계좌예요. 공식 상환 경로를 먼저 확인할게요." else
            "이 계좌가 현재 대출에 등록된 상환처인지 확인한 뒤, 새 거래로 인증합니다.", subdued = true)
        Space(28)
        SurfaceBox { Caption(if (person) "안내받은 개인 계좌" else "조회가 필요한 상환 계좌"); Space(8)
            Money(record.intent.amount, hero = false); Space(10); Body(record.intent.recipient.name)
            Space(6); Caption("${record.intent.recipient.bank} · ${record.intent.recipient.account}") }
        Space(26)
        if (record.intent.purpose == Purpose.LOAN) NumberedReason(1, "보내는 목적", "대출 상환")
        else NumberedReason(1, "최근 요청의 목적", "직전에 대출을 먼저 갚으라는 요청이 있었어요.")
        Rule(); NumberedReason(2, "받는 곳", if (person) "확인된 은행 상환 계좌가 아닌 개인 계좌" else "대출에 등록된 경로와 일치하는지 조회 필요")
        Space(20); Caption("상대가 알려준 번호나 링크로 확인하지 않아요. 새온은행에 준비된 시연 응답을 사용합니다.")
    }
}

@Composable internal fun OfficialRouteScreen(record: TransferRecord, interaction: InteractionState, model: BankViewModel, back: () -> Unit) {
    val route = record.route
    Page(title = "확인된 상환 경로", tag = "trace_official_route", back = back, footer = {
        PrimaryButton("새 송금 내역 확인", Modifier.testTag("official_route_use"), enabled = route != null && !interaction.busy) { model.useRoute(record.intent.id) }
        QuietButton(if (interaction.routeLoading) "경로를 다시 확인하고 있어요." else "공식 경로 다시 확인", Modifier.fillMaxWidth().testTag("official_route_refresh"), enabled = !interaction.busy) { model.resolveRoute(record.intent.id) }
    }) {
        Space(12); TraceSignature(); Space(24); Headline("보내는 목적에 맞는\n받는 곳을 찾았어요.")
        Space(16); Body("아직 송금하지 않았습니다.")
        Space(28)
        if (route != null) SurfaceBox {
            AppIcon(BankIcons.Bank); Space(16)
            Text(route.recipient.name, style = MaterialTheme.typography.titleMedium); Space(10)
            Caption("${route.recipient.bank} · ${route.recipient.account}"); Space(8); Body(route.productName)
        }
        Space(24); DetailRow("이전 받는 분", record.intent.recipient.name)
        DetailRow("유지되는 금액", "${won(record.intent.amount)}원", true)
        Space(18); Rule(); Space(24)
        Body("수취인이 바뀌었으므로 새로운 송금입니다. 내역을 다시 보고, 새로 인증해야 해요.")
        Space(20); Caption("새온은행 Demo Gateway의 등록된 가상 응답입니다. 실제 은행 조회가 아닙니다.")
    }
}

@Composable internal fun UnknownScreen(record: TransferRecord, interaction: InteractionState, model: BankViewModel, back: () -> Unit, home: () -> Unit) {
    Page(title = "공식 경로 확인 불가", tag = "trace_unknown", back = back, footer = {
        PrimaryButton(if (interaction.routeLoading) "다시 확인하고 있어요." else "공식 경로 다시 확인", Modifier.testTag("unknown_retry"), enabled = !interaction.busy) { model.resolveRoute(record.intent.id) }
        SecondaryButton("송금 취소") { model.cancelTransfer(record.intent.id, home) }
    }) {
        Space(12); AppIcon(BankIcons.Lock, tint = TraceColors.Deep, size = 30); Space(26)
        Headline("확인할 수 없으면,\n보내지 않습니다.")
        Space(18); Body("아직 돈은 나가지 않았습니다.")
        Space(12); Body("공식 경로를 확인하지 못했어요. 확인되지 않았다는 건 안전하다는 뜻이 아닙니다.", subdued = true)
        Space(28); SurfaceBox { Money(record.intent.amount, hero = false); Space(10); Body("${record.intent.recipient.name}님에게 보내려던 돈") }
        Space(26); NumberedReason(1, "잔액은 바뀌지 않았어요.")
        Rule(); NumberedReason(2, "송금 완료 내역을 만들지 않았어요.")
        Rule(); NumberedReason(3, "일반 송금으로 전환하지 않아요.")
        Space(20); Caption("확인이 안 될 때는 앱을 닫아도 괜찮아요. 보류 상태는 안전 센터에서 다시 볼 수 있습니다.")
    }
}
