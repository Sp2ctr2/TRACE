from pathlib import Path
r=Path('android-native');u=r/'app/src/main/kotlin/app/saeon/trace/ui';d=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design'
def edit(p,a,b):
 s=p.read_text();assert a in s,(str(p),a[:100]);p.write_text(s.replace(a,b))
p=u/'screens/ViewportScreens.kt';s=p.read_text()
a=s.index('@Composable fun ViewportHome');b=s.index('@Composable fun ViewportRecipient',a);home=s[a:b]
for n in [12,18,24]:home=home.replace(f'TaskGap({n})',f'Space(if(small)2 else {n})')
a1=home.index('        SectionTitle("최근 거래"');b1=home.index('        state.receipts.take',a1)
home=home[:a1]+'''        if(small) {
            val last=state.receipts.firstOrNull()
            Row(Modifier.fillMaxWidth().heightIn(min=56.dp).clickable(role=Role.Button){open("history")}.testTag("home_recent_all"),
                verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("최근 거래",style=MaterialTheme.typography.titleSmall)
                    if(last!=null)Caption("${last.recipient.name}  ${if(last.direction==Direction.DEBIT)"−"else"+"}${won(last.amount)}원")
                }
                Text("전체",style=MaterialTheme.typography.labelMedium,color=TraceColors.Muted)
                AppIcon(BankIcons.Chevron,size=16,tint=TraceColors.Muted)
            }
        } else SectionTitle("최근 거래","전체"){open("history")}
'''+home[b1:]
home=home.replace('state.receipts.take(if(small)1 else 2)','state.receipts.take(if(small)0 else 2)')
s=s[:a]+home+s[b:]
s=s.replace('Body("방금 전의 요청이\\n이 송금과 이어져 있어요.",subdued=true)','Body(if(LocalTaskDense.current)"직전 요청과 연결된 송금이에요."else"방금 전의 요청이\\n이 송금과 이어져 있어요.",subdued=true)')
s=s.replace('reasons.take(3).forEach','reasons.take(if(LocalTaskDense.current)2 else 3).forEach')
s=s.replace('PersonBadge(i.recipient.name,48,true);TaskGap(12)','if(!LocalTaskDense.current){PersonBadge(i.recipient.name,48,true);TaskGap(12)}')
s=s.replace('Text("보낼까요?",style=MaterialTheme.typography.titleLarge)','if(!LocalTaskDense.current)Text("보낼까요?",style=MaterialTheme.typography.titleLarge)')
p.write_text(s)
p=d/'TransferJourney.kt';s=p.read_text();a=s.index('@Composable fun TraceProtectionPanel')
s=s[:a]+'''@Composable fun TraceProtectionPanel(value: ProtectionPresentation, onCancel: () -> Unit,
    onIndependentRoute: () -> Unit, onWarnAcknowledged: () -> Unit, onRetry: () -> Unit) {
    var acknowledged by remember(value) { mutableStateOf(false) }
    var details by remember(value) { mutableStateOf(false) }
    val title = when(value.kind) { ProtectionKind.Warn -> "한 번 더 확인해 주세요"; ProtectionKind.Verify -> "공식 경로로 확인해요"; ProtectionKind.Hold -> "확인하고 보내볼까요?"; ProtectionKind.Unknown -> "확인한 뒤에 보내요" }
    TaskPage(title="송금 안전 확인",tag="trace_embedded_protection",actions={AppIcon(BankIcons.Trace,tint=TraceColors.Coral)},footer={
        when(value.kind) {
            ProtectionKind.Warn -> PrimaryButton("확인한 내용으로 다시 보기",enabled=acknowledged,onClick=onWarnAcknowledged)
            ProtectionKind.Unknown -> PrimaryButton("다시 확인하기",onClick=onRetry)
            else -> PrimaryButton("공식 경로로 확인하기",onClick=onIndependentRoute)
        }
        SecondaryButton("송금 취소",onClick=onCancel)
    }) {
        TaskHeadline(title);TaskGap(14);Body(value.summary,subdued=true);TaskGap(18)
        TaskTransaction(value.recipient,value.amount);TaskGap(16)
        value.reasons.take(if(LocalTaskDense.current)2 else 3).forEach { Text(it.title,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(vertical=5.dp)) }
        if(value.reasons.isNotEmpty())QuietButton("이유 자세히",onClick={details=true})
        if(value.kind==ProtectionKind.Warn)OptionRow("받는 분과 금액을 다른 경로로 확인했어요.",acknowledged){acknowledged=it}
        Caption("아직 돈은 나가지 않았어요.")
    }
    if(details)AlertDialog(onDismissRequest={details=false},title={Text("안전 확인의 이유")},text={
        Column(Modifier.verticalScroll(rememberScrollState())){value.reasons.forEach{NumberedReason(0,it.title,it.explanation)}}
    },confirmButton={QuietButton("확인"){details=false}},containerColor=TraceColors.Surface)
}
'''
s=s.replace('import androidx.compose.foundation.Canvas','import androidx.compose.foundation.Canvas\nimport androidx.compose.foundation.verticalScroll\nimport androidx.compose.foundation.rememberScrollState')
p.write_text(s)
for name in ['ic_launcher.xml','ic_splash.xml']:
 background='<path android:fillColor="#F5F4F0" android:pathData="M0,0H108V108H0Z"/>' if name=='ic_launcher.xml' else ''
 xml='''<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">'''+background+'''<group android:scaleX="2" android:scaleY="2" android:translateX="14" android:translateY="14"><path android:fillColor="#EF4A32" android:pathData="M6 9h28v7H23.5v17h-7V16H6z"/><path android:fillColor="#EF4A32" android:pathData="M6 23h7v10H6z"/></group></vector>'''
 (r/'app/src/main/res/drawable'/name).write_text(xml)
p=r/'app/src/androidTest/kotlin/app/saeon/trace/ViewportTest.kt'
edit(p,'class ViewportTest : UiHarness() {','''class ViewportTest : UiHarness() {
    private val overflowFailures=mutableListOf<String>()
    @org.junit.After fun assertAllTaskBounds(){assertTrue(overflowFailures.joinToString("\\n"),overflowFailures.isEmpty())}''')
edit(p,'if(requireFit)assertEquals("$name has unnecessary vertical overflow",0f,max,1f)','if(requireFit&&(nodes.isEmpty()||max>1f))overflowFailures.add("$name overflow=$max; measured=${nodes.isNotEmpty()}")')
edit(p,'assertEquals(normal.recipient,risk.recipient);assertEquals(normal.amount,risk.amount)','assertEquals(normal.recipient,risk.recipient);assertEquals(normal.amount,risk.amount);assertEquals(normal.purpose,risk.purpose)')
print('Corrected compact home and HOLD without clipping or shrinking touch targets.')
# Retain 48dp targets and move secondary choices into the reserved header.
p=u/'screens/ViewportScreens.kt'
s=p.read_text()
a=s.index('@Composable fun ViewportRecipient');b=s.index('@Composable fun ViewportAmount',a)
part=s[a:b]
part=part.replace('    var query by rememberSaveable', '    val compact=androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp<640\n    var query by rememberSaveable')
part=part.replace('TaskPage("송금","transfer_recipient",root=true)', 'TaskPage("송금","transfer_recipient",root=true,actions={if(compact)QuietButton("전체 계좌"){open("recipients_all")}})')
part=part.replace('        MenuRow("계좌번호로 보내기",icon=BankIcons.Bank,tag="recipient_account_entry"){open("recipient_entry")}', '''        if(compact) {
            Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clickable(role=Role.Button){open("recipient_entry")}.testTag("recipient_account_entry"),verticalAlignment=Alignment.CenterVertically) {
                AppIcon(BankIcons.Bank,size=20);Text("계좌번호로 보내기",Modifier.weight(1f).padding(start=10.dp),style=MaterialTheme.typography.bodyMedium);AppIcon(BankIcons.Chevron,size=16)
            }
        } else MenuRow("계좌번호로 보내기",icon=BankIcons.Bank,tag="recipient_account_entry"){open("recipient_entry")}''')
part=part.replace('        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {\n            QuietButton("최근"', '        if(compact)Caption("최근",Modifier.padding(vertical=2.dp)) else Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {\n            QuietButton("최근"')
s=s[:a]+part+s[b:]
a=s.index('@Composable fun ViewportAmount');b=s.index('@Composable fun ViewportReview',a)
part=s[a:b]
part=part.replace('    val draft=state.draft', '    val compact=androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp<640\n    val draft=state.draft')
part=part.replace('TaskPage("송금 금액","transfer_amount",back=back,footer={', 'TaskPage(if(compact)"${draft.recipient.name}님에게"else"송금 금액","transfer_amount",back=back,actions={if(compact)QuietButton(selected.label,Modifier.testTag("transfer_purpose")){purposeOpen=true}},footer={')
part=part.replace('        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){\n            Text("${draft.recipient.name}님에게"', '        if(!compact)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){\n            Text("${draft.recipient.name}님에게"')
s=s[:a]+part+s[b:]
s=s.replace('TaskGap(16);Body("이미 알고 있던 연락처나 공식 앱에서 받는 분과 금액을 확인하세요.",subdued=true)', 'TaskGap(16);if(LocalTaskHeight.current>=380.dp)Body("이미 알고 있던 연락처나 공식 앱에서 받는 분과 금액을 확인하세요.",subdued=true)')
s=s.replace('TaskGap(16);TaskDetail("보내는 목적","대출 상환");TaskDetail("받는 계좌",record.intent.recipient.account)', 'TaskGap(16);if(LocalTaskHeight.current>=380.dp)TaskDetail("보내는 목적","대출 상환");TaskDetail("받는 계좌",record.intent.recipient.account)')
p.write_text(s)
print('Resolved measured compact recipient/amount/WARN/VERIFY overflow; all retained controls have normal touch targets.')
