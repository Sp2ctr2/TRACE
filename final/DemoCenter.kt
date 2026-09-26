package app.saeon.trace.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saeon.trace.core.*
import app.saeon.trace.ui.design.*

@Immutable
data class DemoShortcut(
    val scenario: DemoScenario,
    val title: String,
    val caption: String,
    val result: String,
    val icon: ImageVector,
    val emphasized: Boolean = false
)

private val demoShortcuts = listOf(
    DemoShortcut(DemoScenario.NORMAL, "정상 송금", "이서연 · 32,000원 정산", "ALLOW", BankIcons.Transfer, true),
    DemoShortcut(DemoScenario.IMPERSONATION, "위험 송금", "기관 사칭 직후 · 새 계좌 · 300만 원", "HOLD", BankIcons.Shield, true),
    DemoShortcut(DemoScenario.LOAN, "대출 상환", "개인 계좌로 선상환 요청", "VERIFY", BankIcons.Bank, true),
    DemoShortcut(DemoScenario.WARN, "링크 유도", "확인하지 않은 링크 뒤의 입금", "WARN", BankIcons.Link),
    DemoShortcut(DemoScenario.NEW_ACCOUNT, "처음 보는 계좌", "직접 확인한 300만 원 신규 거래", "ALLOW", BankIcons.Profile),
    DemoShortcut(DemoScenario.FAMILY_FAKE, "가족 사칭", "새 번호 · 급한 송금 요청", "HOLD", BankIcons.Phone),
    DemoShortcut(DemoScenario.REMOTE, "원격 조작", "원격 지원 중 계좌 이동 지시", "HOLD", BankIcons.Device),
    DemoShortcut(DemoScenario.INVESTMENT, "투자금 요구", "출금 전 보증금·비밀 요구", "HOLD", BankIcons.Assets),
    DemoShortcut(DemoScenario.UNKNOWN, "확인 불가", "공식 경로 조회 실패", "UNKNOWN", BankIcons.Pause),
    DemoShortcut(DemoScenario.EDUCATION, "끝난 교육 문맥", "만료된 예방 교육 예시는 현재 요청이 아님", "ALLOW", BankIcons.Info),
    DemoShortcut(DemoScenario.UNRELATED, "관련 없는 링크", "다른 거래의 링크를 현재 정산에 연결하지 않음", "ALLOW", BankIcons.Link)
)

@Composable
fun DemoCenterScreen(model: BankViewModel, open: (String) -> Unit) {
    val stage by model.stage.collectAsStateWithLifecycle()
    val interaction by model.interaction.collectAsStateWithLifecycle()
    var expanded by rememberSaveable { mutableStateOf(false) }
    if (!stage.unlocked) {
        Page(title = "시연센터", tag = "demo_center_locked") {
            EmptyState("시연센터가 잠겨 있어요.", "앱 정보의 버전 행을 여러 번 눌러 활성화할 수 있습니다.")
        }
        return
    }

    Page(title = "시연센터", tag = "demo_center", actions = {
        IconAction(BankIcons.More, "전체 메뉴") { open("more") }
    }) {
        Space(4)
        Text("TRACE를 짧게 보여주는 시연 모드", style = MaterialTheme.typography.headlineSmall)
        Space(8)
        Body("거래 결과를 버튼이 정하지 않습니다. 각 상황을 준비한 뒤 실제 송금 확인·인증·TRACE 판단을 그대로 거칩니다.", subdued = true)
        Space(20)

        SectionTitle("빠른 시연")
        demoShortcuts.filter { it.emphasized }.forEach { item ->
            DemoShortcutRow(item, interaction.busy) {
                model.stageScenario(item.scenario) { open("transfer_state") }
            }
        }

        Space(18); Rule(); Space(14)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("다른 상황", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            QuietButton(if (expanded) "접기" else "모두 보기", Modifier.testTag("demo_more")) { expanded = !expanded }
        }
        if (expanded) {
            demoShortcuts.filterNot { it.emphasized }.forEach { item ->
                DemoShortcutRow(item, interaction.busy) {
                    model.stageScenario(item.scenario) { open("transfer_state") }
                }
            }
        } else {
            listOf(DemoScenario.WARN, DemoScenario.NEW_ACCOUNT).forEach { scenario ->
                val item = demoShortcuts.first { it.scenario == scenario }
                DemoShortcutRow(item, interaction.busy) {
                    model.stageScenario(item.scenario) { open("transfer_state") }
                }
            }
        }

        Space(18); Rule(); Space(14)
        MenuRow("같은 300만 원 비교", "새 계좌·같은 금액이어도 맥락에 따라 결과가 달라져요.", BankIcons.History,
            tag = "demo_compare") { open("comparison") }
        MenuRow("전체 시나리오 목록", "세부 설명과 모든 테스트 케이스", BankIcons.More,
            tag = "demo_all_cases") { open("demo_lab") }

        Space(18)
        Caption("시연센터는 발표용 입력 도구입니다. 실제 은행 서버·실제 TRACE 학습 가중치·실제 자금 이동은 연결하지 않습니다.")
    }
}

@Composable
private fun DemoShortcutRow(item: DemoShortcut, busy: Boolean, onClick: () -> Unit) {
    val resultTint = when (item.result) {
        "HOLD", "VERIFY" -> TraceColors.CoralText
        else -> TraceColors.Muted
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp)
            .clickable(enabled = !busy, role = Role.Button, onClick = onClick)
            .testTag("demo_${item.scenario.name}")
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(42.dp).background(TraceColors.Surface, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) { AppIcon(item.icon, size = 20, tint = if (item.emphasized) TraceColors.Coral else TraceColors.Muted) }
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Space(3)
            Caption(item.caption)
        }
        Text(item.result, style = MaterialTheme.typography.labelMedium, color = resultTint)
        AppIcon(BankIcons.Chevron, size = 15, tint = TraceColors.Muted)
    }
}
