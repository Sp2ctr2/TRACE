from pathlib import Path
root=Path('android-native')
u=root/'app/src/main/kotlin/app/saeon/trace/ui'
def edit(p,old,new):
 s=p.read_text()
 if old not in s:raise RuntimeError(f'Missing polish anchor: {p}: {old[:80]}')
 p.write_text(s.replace(old,new))
p=u/'screens/StudioScreens.kt'
edit(p,'import app.saeon.trace.core.*','import app.saeon.trace.core.*\nimport androidx.lifecycle.compose.collectAsStateWithLifecycle')
edit(p,'    val sendPress=remember','    val service by model.services.state.collectAsStateWithLifecycle()\n    val nextSchedule=service.schedules.firstOrNull{it.active}\n    val sendPress=remember')
edit(p,'shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors','contentPadding=PaddingValues(horizontal=8.dp,vertical=10.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors')
edit(p,'shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.filledTonalButtonColors','contentPadding=PaddingValues(horizontal=8.dp,vertical=10.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.filledTonalButtonColors')
edit(p,'Text("가져오기",style=MaterialTheme.typography.labelLarge)','Text("가져오기",style=MaterialTheme.typography.labelLarge,maxLines=1)')
edit(p,'        Row(Modifier.fillMaxWidth().heightIn(min=if(compact)52.dp else 64.dp)','        if(!compact) Row(Modifier.fillMaxWidth().heightIn(min=64.dp)')
edit(p,'Text(if(state.recurringEnabled)"통신비 68,000원"else"예정 내역 보기",style=MaterialTheme.typography.bodySmall)','Text(nextSchedule?.let{"${it.title} ${won(it.amount)}원"}?:"예정 내역 보기",style=MaterialTheme.typography.bodySmall,maxLines=1)')
p=u/'screens/SettingsScreens.kt'
edit(p,'@Composable fun NotificationsScreen(state: BankState, preferences: BankPreferences, model: BankViewModel, open: (String) -> Unit, back: () -> Unit) {','@Composable fun NotificationsScreen(state: BankState, preferences: BankPreferences, model: BankViewModel, open: (String) -> Unit, back: () -> Unit) {\n    val services by model.services.state.collectAsStateWithLifecycle()')
edit(p,'            if (state.recurringEnabled) {\n                MenuRow("자동이체가 예정되어 있어요.", "09월 25일 · 통신비 68,000원", BankIcons.Calendar) { open("recurring") }','            services.schedules.filter{it.active}.forEach { schedule ->\n                MenuRow("자동이체가 예정되어 있어요.", "매월 ${schedule.day}일 · ${schedule.title} ${won(schedule.amount)}원", BankIcons.Calendar) { open("recurring") }')
p=u/'screens/StudioServicesScreens.kt'
edit(p,'.testTag("inquiry_body")','.testTag("inquiry_message")')
old='val text="새온은행 × TRACE\\n시연용 $kind\\n\\n예금주 ${pref.displayName}\\n계좌 110-***-0001\\n생활통장 ${won(state.balance)}원\\n모아적금 ${won(state.savings)}원\\n발급 시각 ${dateLabel(model.repository.clock.now())}\\n\\n가상 데이터로 작성된 시연용 문서이며 증빙 효력이 없습니다."'
edit(p,old,'val text=studioDocument(kind,state,pref.displayName,model.repository.clock.now())')
p.write_text(p.read_text()+'''
/** Document contents follow the selected purpose; all remain visibly non-legal demo records. */
private fun studioDocument(kind:String,state:BankState,name:String,now:Long):String {
    val body=when(kind){
        "잔액 확인서" -> "계좌 110-***-0001\\n생활통장 ${won(state.balance)}원"
        "계좌 확인서" -> "계좌 종류 새온 생활통장\\n계좌 번호 110-***-0001"
        "거래내역 확인서" -> state.receipts.joinToString("\\n"){"${dateLabel(it.completedAt)} ${it.recipient.name} ${if(it.direction==Direction.DEBIT)\"−\"else\"+\"}${won(it.amount)}원"}
        "보유 자산 확인서" -> "생활통장 ${won(state.balance)}원\\n모아적금 ${won(state.savings)}원\\n투자 평가액 1,840,000원\\n보유 자산 합계 ${won(state.balance+state.savings+1_840_000)}원"
        "보험 납입 확인서" -> "새온 생활안심보험\\n계약번호 SIM-INS-0001\\n시연 월 납입액 28,000원\\n기준 월 2026년 9월"
        else -> error("지원하지 않는 문서입니다.")
    }
    return "새온은행 × TRACE\\n시연용 $kind\\n\\n예금주 $name\\n$body\\n발급 시각 ${dateLabel(now)}\\n\\n가상 데이터로 작성된 시연용 문서이며 증빙 효력이 없습니다."
}
''')
p=u/'screens/BankingScreens.kt'
edit(p,'Brush.linearGradient(listOf(TraceColors.Ink,TraceColors.Ink))','Brush.linearGradient(listOf(Color(0xFF20211F),Color(0xFF20211F)))') if 'Brush.linearGradient(listOf(TraceColors.Ink,TraceColors.Ink))' in p.read_text() else None
# Correct text-field tag collision and wait for the actual keyboard/window to settle before tapping.
p=root/'app/src/androidTest/kotlin/app/saeon/trace/StudioTest.kt'
edit(p,'onNodeWithTag("inquiry_body")','onNodeWithTag("inquiry_message")')
edit(p,'val expected=if(s.expected=="ALLOW")TransferStage.COMPLETE else TransferStage.valueOf(s.expected)','val expected=when(s){DemoScenario.NORMAL,DemoScenario.EDUCATION,DemoScenario.UNRELATED,DemoScenario.NEW_ACCOUNT->TransferStage.COMPLETE;DemoScenario.LOAN->TransferStage.VERIFY;DemoScenario.UNKNOWN->TransferStage.UNKNOWN;DemoScenario.WARN->TransferStage.WARN;else->TransferStage.HOLD}')
edit(p,'    };Thread.sleep(300)}','    };Thread.sleep(700);compose.waitForIdle()}')
edit(p,'capture("service_01_schedule_form",audit=false);tap("schedule_save")','capture("service_01_schedule_form",audit=false);compose.onNodeWithTag("schedule_save").assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick){it()}')
edit(p,'tap("card_loss");capture','tap("card_loss",scroll=true);capture')
edit(p,'tap("account_lock");tap','tap("account_lock",scroll=true);tap')
# The old scenario fixture expectations contain transition descriptions, not enum identifiers.
print('Polished compact hierarchy, one-line bank actions, service-to-home consistency and per-kind documents.')
