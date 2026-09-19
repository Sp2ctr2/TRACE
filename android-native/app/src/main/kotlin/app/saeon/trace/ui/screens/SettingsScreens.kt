package app.saeon.trace.ui.screens

import android.os.Build
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saeon.trace.BuildConfig
import app.saeon.trace.core.*
import app.saeon.trace.data.BankPreferences
import app.saeon.trace.ui.BankViewModel
import app.saeon.trace.ui.design.*

@Composable fun MoreScreen(open: (String) -> Unit) {
    Page(title = "전체", tag = "settings") {
        Space(12)
        MenuRow("한지우님", "새온은행 시연 계정", BankIcons.Profile) { open("profile") }
        Space(20); Rule(); Space(16)
        SectionTitle("보안과 설정")
        MenuRow("보안 및 인증", "거래 인증·등록 기기·보안 알림", BankIcons.Lock) { open("security") }
        MenuRow("송금 설정", "기본 계좌·한도·자주 쓰는 계좌", BankIcons.Transfer) { open("transfer_settings") }
        MenuRow("알림", "송금 완료·안전 확인·예정 내역", BankIcons.Bell) { open("notifications") }
        Space(18); Rule(); Space(16)
        MenuRow("개인정보", "원문을 가져가지 않는 데이터 경계", BankIcons.Shield) { open("privacy") }
        MenuRow("접근성", "쉬운 모드·잔액 표시", BankIcons.Settings) { open("accessibility") }
        Space(18); Rule(); Space(16)
        MenuRow("고객지원", "새온은행 시연 고객센터", BankIcons.Phone) { open("support") }
        MenuRow("TRACE 도움말", "보류·확인·공식 경로 안내", BankIcons.Info) { open("help") }
        MenuRow("앱 정보", "버전·시연 환경", BankIcons.More, tag = "app_info_open") { open("app_info") }
        Space(20); Caption("새온은행 × TRACE")
    }
}

@Composable fun ProfileScreen(back: () -> Unit) {
    Page(title = "내 정보", tag = "profile", back = back) {
        Space(16); Text("한지우님", style = MaterialTheme.typography.headlineLarge); Space(12); Caption("새온은행 시연 계정")
        Space(32); Rule(); Space(14)
        DetailRow("계정 종류", "기기 내 가상 계정")
        DetailRow("주 거래 계좌", "새온 생활통장")
        DetailRow("가입일", "2026.03.20")
        DetailRow("개인정보 등록", "실명·연락처 등록 없음")
        Space(26); Body("로그인이나 실제 본인확인을 하지 않는 시연 계정입니다. 이 계정으로 실제 금융 거래를 할 수는 없어요.", subdued = true)
    }
}

@Composable fun SecurityScreen(preferences: BankPreferences, model: BankViewModel, open: (String) -> Unit, back: () -> Unit) {
    Page(title = "보안 및 인증", tag = "security_settings", back = back) {
        SectionTitle("로그인 및 거래 인증")
        Caption("시연 계정은 로그인 없이 열립니다. 송금할 때는 매번 별도의 확인을 거쳐요.")
        Space(12)
        OptionRow("생체 인증 우선 사용", preferences.biometric, "등록된 강한 생체 인증을 사용합니다. 사용할 수 없으면 시연 확인을 직접 선택할 수 있어요.") {
            model.preference { biometric(it) }
        }
        Rule()
        MenuRow("거래 인증 안내", "기기 인증이 거래의 안전을 보장하지는 않아요.", BankIcons.Lock) { open("help") }
        Space(22); Rule(); Space(20); SectionTitle("등록된 기기")
        DetailRow("현재 기기", "${Build.MANUFACTURER} ${Build.MODEL}")
        DetailRow("Android", Build.VERSION.RELEASE)
        DetailRow("서명 키", "Android Keystore · EC P-256")
        Caption("서명용 개인키는 기기 밖으로 내보내지 않습니다. 실제 은행의 기기 등록 인증서는 아닙니다.")
        Space(26); Rule(); Space(18)
        OptionRow("앱 내 보안 알림", preferences.notifications, "송금 결과와 TRACE 확인 내역을 알림 화면에 표시해요.") {
            model.preference { notifications(it) }
        }
        Space(18); Rule(); Space(14)
        MenuRow("TRACE 안전 확인", "보류된 거래는 화면 밖에서도 실행하지 못하게 막습니다.", BankIcons.Shield) { open("safety") }
    }
}

@Composable fun TransferSettingsScreen(state: BankState, model: BankViewModel, open: (String) -> Unit, back: () -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var digits by rememberSaveable { mutableStateOf(state.transferLimit.toString()) }
    Page(title = "송금 설정", tag = "transfer_settings", back = back) {
        SectionTitle("출금 계좌")
        MenuRow("새온 생활통장", "기본 출금 계좌 · 110-***-0001", BankIcons.Bank) { open("account") }
        Space(20); Rule(); Space(18)
        MenuRow("하루 송금 한도", "${won(state.transferLimit)}원", BankIcons.Transfer) { digits = state.transferLimit.toString(); editing = true }
        Caption("하루 동안 완료한 송금의 합계에 적용합니다. 인증과 위험 판단을 생략하는 한도는 아니에요.")
        Space(24); Rule(); Space(18)
        MenuRow("자주 쓰는 계좌", "빠른 선택 목록 관리", BankIcons.Profile) { open("favorites") }
        MenuRow("자동이체 예정 내역", "예정 표시와 시연 내역 확인", BankIcons.Calendar) { open("recurring") }
    }
    if (editing) AlertDialog(onDismissRequest = { editing = false }, title = { Text("하루 송금 한도") },
        text = { Column { Field(digits, "원", { digits = it.filter(Char::isDigit).take(9) }, keyboard = KeyboardType.Number)
            Space(12); Caption("1만 원부터 1억 원까지 설정할 수 있어요.") } },
        confirmButton = { QuietButton("저장") { model.act { model.repository.setLimit(digits.toLongOrNull() ?: 0); editing = false } } },
        dismissButton = { QuietButton("취소") { editing = false } }, containerColor = TraceColors.Surface)
}

@Composable fun FavoritesScreen(state: BankState, model: BankViewModel, back: () -> Unit) {
    val preferences by model.preferences.collectAsStateWithLifecycle()
    Page(title = "자주 쓰는 계좌", tag = "favorite_accounts", back = back) {
        Body("목록에 추가해도 안전한 수취인으로 인증되는 것은 아니에요.", subdued = true)
        Space(22)
        state.recipients.forEach { recipient ->
            OptionRow(recipient.name, recipient.id in preferences.favoriteIds, "${recipient.bank} · ${recipient.account}") { enabled ->
                model.preference { favorite(recipient.id, enabled) }
            }
            Rule()
        }
    }
}

@Composable fun RecurringScreen(state: BankState, model: BankViewModel, back: () -> Unit) {
    Page(title = "자동이체 예정 내역", tag = "recurring", back = back) {
        Space(14); Caption("09월 25일 예정"); Space(8); Headline("통신비"); Space(18); Money(68_000)
        Space(28); Rule(); Space(12)
        DetailRow("출금 계좌", "새온 생활통장")
        DetailRow("받는 곳", "새온모바일 · 가상 통신사")
        DetailRow("주기", "매월 25일")
        Space(20); Rule(); Space(16)
        OptionRow("홈에 예정 내역 표시", state.recurringEnabled, "홈과 알림 화면에서 이 예정 내역을 볼 수 있어요.") {
            model.act { model.repository.recurring(it) }
        }
        Space(20); Caption("시연 예정 내역만 관리합니다. 예약 시각에 자동으로 송금을 실행하거나 실제 요금을 납부하지 않습니다.")
    }
}

@Composable fun NotificationsScreen(state: BankState, preferences: BankPreferences, model: BankViewModel, open: (String) -> Unit, back: () -> Unit) {
    Page(title = "알림", tag = "notifications", back = back, actions = { IconAction(BankIcons.Settings, "알림 설정") { open("security") } }) {
        if (!preferences.notifications) {
            EmptyState("앱 내 알림을 꺼두었어요.", "보안 및 인증 설정에서 다시 켤 수 있습니다. 거래 내역이나 보류 상태는 그대로 유지됩니다.")
            PrimaryButton("알림 설정 열기") { open("security") }
        } else {
            state.pending.asReversed().forEach { record ->
                MenuRow("TRACE 안전 확인", "${record.intent.recipient.name} · ${won(record.intent.amount)}원\n${stageLabel(record.stage)}", BankIcons.Shield) {
                    model.resume(record.intent.id) { open("transfer_state") }
                }; Rule()
            }
            state.receipts.filterNot { it.seed }.take(10).forEach { receipt ->
                MenuRow(if (receipt.direction == Direction.DEBIT) "송금을 마쳤어요." else "내 계좌에서 가져왔어요.",
                    "${receipt.recipient.name} · ${won(receipt.amount)}원\n${dateLabel(receipt.completedAt)}", BankIcons.History) { open("receipt/${receipt.id}") }
                Rule()
            }
            state.records.filter { it.route != null }.distinctBy { it.route!!.id }.asReversed().take(3).forEach { record ->
                MenuRow("공식 경로 확인 완료", "새온은행 대출상환센터 · ${record.route!!.productName}", BankIcons.Bank) {
                    val next = state.records.find { it.intent.originIntentId == record.intent.id } ?: record
                    model.resume(next.intent.id) { open("transfer_state") }
                }; Rule()
            }
            if (state.recurringEnabled) {
                MenuRow("자동이체가 예정되어 있어요.", "09월 25일 · 통신비 68,000원", BankIcons.Calendar) { open("recurring") }
                Rule()
            }
            MenuRow("TRACE 안전 확인", "최근 위험 맥락과 보류 내역을 확인하세요.", BankIcons.Shield) { open("safety") }
        }
        Space(24); Caption("이 기기의 가상 거래와 시연 예정 내역에서 만든 알림입니다. 푸시 메시지나 외부 알림을 수집하지 않습니다.")
    }
}

@Composable fun AccessibilityScreen(preferences: BankPreferences, model: BankViewModel, back: () -> Unit) {
    Page(title = "접근성", tag = "easy_mode_settings", back = back) {
        Headline("편한 크기로,\n분명한 안내로."); Space(22)
        OptionRow("쉬운 모드", preferences.easyMode, "글자를 키우고, 위험 상황에서는 한 번에 확인할 내용을 줄입니다.") { model.preference { easy(it) } }
        Rule()
        OptionRow("홈에서 잔액 숨기기", preferences.hideBalance, "주변에서 화면을 볼 때 금액을 감출 수 있어요.") { model.preference { hideBalance(it) } }
        Space(24); Rule(); Space(20); SectionTitle("기본 접근성")
        Body("기기에서 설정한 글자 크기와 화면 읽기 기능을 따릅니다. 상태는 색뿐 아니라 문장과 버튼 이름으로 전달해요.", subdued = true)
        Space(22); Caption("쉬운 모드를 켜도, 확인되지 않은 송금을 진행하는 버튼은 생기지 않습니다.")
        Space(24); SurfaceBox {
            Text("아직 돈은 나가지 않았습니다.", style = MaterialTheme.typography.titleMedium)
            Space(12); Body("공식 경로로 확인한 뒤 다음 행동을 선택하세요.")
        }
    }
}

@Composable fun SupportScreen(open: (String) -> Unit, back: () -> Unit) {
    Page(title = "고객지원", tag = "support", back = back) {
        Space(12); Headline("새온은행\n시연 고객센터"); Space(16)
        Body("실제 은행이나 경찰로 연결되는 화면이 아닙니다. 현재 거래 상태와 확인할 방법을 안내해요.", subdued = true)
        Space(28); Rule(); Space(14)
        MenuRow("현재 보류 상태 확인", "돈이 나갔는지, 왜 멈췄는지 확인해요.", BankIcons.Pause) { open("pending") }
        MenuRow("대출 공식 상환 메뉴", "등록된 가상 대출과 상환 경로", BankIcons.Bank) { open("loan") }
        MenuRow("사고 상황에서 확인할 일", "상대가 준 연락처를 쓰지 않는 확인 방법", BankIcons.Shield) { open("safety_guide") }
        MenuRow("자주 묻는 질문", "보류·인증·원문 처리 안내", BankIcons.Info) { open("help") }
        Space(26); Rule(); Space(20)
        Body("실제 피해가 의심되면 거래한 은행의 공식 앱·카드·공식 홈페이지에서 연락처를 직접 확인하세요.")
        Space(16); Caption("가상의 전화번호를 실제 상담 번호처럼 표시하지 않습니다.")
    }
}

@Composable fun HelpScreen(back: () -> Unit) {
    val questions = listOf(
        "인증했는데 왜 송금이 멈췄나요?" to "인증은 내가 맞는지 확인합니다. TRACE는 그 거래가 만들어진 과정을 별도로 확인해요. 내가 직접 보낸 돈이라도 사기 요청에 의한 거래일 수 있습니다.",
        "보류 상태에서 앱을 닫아도 되나요?" to "네. 보류 상태는 기기에 저장됩니다. 앱을 다시 열어도 자동으로 송금되지 않아요. 홈이나 안전 센터에서 다시 확인할 수 있습니다.",
        "WARN과 HOLD는 무엇이 다른가요?" to "확인이 필요한 WARN은 다른 경로에서 내용을 확인한 뒤 다시 검토할 수 있습니다. HOLD는 확인 표시만으로 송금할 수 없어요. 공식 경로로 확인하거나 송금을 취소해야 합니다.",
        "상환 경로가 확인되면 바로 보내나요?" to "아니요. 수취인이 바뀌면 새로운 거래입니다. 새 내역을 확인하고 다시 인증한 뒤, 정책을 다시 평가합니다.",
        "공식 경로 조회가 안 되면요?" to "일반 송금으로 바꾸지 않습니다. 잔액과 완료 내역은 그대로 유지하고, 확인할 수 없다는 사실을 안내해요.",
        "공유한 메시지가 저장되나요?" to "입력한 원문을 데이터베이스나 로그에 저장하지 않습니다. 확인 뒤 종류·시간·출처 같은 구조화된 신호만 남겨요. 신호는 15분 동안 현재 거래의 맥락에 사용합니다.",
        "통화를 듣거나 문자를 가져오나요?" to "아니요. SMS와 통화 기록을 읽지 않습니다. 음성 입력은 사용자가 버튼을 누르고 권한을 허용했을 때만 기기 내 음성 인식으로 실행합니다.",
        "모든 사기를 탐지하나요?" to "그렇지 않습니다. 이 구현의 로컬 규칙은 가능한 위험 표현을 찾는 참고 신호입니다. 실제 사기 여부를 확정하거나 모든 우회 표현을 탐지하지 못합니다.",
        "실제로 돈을 보낼 수 있나요?" to "아니요. 새온은행과 수취인, 계좌, 고객센터는 모두 가상입니다. 모든 잔액과 영수증은 시연 데이터이며 외부 은행에 연결하지 않습니다."
    )
    var opened by rememberSaveable { mutableIntStateOf(-1) }
    Page(title = "TRACE 도움말", tag = "help", back = back) {
        questions.forEachIndexed { index, (question, answer) ->
            Column(Modifier.fillMaxWidth()) {
                MenuRow(question) { opened = if (opened == index) -1 else index }
                if (opened == index) { Body(answer, Modifier.padding(bottom = 20.dp), subdued = true) }
                Rule()
            }
        }
    }
}

@Composable fun AppInfoScreen(open: (String) -> Unit, back: () -> Unit) {
    var taps by rememberSaveable { mutableIntStateOf(0) }
    Page(title = "앱 정보", tag = "app_info", back = back) {
        Space(20); Text("새온은행", style = MaterialTheme.typography.headlineLarge); Space(12); TraceSignature("새온은행 × TRACE")
        Space(30); Rule(); Space(12)
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button) {
            taps += 1
            if (taps >= 5) { taps = 0; open("demo_lab") }
        }.testTag("app_version").padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("버전", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyMedium)
        }
        DetailRow("실행 환경", "오프라인 시연")
        DetailRow("화면", "Kotlin · Jetpack Compose")
        DetailRow("은행 연결", "외부 금융망 연결 없음")
        DetailRow("지원", "Android 8.0 이상")
        Space(24); Rule(); Space(20)
        Body("평소에는 조용하게,\n위험할 땐 분명하게.")
        Space(20); Caption("TRACE 웹사이트의 시각 언어와 보호 흐름을 계승한 네이티브 reference implementation입니다.")
        Space(20); SimulationNote()
    }
}

@Composable fun DemoLabScreen(state: BankState, model: BankViewModel, home: () -> Unit, back: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var resetAll by rememberSaveable { mutableStateOf(false) }
    Page(title = "TRACE Demo Lab", tag = "demo_lab", back = back) {
        Body("발표용 가상 상태를 선택합니다.", subdued = true)
        Space(8); Caption("선택하면 이 기기의 가상 잔액·거래·위험 신호를 초기 상태로 바꿉니다.")
        Space(26)
        DemoScenario.entries.forEachIndexed { index, scenario ->
            Row(Modifier.fillMaxWidth().heightIn(min = 84.dp).clickable(role = Role.Button) { selected = scenario.name }
                .testTag("demo_${scenario.name}").padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text((index + 1).toString().padStart(2, '0'), style = MaterialTheme.typography.labelMedium, color = TraceColors.Muted)
                Column(Modifier.weight(1f)) {
                    Text(scenario.label, style = MaterialTheme.typography.titleSmall); Space(5); Caption(scenario.expected)
                }
                AppIcon(BankIcons.Chevron, size = 18, tint = TraceColors.Muted)
            }
            Rule()
        }
        Space(24)
        PrimaryButton("모든 시연 데이터 초기화", Modifier.testTag("demo_reset")) { resetAll = true }
        Space(14)
        QuietButton("15분 경과 재현") { model.act { model.repository.expireSignalsForDemo() } }
        Caption("현재 시나리오: ${state.scenario.label}\n시간이 지나도 이미 보류된 거래를 자동 해제하지 않습니다.")
    }
    if (selected != null || resetAll) {
        val scenario = selected?.let(DemoScenario::valueOf) ?: DemoScenario.NORMAL
        AlertDialog(onDismissRequest = { selected = null; resetAll = false },
            title = { Text(if (resetAll) "시연 데이터를 초기화할까요?" else "${scenario.label} 시나리오를 시작할까요?") },
            text = { Text("가상 잔액은 12,840,000원으로 돌아갑니다. 기기에 저장된 시연 거래와 위험 신호, 설정을 지웁니다. 실제 금융 데이터는 없습니다.") },
            confirmButton = { QuietButton("시작", Modifier.testTag("demo_start")) {
                model.reset(scenario) { selected = null; resetAll = false; home() }
            } }, dismissButton = { QuietButton("취소") { selected = null; resetAll = false } }, containerColor = TraceColors.Surface)
    }
}
