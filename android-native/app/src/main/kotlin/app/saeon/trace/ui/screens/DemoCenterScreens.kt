package app.saeon.trace.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saeon.trace.core.*
import app.saeon.trace.data.*
import app.saeon.trace.ui.*
import app.saeon.trace.ui.design.*

private fun scenarioDescription(scenario: DemoScenario) = when (scenario) {
    DemoScenario.NORMAL -> "저장된 사람에게 저녁값 보내기"
    DemoScenario.IMPERSONATION -> "안전계좌로 돈을 옮기라는 요청"
    DemoScenario.LOAN -> "개인 계좌로 대출을 갚으라는 요청"
    DemoScenario.UNKNOWN -> "공식 상환처를 확인하지 못한 상황"
    DemoScenario.WARN -> "링크를 확인한 뒤 돈을 보내라는 요청"
    DemoScenario.EASY -> "큰 글씨로 송금 보류 경험하기"
}
private fun scenarioOutcome(scenario: DemoScenario) = when (scenario) {
    DemoScenario.NORMAL -> "추가 경고 없이 송금을 마칩니다. 잔액은 12,808,000원이 됩니다."
    DemoScenario.IMPERSONATION, DemoScenario.EASY -> "요청과 송금이 이어진 정황을 보여주고 송금을 보류합니다. 잔액은 바뀌지 않습니다."
    DemoScenario.LOAN -> "개인 계좌 대신 등록된 상환 경로를 확인합니다. 받는 곳을 바꾼 뒤 다시 인증해야 보낼 수 있습니다."
    DemoScenario.UNKNOWN -> "확인되지 않은 계좌로 보내지 않습니다. 잔액과 완료 내역은 그대로입니다."
    DemoScenario.WARN -> "받는 분과 금액을 다른 경로로 확인한 뒤, 송금 내역과 인증을 다시 진행합니다."
}

/** Preview is read-only. Loading a scenario is an explicit, separate ledger write. */
@Composable fun DemoCenterScreen(state: BankState, model: BankViewModel, home: () -> Unit, open: (String) -> Unit, back: () -> Unit) {
    val preferences by model.preferences.collectAsStateWithLifecycle()
    val interaction by model.interaction.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var reset by rememberSaveable { mutableStateOf(false) }
    val scenario = selected?.let(DemoScenario::valueOf)
    BackHandler(enabled = scenario != null) { selected = null }
    if (scenario != null) {
        Page(title = scenario.label, tag = "demo_preview", back = { selected = null }, footer = {
            PrimaryButton("이 상황으로 시작", Modifier.testTag("demo_start"), enabled = !interaction.busy) {
                model.startDemo(scenario) { selected = null; home() }
            }
            SecondaryButton("다른 상황 선택") { selected = null }
        }) {
            Space(10); TraceSignature(); Space(22)
            Text(scenarioDescription(scenario), style = MaterialTheme.typography.headlineSmall)
            Space(24); Caption("받은 요청"); Space(10)
            SurfaceBox { Body(Fixtures.message(scenario)) }
            Space(20); DetailRow("받는 분", Fixtures.recipient(scenario).name)
            DetailRow("보낼 금액", "${won(Fixtures.amount(scenario))}원", true)
            Space(14); Rule(); Space(20); Caption("직접 확인할 차이"); Space(10); Body(scenarioOutcome(scenario))
            Space(22); Caption("시작하면 가상 잔액과 시연 거래를 초기화합니다. 실제 자금은 이동하지 않습니다. 화면 모드 설정은 유지됩니다.")
        }
        return
    }
    Page(title = "시연 센터", tag = "demo_lab", back = back) {
        Column(Modifier.testTag("demo_center")) {
            Space(8); TraceSignature(); Space(20)
            Headline("같은 송금,\n다른 상황을 확인해요."); Space(12)
            Caption("상황을 선택한 뒤 은행 앱에서 직접 송금해 보세요.")
            Space(24)
            DemoScenario.entries.filter { it != DemoScenario.EASY }.forEachIndexed { index, item ->
                Row(Modifier.fillMaxWidth().heightIn(min = 76.dp).clickable(role = Role.Button) { selected = item.name }
                    .testTag("demo_${item.name}").padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text((index + 1).toString().padStart(2, '0'), style = MaterialTheme.typography.labelMedium, color = TraceColors.Muted)
                    Column(Modifier.weight(1f)) {
                        Text(item.label, style = MaterialTheme.typography.titleSmall)
                        Space(5); Caption(scenarioDescription(item))
                    }
                    AppIcon(BankIcons.Chevron, size = 17, tint = TraceColors.Muted)
                }
                Rule()
            }
            Space(20)
            MenuRow("화면 모드", "노약자·어린이에게 맞는 안내", trailing = preferences.readingMode.label, tag = "demo_reading_mode") { open("accessibility") }
            MenuRow("쉬운 모드로 보류 경험", tag = "demo_EASY") { selected = DemoScenario.EASY.name }
            Space(10); Rule(); Space(10)
            QuietButton("시연 데이터 초기화", Modifier.testTag("demo_reset")) { reset = true }
            Caption("실제 금융망에 연결하지 않는 가상 거래입니다.")
        }
    }
    if (reset) AlertDialog(onDismissRequest = { reset = false }, title = { Text("시연 데이터를 초기화할까요?") },
        text = { Text("잔액은 12,840,000원으로 돌아가고 가상 거래와 위험 신호를 지웁니다. 화면 모드 설정은 유지됩니다.") },
        confirmButton = { QuietButton("초기화", enabled = !interaction.busy) { model.startDemo(DemoScenario.NORMAL) { reset = false; home() } } },
        dismissButton = { QuietButton("취소") { reset = false } }, containerColor = TraceColors.Surface)
}

@Composable fun QuietAccessibility(preferences: BankPreferences, model: BankViewModel, back: () -> Unit) {
    Page(title = "화면과 안내", tag = "easy_mode_settings", back = back) {
        Space(10); Headline("편하게 볼 수 있는\n화면을 선택하세요."); Space(22)
        ReadingMode.entries.forEach { mode ->
            val checked = mode == preferences.readingMode
            Row(Modifier.fillMaxWidth().heightIn(min = 88.dp).clickable(role = Role.RadioButton) { model.preference { readingMode(mode) } }
                .testTag("mode_${mode.name}").semantics { selected = checked }
                .padding(vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(mode.label, style = MaterialTheme.typography.titleMedium)
                    Space(6); Caption(when (mode) { ReadingMode.STANDARD -> "계좌와 거래를 한눈에 확인해요."
                        ReadingMode.LARGE -> "큰 글씨와 버튼으로 한 단계씩 안내해요."
                        ReadingMode.CHILD -> "쉬운 말로 안내하고, 위험할 땐 믿을 수 있는 어른과 확인해요." })
                }
                RadioButton(checked, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = TraceColors.Ink))
            }
            Rule()
        }
        Space(24)
        OptionRow("홈에서 금액 숨기기", preferences.hideBalance) { model.preference { hideBalance(it) } }
        Space(22); Caption("화면을 바꿔도 송금 보류와 확인 절차는 그대로입니다. 어린이 모드는 보호자 인증이나 금융상품이 아닌, 이 시연 앱의 안내 방식입니다.")
    }
}

@Composable fun QuietMore(open: (String) -> Unit) {
    Page(title = "전체", tag = "settings") {
        MenuRow("한지우님", "새온은행 시연 계정") { open("profile") }
        Space(18); Rule(); Space(10)
        MenuRow("시연 센터", "정상 송금과 보호 상황 직접 경험하기", tag = "more_demo_center") { open("demo_center") }
        MenuRow("큰 글씨·어린이 화면") { open("accessibility") }
        Space(14); Rule(); Space(10)
        MenuRow("보안 및 인증") { open("security") }
        MenuRow("송금 설정") { open("transfer_settings") }
        MenuRow("알림") { open("notifications") }
        Space(14); Rule(); Space(10)
        MenuRow("개인정보") { open("privacy") }
        MenuRow("고객지원") { open("support") }
        MenuRow("앱 정보", tag = "app_info_open") { open("app_info") }
    }
}
