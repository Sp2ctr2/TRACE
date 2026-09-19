package dev.sp2ctr2.saeon.ui

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sp2ctr2.saeon.BankViewModel
import dev.sp2ctr2.saeon.BuildConfig
import dev.sp2ctr2.saeon.data.AppOptions
import dev.sp2ctr2.saeon.domain.*

@OptIn(ExperimentalFoundationApi::class)
@Composable fun MoreScreen(go: (String)->Unit) {
    var versionTaps by remember { mutableIntStateOf(0) }
    Screen("전체",tag="more_screen") {
        ListRow("새온 고객님","시연 프로필",icon="profile",tag="profile_row",onClick={go("settings/profile")})
        Gap(12);Rule();Gap(14)
        SectionHeading("계좌와 송금")
        ListRow("송금 설정",icon="transfer",onClick={go("settings/transfer")})
        ListRow("자주 쓰는 계좌",icon="bank",onClick={go("settings/favorites")})
        ListRow("예약 송금 · 자동이체",icon="clock",onClick={go("schedules")})
        Gap(16);Rule();Gap(14)
        SectionHeading("보안과 사용 환경")
        ListRow("보안 및 인증",icon="lock",tag="security_settings",onClick={go("settings/security")})
        ListRow("화면 · 접근성",icon="eye",tag="accessibility_settings",onClick={go("settings/accessibility")})
        ListRow("알림 내역",icon="bell",onClick={go("notifications")})
        ListRow("개인정보 경계",icon="shield",onClick={go("privacy")})
        Gap(16);Rule();Gap(14)
        ListRow("고객지원",icon="phone",onClick={go("support")})
        ListRow("앱 정보",icon="info",onClick={go("about")})
        Gap(28)
        Row(Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("demo_entry").combinedClickable(
            onClick={versionTaps++;if(versionTaps>=5){versionTaps=0;go("demo")}},
            onLongClick={go("demo")},onLongClickLabel="TRACE 시연 제어 열기"
        ),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            TraceMark(Modifier.size(20.dp));Copy("TRACE  ·  ${BuildConfig.VERSION_NAME}",size=12)
        }
    }
}

@Composable fun OptionRow(title: String, detail: String, checked: Boolean, tag: String, change: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical=16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f)){Copy(title,size=16,color=TraceColors.Ink,weight=FontWeight.Medium);Gap(5);Copy(detail,size=13)}
        Switch(checked,change,modifier=Modifier.testTag(tag),colors=SwitchDefaults.colors(checkedTrackColor=TraceColors.Action,checkedThumbColor=TraceColors.White,uncheckedTrackColor=TraceColors.Paper,uncheckedBorderColor=TraceColors.Line))
    }
}

@Composable fun SettingsScreen(section: String,state: BankState,options: AppOptions,vm: BankViewModel,go:(String)->Unit,back:()->Unit) {
    val title=when(section){"security"->"보안 및 인증";"accessibility"->"화면 · 접근성";"transfer"->"송금 설정";"favorites"->"자주 쓰는 계좌";"profile"->"내 프로필";"device"->"이 기기";else->"설정"}
    var limit by rememberSaveable(state.dailyLimit){mutableStateOf(state.dailyLimit.toString())}
    var saved by remember { mutableStateOf(false) }
    Screen(title,tag="settings_$section",onBack=back) {
        when(section) {
            "security" -> {
                Title("나를 확인하고,\n거래를 다시 확인해요",27);Gap(14);Copy("본인 확인에 성공해도 송금의 위험 맥락은 별도로 평가합니다.")
                Gap(24)
                OptionRow("기기 인증 우선 사용","지원되는 기기에서 생체 인증 또는 기기 암호로 확인해요",options.biometric,"option_biometric"){vm.setOption("biometric",it)}
                Rule();ListRow("등록된 기기","${Build.MANUFACTURER} ${Build.MODEL}",icon="profile",onClick={go("settings/device")})
                ListRow("TRACE 데이터 경계","원문 없이 필요한 신호만",icon="lock",onClick={go("privacy")})
                Gap(24);QuietNotice("이번 앱은 가상 은행입니다. 시연 확인은 실제 은행 인증을 대신하지 않으며, 운영용 인증 체계를 주장하지 않습니다.")
            }
            "accessibility" -> {
                Title("편한 방식으로\n사용하세요",28);Gap(18)
                OptionRow("쉬운 모드","큰 글씨와 간단한 보호 안내를 사용해요",options.easy,"option_easy"){vm.setOption("easy",it)};Rule()
                OptionRow("화면 전환 줄이기","화면 전환 효과를 없애요",options.reducedMotion,"option_motion"){vm.setOption("motion",it)};Rule()
                OptionRow("진동 피드백","중요한 송금 상태에서만 짧게 알려요",options.haptics,"option_haptics"){vm.setOption("haptics",it)};Rule()
                OptionRow("홈 잔액 숨기기","홈 화면의 잔액 숫자를 숨겨요",options.hideBalance,"option_hide_balance"){vm.setOption("hideBalance",it)}
                Gap(24);Copy("시스템 글자 크기와 TalkBack을 함께 사용할 수 있어요. 이번 시연 앱은 밝은 테마로 제공됩니다.",size=13)
            }
            "transfer" -> {
                Title("송금 기준을\n설정하세요",28);Gap(20);DetailRow("기본 출금 계좌","새온 생활통장");Gap(16)
                LabeledField(limit,{limit=it.filter(Char::isDigit).take(8);saved=false},"1회 송금 한도","limit_input")
                Gap(8);Copy("1만원부터 1,000만원까지 설정할 수 있어요.",size=13)
                Gap(24);Primary(if(saved) "저장됨" else "한도 저장","limit_save",(limit.toLongOrNull() ?: 0L) in 10_000L..10_000_000L){vm.setLimit(limit.toLong());saved=true}
                Gap(22);Rule();ListRow("예약 송금 관리",icon="clock",onClick={go("schedules")})
            }
            "favorites" -> {
                Title("자주 보내는 사람",27);Gap(14);Copy("등록한 수취인을 송금 화면에서 먼저 찾을 수 있어요.")
                Gap(20)
                Fixtures.recipients.forEach { r -> OptionRow(r.name,r.displayAccount,r.id in state.favorites,"favorite_${r.id}"){vm.favorite(r.id)};Rule() }
            }
            "profile" -> {
                Title("새온 고객님",28);Gap(14);Copy("이 프로필은 대회용 가상 고객입니다.")
                Gap(28);DetailRow("고객 구분","개인 · 가상 고객");DetailRow("연결 계좌","새온 생활통장");DetailRow("실명 확인","실제 실명 인증 없음")
                Gap(20);QuietNotice("개인 이름, 전화번호, 신분증을 수집하지 않습니다.","lock")
            }
            "device" -> {
                Title("현재 사용하는 기기",27);Gap(24);DetailRow("기기",Build.MODEL);DetailRow("제조사",Build.MANUFACTURER);DetailRow("Android",Build.VERSION.RELEASE)
                DetailRow("거래 서명","Android Keystore · P-256");DetailRow("네트워크 권한","요청하지 않음")
                Gap(22);QuietNotice("기기 식별번호나 연락처를 수집하지 않습니다. Keystore 서명은 이 앱의 거래 연결을 검증하는 시연이며, 은행 서버의 신뢰 검증을 대신하지 않습니다.")
            }
        }
    }
}

@Composable fun SchedulesScreen(state: BankState,vm: BankViewModel,go:(String)->Unit,back:()->Unit) {
    var selected by remember { mutableStateOf<ScheduledTransfer?>(null) }
    Screen("예약 송금 · 자동이체",tag="schedules_screen",onBack=back,actions={GlyphButton("plus","예약 추가","schedule_add"){go("schedule-new")}}) {
        Title("다가오는 송금을\n미리 정리해요",27);Gap(14)
        Copy("예약 정보를 이 기기에 저장합니다. 시연에서는 실행 전에 거래를 직접 다시 확인합니다.",size=14);Gap(24)
        if(state.schedules.isEmpty()) QuietNotice("등록된 예약이 없어요. 오른쪽 위에서 새 예약을 추가하세요.","clock")
        state.schedules.forEach { p ->
            ListRow(p.recipient.name,"매월 ${p.day}일 · "+if(p.active) "예약 사용 중" else "일시 중지",money(p.amount)+"원",tag="schedule_${p.id}",onClick={selected=p});Rule()
        }
    }
    selected?.let { p -> TraceSheet("예약 상세",{selected=null}) {
        Title(p.recipient.name,24);Gap(12);Amount(p.amount,size=32);Gap(16)
        DetailRow("받는 계좌",p.recipient.displayAccount);DetailRow("예약일","매월 ${p.day}일")
        Gap(20);Primary(if(p.active) "예약 일시 중지" else "예약 다시 사용","schedule_toggle"){selected=null;vm.scheduleToggle(p.id)}
        Secondary("예약 삭제","schedule_delete"){selected=null;vm.scheduleDelete(p.id)}
        Copy("예약을 삭제해도 이미 완료된 거래 내역은 바뀌지 않습니다.",size=12)
    } }
}

@Composable fun ScheduleEditor(state: BankState,vm: BankViewModel,back:()->Unit) {
    var recipient by rememberSaveable { mutableStateOf(Fixtures.friend.id) }
    var amount by rememberSaveable { mutableStateOf("32000") }
    var day by rememberSaveable { mutableStateOf("25") }
    val sum=amount.toLongOrNull() ?: 0L
    val date=day.toIntOrNull() ?: 0
    val r=Fixtures.recipients.first{it.id==recipient}
    Screen("새 예약",tag="schedule_editor",onBack=back,footer={Primary("예약 저장","schedule_save",sum>0 && sum<=state.dailyLimit && date in 1..28){vm.scheduleAdd(r,sum,date)}}) {
        Title("언제, 얼마를\n보낼까요?",28);Gap(24)
        Copy("받는 분",size=13)
        Fixtures.recipients.take(2).forEach { person -> ListRow(person.name,person.displayAccount,value=if(person.id==recipient) "선택됨" else null,onClick={recipient=person.id}) }
        Gap(16);LabeledField(amount,{amount=it.filter(Char::isDigit).take(8)},"예약 금액","schedule_amount")
        Gap(18);LabeledField(day,{day=it.filter(Char::isDigit).take(2)},"매월 며칠 · 1~28일","schedule_day")
        Gap(24);QuietNotice("예약은 송금 승인이 아닙니다. 실제 실행할 때 수취인과 맥락을 다시 확인해야 합니다.","shield")
    }
}

@Composable fun ManualAccountScreen(vm: BankViewModel,back:()->Unit) {
    var bank by rememberSaveable { mutableStateOf("노을은행") }
    var digits by rememberSaveable { mutableStateOf("") }
    var found by remember { mutableStateOf<Recipient?>(null) }
    var attempted by remember { mutableStateOf(false) }
    Screen("계좌로 보내기",tag="manual_account_screen",onBack=back,footer={
        val r=found
        if(r!=null) Primary("이 계좌로 보내기","manual_recipient_confirm"){vm.start(r)}
        else Primary("가상 예금주 확인","manual_lookup",digits.length==4){attempted=true;found=Fixtures.recipients.firstOrNull{it.bank==bank && it.account.endsWith(digits)}}
    }) {
        Title("받는 계좌를\n입력해 주세요",28);Gap(18)
        Copy("실제 계좌는 조회하지 않습니다. 등록된 시연 계좌의 뒷자리 4개로 찾을 수 있어요.",size=14)
        Gap(22)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {listOf("노을은행","새온은행").forEach { value -> FilterChip(selected=bank==value,onClick={bank=value;found=null;attempted=false},label={Text(value)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=TraceColors.Soft,selectedLabelColor=TraceColors.Action))}}
        Gap(18);LabeledField(digits,{digits=it.filter(Char::isDigit).take(4);found=null;attempted=false},"시연 계좌 뒷자리","manual_account_input",error=attempted && found==null)
        Gap(12);Copy("노을은행 7421 · 9802 · 5934\n새온은행 4512",size=13)
        if(attempted && found==null){Gap(14);Copy("등록된 시연 계좌를 찾지 못했어요.",color=TraceColors.Action)}
        found?.let { r -> Gap(24);Rule();ListRow(r.name,r.displayAccount,icon="profile");QuietNotice("예금주 확인은 거래의 안전을 보증하지 않습니다.","shield") }
    }
}

@Composable fun SupportScreen(go:(String)->Unit,back:()->Unit) {
    var question by remember { mutableStateOf<String?>(null) }
    Screen("고객지원",tag="support_screen",onBack=back) {
        Eyebrow("새온은행 시연 고객센터");Gap(16);Title("확인이 필요할 때,\n혼자 결정하지 마세요",27);Gap(18)
        Copy("직접 연 은행 앱 안에서 현재 상황과 받는 곳을 다시 확인하세요.")
        Gap(24);Primary("보류된 송금 상담 시연","support_case"){go("support-case")}
        Gap(26);SectionHeading("자주 묻는 질문")
        listOf("TRACE가 통화를 듣나요?","돈이 이미 나갔나요?","공식 상환 경로는 어떻게 확인하나요?","위험 신호가 없으면 안전한가요?").forEach { q -> ListRow(q,onClick={question=q});Rule() }
        Gap(22);QuietNotice("이 메뉴는 실제 상담원, 금융기관 또는 경찰에 연결하지 않습니다. 실제 사고라면 이용 중인 은행의 공식 고객지원 경로를 별도로 이용하세요.","phone")
    }
    question?.let { q -> TraceSheet(q,{question=null}) {
        Copy(when(q){"TRACE가 통화를 듣나요?"->"아니요. 사용자가 직접 공유하거나 입력하고 확인을 누른 텍스트만 기기 안에서 분석합니다. 마이크와 문자 권한을 요청하지 않습니다.";"돈이 이미 나갔나요?"->"이 앱에서는 실제 돈이 이동하지 않습니다. 가상 거래도 보류·추가 확인 중에는 잔액을 차감하지 않습니다. 완료된 거래만 거래 내역에 표시합니다.";"공식 상환 경로는 어떻게 확인하나요?"->"상대가 보낸 링크나 전화번호가 아니라 직접 연 은행 앱의 상환 메뉴에서 확인하세요. TRACE 시연에서는 등록 대출 응답을 확인한 뒤 새 수취인으로 거래를 다시 검토합니다.";else->"아니요. 규칙 기반 분석이 놓치는 표현이 있을 수 있습니다. 위험 신호의 부재는 안전을 보증하지 않습니다."});Gap(22);Primary("확인",neutral=true){question=null}
    } }
}

@Composable fun SupportCaseScreen(state: BankState,back:()->Unit) {
    var stage by rememberSaveable { mutableIntStateOf(0) }
    Screen("보류된 송금 상담",tag="support_case_screen",onBack=back,footer={Primary(when(stage){0->"상황 정리하기";1->"확인할 내용 정리";else->"확인"},"support_case_next"){if(stage<2)stage++ else back()}}) {
        Eyebrow("실제 상담 연결 없는 시연",true);Gap(18)
        when(stage) {
            0 -> {Title("먼저 현재 상태를\n정리할게요",28);Gap(16);Copy("거래가 완료됐는지와, 어떤 요청이 있었는지를 분리해서 확인합니다.");Gap(24);state.draft?.let{TransactionSummary(it)} ?: QuietNotice("진행 중인 송금이 없습니다.")}
            1 -> {Title("이 내용을\n독립적으로 확인하세요",28);Gap(20);ReasonRows(state,true);Gap(20);QuietNotice("상대가 준 번호로 되묻지 말고, 직접 찾은 공식 금융기관의 경로를 사용하세요.","phone",true)}
            else -> {Title("확인할 내용을\n정리했습니다",28);Gap(18);Copy("요청한 업무와 실제 받는 계좌가 일치하는지 확인하세요. 확인 전에는 송금하지 않아도 됩니다.");Gap(24);QuietNotice("실제 상담 접수나 보류 해제가 이루어진 것은 아닙니다. 시연 상담을 마쳐도 거래 상태는 바뀌지 않습니다.","lock")}
        }
    }
}

@Composable fun AboutScreen(back:()->Unit) {
    Screen("앱 정보",tag="about_screen",onBack=back) {
        TraceMark(Modifier.size(34.dp));Gap(24);Title("새온은행 × TRACE",28);Gap(12);Copy("거래 이전의 맥락을,\n거래가 일어나는 순간에.",size=17)
        Gap(28);DetailRow("버전",BuildConfig.VERSION_NAME);DetailRow("환경","오프라인 대회 시연");DetailRow("실제 은행 연동","없음");DetailRow("분석 방식","기기 안의 규칙 기반 분석")
        Gap(24);Rule();Gap(20);Copy("구현 경계",size=18,color=TraceColors.Ink,weight=FontWeight.SemiBold);Gap(10)
        Copy("새온은행은 가상 은행입니다. 송금, 상환 경로와 고객지원은 시연 데이터로 동작합니다. 실제 금융 거래, 운영 승인, 모델 탐지 정확도를 보증하지 않습니다.",size=14)
        Gap(20);Copy("Keystore 서명은 거래 정보 변경과 중복 실행을 검증하기 위한 참조 구현입니다. 앱 내부의 검증이 독립된 은행 서버나 하드웨어 원격 검증을 대신하지는 않습니다.",size=14)
        Gap(24);Copy("Kotlin · Jetpack Compose · AndroidX\nRoom · DataStore · Android Keystore\n기기 기본 글꼴을 사용합니다.",size=12)
    }
}

@Composable fun DemoLab(state: BankState,options: AppOptions,vm: BankViewModel,back:()->Unit) {
    var resetConfirm by remember { mutableStateOf<Scenario?>(null) }
    Screen("TRACE Demo Lab",tag="demo_lab",onBack=back) {
        Title("발표할 상황을\n선택하세요",28);Gap(14);Copy("시나리오를 선택하면 잔액과 내역을 초기화하고, 준비된 신호를 주입합니다.",size=14)
        Gap(24)
        val scenarios=listOf(Triple(Scenario.NORMAL,"정상 송금","ALLOW · 32,000원"),Triple(Scenario.IMPERSONATION,"기관 사칭","HOLD · 3,000,000원"),Triple(Scenario.LOAN,"대출 상환","VERIFY · 공식 경로"),Triple(Scenario.LOOKUP_FAILURE,"공식 경로 조회 실패","UNKNOWN · 출금 없음"),Triple(Scenario.WARN,"한 번 더 확인","WARN · 재확인 가능"))
        scenarios.forEachIndexed { i,(scenario,title,detail) -> ListRow("0${i+1}  $title",detail,tag="scenario_${scenario.name}",onClick={resetConfirm=scenario});Rule() }
        Gap(20);OptionRow("쉬운 모드","글씨와 안내를 크게 표시합니다",options.easy,"demo_easy"){vm.setOption("easy",it)}
        Secondary("신호 시간을 20분 뒤로 이동","demo_expire"){vm.advanceDemoClock()}
        Gap(10);Copy("현재: ${state.scenario.name}\n가상 시간: ${dateLabel(state.now(),"yyyy.MM.dd HH:mm:ss")}",size=12)
        Gap(24);QuietNotice("HOLD는 사용자 확인으로 풀리지 않습니다. 시연 초기화만 별도로 분리돼 있습니다. 실제 AI 모델의 탐지 결과를 재생하는 메뉴가 아닙니다.","info")
    }
    resetConfirm?.let { scenario -> TraceSheet("시연을 처음부터 시작할까요?",{resetConfirm=null}) {
        Copy("가상 거래 내역과 잔액, 위험 맥락을 초기화합니다. 실제 금융 데이터는 사용하지 않습니다.");Gap(24)
        Primary("초기화하고 시작","demo_reset_confirm"){resetConfirm=null;vm.reset(scenario)}
        Secondary("취소"){resetConfirm=null}
    } }
}
