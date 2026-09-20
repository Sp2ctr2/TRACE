package app.saeon.trace.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
    DemoScenario.NORMAL -> "친구에게 저녁값 보내기"
    DemoScenario.IMPERSONATION -> "안전계좌로 돈을 옮기라는 요청"
    DemoScenario.LOAN -> "개인 계좌로 대출 상환 요구"
    DemoScenario.UNKNOWN -> "공식 상환처를 찾지 못한 상황"
    DemoScenario.WARN -> "확인하지 않은 링크 뒤의 송금"
    DemoScenario.EASY -> "큰 글씨로 송금 보류 확인"
}
private fun scenarioOutcome(scenario: DemoScenario) = when (scenario) {
    DemoScenario.NORMAL -> "추가 경고 없이 마치는 송금"
    DemoScenario.IMPERSONATION, DemoScenario.EASY -> "요청과 송금을 연결해 멈추는 이유"
    DemoScenario.LOAN -> "독립된 상환 경로와 새 거래 인증"
    DemoScenario.UNKNOWN -> "확인하지 못했을 때 보내지 않는 처리"
    DemoScenario.WARN -> "다른 경로로 확인한 뒤 다시 보는 송금"
}

@Composable private fun ModeSelector(current: ReadingMode, model: BankViewModel) {
    val large = LocalDensity.current.fontScale >= 1.4f
    val option: @Composable (ReadingMode, Modifier) -> Unit = { mode, modifier ->
        Column(modifier.heightIn(min = 52.dp).selectable(selected = current == mode, role = Role.RadioButton) {
            model.preference { readingMode(mode) }
        }.testTag("demo_mode_${mode.name}").padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(mode.label, style = MaterialTheme.typography.labelMedium,
                color = if (current == mode) TraceColors.Ink else TraceColors.Muted,
                fontWeight = if (current == mode) FontWeight.SemiBold else FontWeight.Normal)
            Space(7)
            Box(Modifier.width(24.dp).height(2.dp).background(if (current == mode) TraceColors.Ink else TraceColors.Surface))
        }
    }
    if (large) Column(Modifier.fillMaxWidth().selectableGroup()) { ReadingMode.entries.forEach { option(it, Modifier.fillMaxWidth()) } }
    else Row(Modifier.fillMaxWidth().selectableGroup()) { ReadingMode.entries.forEach { option(it, Modifier.weight(1f)) } }
}

/** Preview is read-only; only the labelled Start action changes the fictional ledger. */
@Composable fun DemoCenterScreen(state: BankState, model: BankViewModel, home: () -> Unit, open: (String) -> Unit, back: () -> Unit) {
    val preferences by model.preferences.collectAsStateWithLifecycle()
    val interaction by model.interaction.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var reset by rememberSaveable { mutableStateOf(false) }
    val scenario = selected?.let(DemoScenario::valueOf)
    BackHandler(enabled = scenario != null) { selected = null }
    if (scenario != null) {
        Page(title = scenario.label, tag = "demo_preview", back = { selected = null }, footer = {
            Caption("시작하면 가상 거래가 초기화됩니다."); Space(8)
            PrimaryButton("이 상황으로 시작", Modifier.testTag("demo_start"), enabled = !interaction.busy) {
                model.startDemo(scenario) { selected = null; home() }
            }
            SecondaryButton("다른 상황 선택") { selected = null }
        }) {
            Space(12); TraceSignature(); Space(22)
            Text(scenarioDescription(scenario), style = MaterialTheme.typography.headlineSmall)
            Space(24); Caption("송금 전에 받은 요청"); Space(10)
            SurfaceBox { Body(Fixtures.message(scenario)) }
            Space(26); Money(Fixtures.amount(scenario)); Space(8)
            Text("${Fixtures.recipient(scenario).name}님에게", style = MaterialTheme.typography.titleMedium)
            Space(4); Caption("${Fixtures.recipient(scenario).bank} · ${Fixtures.recipient(scenario).account}")
            Space(26); Rule(); Space(20)
            Caption("이번에 살펴볼 점"); Space(8); Body(scenarioOutcome(scenario))
            Space(18); Caption("화면 모드는 ${if (scenario == DemoScenario.EASY) "큰 글씨" else preferences.readingMode.label}입니다. 실제 자금은 이동하지 않습니다.")
        }
        return
    }
    Page(title = "시연 센터", tag = "demo_lab", back = back) {
        Column(Modifier.testTag("demo_center")) {
            Space(8); TraceSignature(); Space(18)
            Text("어떤 상황을\n확인해 볼까요?", style = MaterialTheme.typography.headlineSmall)
            Space(8); Caption("실제 돈이 움직이지 않는 체험입니다.")
            Space(22)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("화면 모드", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                QuietButton("안내", Modifier.testTag("demo_reading_mode")) { open("accessibility") }
            }
            ModeSelector(preferences.readingMode, model)
            Space(14); Rule(); Space(6)
            DemoScenario.entries.filter { it != DemoScenario.EASY }.forEachIndexed { index, item ->
                Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(role = Role.Button) { selected = item.name }
                    .testTag("demo_${item.name}").padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text((index + 1).toString().padStart(2, '0'), style = MaterialTheme.typography.labelSmall, color = TraceColors.Muted)
                    Column(Modifier.weight(1f)) {
                        Text(item.label, style = MaterialTheme.typography.titleSmall)
                        Space(4); Caption(scenarioDescription(item))
                    }
                    AppIcon(BankIcons.Chevron, size = 16, tint = TraceColors.Muted)
                }
            }
            Space(8); Rule(); Space(8)
            MenuRow("쉬운 모드로 보류 경험", tag = "demo_EASY") { selected = DemoScenario.EASY.name }
            QuietButton("시연 데이터 초기화", Modifier.testTag("demo_reset")) { reset = true }
        }
    }
    if (reset) AlertDialog(onDismissRequest = { reset = false }, title = { Text("처음 상태로 돌아갈까요?") },
        text = { Text("가상 거래와 위험 신호를 지우고 잔액을 12,840,000원으로 되돌립니다. 화면 모드는 유지합니다.") },
        confirmButton = { QuietButton("초기화", enabled = !interaction.busy) { model.startDemo(DemoScenario.NORMAL) { reset = false; home() } } },
        dismissButton = { QuietButton("취소") { reset = false } }, containerColor = TraceColors.Surface)
}

@Composable fun QuietAccessibility(preferences: BankPreferences, model: BankViewModel, back: () -> Unit) {
    Page(title = "화면과 안내", tag = "easy_mode_settings", back = back) {
        Space(12); Text("나에게 편한 화면", style = MaterialTheme.typography.headlineSmall)
        Space(8); Caption("글자 크기뿐 아니라 안내 방식도 달라집니다."); Space(24)
        ReadingMode.entries.forEach { mode ->
            val checked = mode == preferences.readingMode
            Row(Modifier.fillMaxWidth().heightIn(min = 86.dp).selectable(selected = checked, role = Role.RadioButton) {
                model.preference { readingMode(mode) }
            }.testTag("mode_${mode.name}").padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(mode.label, style = MaterialTheme.typography.titleMedium)
                    Space(6); Caption(when (mode) {
                        ReadingMode.STANDARD -> "계좌와 거래를 한눈에 봅니다."
                        ReadingMode.LARGE -> "큰 글씨로, 한 번에 한 가지씩 확인합니다."
                        ReadingMode.CHILD -> "쉬운 말로 설명하고, 위험할 땐 어른과 확인합니다."
                    })
                }
                RadioButton(checked, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = TraceColors.Ink))
            }
            Rule()
        }
        Space(24); OptionRow("홈에서 금액 숨기기", preferences.hideBalance) { model.preference { hideBalance(it) } }
        Space(20); Caption("화면 모드가 달라져도 송금 보류는 풀리지 않습니다. 어린이 모드는 안내 방식이며, 보호자의 인증이나 허락을 대신하지 않습니다.")
    }
}

@Composable fun QuietMore(open: (String) -> Unit) {
    Page(title = "전체", tag = "settings") {
        Space(8); MenuRow("한지우님", "새온은행 시연 계정") { open("profile") }
        Space(18); Rule(); Space(20); Caption("체험과 화면")
        MenuRow("시연 센터", tag = "more_demo_center") { open("demo_center") }
        MenuRow("큰 글씨·어린이 화면") { open("accessibility") }
        Space(18); Rule(); Space(20); Caption("이용 설정")
        MenuRow("보안 및 인증") { open("security") }
        MenuRow("송금 설정") { open("transfer_settings") }
        MenuRow("알림") { open("notifications") }
        Space(18); Rule(); Space(20); Caption("도움과 정보")
        MenuRow("개인정보") { open("privacy") }
        MenuRow("고객지원") { open("support") }
        MenuRow("앱 정보", tag = "app_info_open") { open("app_info") }
    }
}
