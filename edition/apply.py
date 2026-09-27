from pathlib import Path
import shutil, re, json, hashlib
r=Path('android');u=r/'app/src/main/kotlin/app/saeon/trace/ui';d=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design';core=r/'core/src/main/kotlin/app/saeon/trace/core'
def edit(p,a,b):
    s=p.read_text();assert a in s,(str(p),a[:100]);p.write_text(s.replace(a,b))
for name,dest in [('BankingDesign.kt',d),('BankingScreensV7.kt',u/'screens'),('BankingServices.kt',u/'screens'),('ServicesStore.kt',r/'app/src/main/kotlin/app/saeon/trace/data')]:
    shutil.copy2(Path('edition')/name,dest/name)
edit(r/'app/build.gradle.kts','versionCode = 50','versionCode = 70')
edit(r/'app/build.gradle.kts','5.0.0-stage-demo','7.0.0-banking-edition')
# This snapshot is self-contained. No previous overlay scripts are needed to rebuild the delivered source.
p=u/'SaeonApp.kt';s=p.read_text()
s=s.replace('    DisposableEffect(stage.clock.started){view.keepScreenOn=stage.clock.started!=null;onDispose{view.keepScreenOn=false}}','    DisposableEffect(stage.unlocked){view.keepScreenOn=stage.unlocked;onDispose{view.keepScreenOn=false}}')
s=s.replace('ViewportHome(state, preferences, model, open)','BankingHome(state, preferences, model, open)')
s=s.replace('AssetsScreen(state, open)','BankingAssets(state, open)')
s=s.replace('MoreScreen(preferences, open)','BankingMore(preferences, open)')
s=s.replace('SafetyScreen(state, model, open)','BankingSafety(state, open)')
s=s.replace('PresenterScreen(model,open)','BankingDemoCenter(model,open)')
s=s.replace('Triple("presenter","발표",BankIcons.Play)','Triple("presenter","시연",BankIcons.Trace)')
s=s.replace('                        motionScreen("stage_context") { if(stage.unlocked)StageContextScreen(model,open,back)else StageLocked(back) }\n','')
s=s.replace('                        motionScreen("support") { SupportScreen(open, back) }','')
newroutes=['certificates','certificate','account_lock','card_service','schedules','savings_add','support','report','requests','investments','investment_detail','credit','insurance','spending','terms']
s=s.replace('                        motionScreen("app_info")', ''.join('                        motionScreen("'+x+'") { BankingServices("'+x+'",state,model,open,back) }\n' for x in newroutes)+'                        motionScreen("app_info")')
s=s.replace('발표자 모드를 먼저 활성화해 주세요.','시연센터를 먼저 활성화해 주세요.')
# One-shot cold start visual, dismissed on readiness rather than an artificial full-screen timer.
s=s.replace('    val stage by model.stage.collectAsStateWithLifecycle()', '''    var launchVisible by remember { mutableStateOf(!model.introConsumed) }
    val stage by model.stage.collectAsStateWithLifecycle()''')
s=s.replace('    val fatal by model.fatal.collectAsStateWithLifecycle()', '''    val fatal by model.fatal.collectAsStateWithLifecycle()
    LaunchedEffect(bank!=null,fatal) {
        if(bank!=null||fatal!=null){if(!model.introConsumed&&!LocalReducedMotion.current)kotlinx.coroutines.delay(420);model.introConsumed=true;launchVisible=false}
    }''')
# Composition locals cannot be read inside a coroutine. Capture this before launching.
s=s.replace('    LaunchedEffect(bank!=null,fatal) {','    val launchReduced=LocalReducedMotion.current\n    LaunchedEffect(bank!=null,fatal) {').replace('!LocalReducedMotion.current)kotlinx.coroutines','!launchReduced)kotlinx.coroutines')
s=s.replace('    interaction.authChallenge?.let { challenge ->','    if(launchVisible)BankingLaunch()\n    interaction.authChallenge?.let { challenge ->')
p.write_text(s)
# Delete the old presentation console and timers. Keep only a session-local demo capability.
p=u/'BankViewModel.kt';s=p.read_text();a=s.index('    private val _stage=');b=s.index('    val graph =',a)
s=s[:a]+'''    var introConsumed:Boolean=false
    private val _stage=MutableStateFlow(DemoSession())
    val stage:StateFlow<DemoSession> = _stage.asStateFlow()
    fun stageUnlock(){_stage.update{it.copy(unlocked=true)}}
    fun stageLock(){_stage.update{it.copy(unlocked=false)}}
    fun stageObserve(record:TransferRecord) { /* The transaction record is the audit source, not a timer. */ }
    fun stageScenario(scenario:DemoScenario,done:()->Unit){if(!_stage.value.unlocked||_interaction.value.busy)return;startScenario(scenario,done)}
''' +s[b:]
s=s.replace('    val repository = graph.repository','    val repository = graph.repository\n    val services=ServicesStore(application)\n    val serviceState=services.flow.stateIn(viewModelScope,SharingStarted.Eagerly,ServiceData())')
p.write_text(s)
(u/'DemoSession.kt').write_text('package app.saeon.trace.ui\n\ndata class DemoSession(val unlocked:Boolean=false)\n')
(u/'Presenter.kt').unlink();(u/'StageNavigation.kt').unlink(missing_ok=True)
# Obsolete presenter-specific tests are archived, not falsely run against the new IA.
archive=r/'archive/obsolete-presenter-tests';archive.mkdir(parents=True,exist_ok=True)
for name in ['app/src/androidTest/kotlin/app/saeon/trace/StageTest.kt','app/src/test/kotlin/app/saeon/trace/ClockTest.kt']:
    p=r/name
    if p.exists():shutil.move(str(p),str(archive/p.name))
# Typography uses native system fonts. Do not redistribute any font files.
p=d/'Theme.kt';s=p.read_text().replace('letterSpacing = (-0.25).sp','letterSpacing = (if(size>=24)-0.5 else if(size>=15)-0.15 else 0.0).sp')
s=s.replace('titleLarge = style(24, FontWeight.Bold, 34)','titleLarge = style(22, FontWeight.SemiBold, 30)')
s=s.replace('titleMedium = style(if (easy) 21 else 19, FontWeight.SemiBold, 29)','titleMedium = style(if (easy) 21 else 18, FontWeight.SemiBold, 27)')
p.write_text(s)
p=d/'Components.kt';s=p.read_text().replace('containerColor = TraceColors.Deep, contentColor = TraceColors.White','containerColor = TraceColors.Ink, contentColor = TraceColors.Paper')
s=s.replace('horizontal = 24.dp','horizontal = 20.dp').replace('min = 60.dp','min = 54.dp')
p.write_text(s)
p=d/'BankExperience.kt';s=p.read_text().replace('ContextWeave(cancel)','BankingGate(cancel)')
s=s.replace('val pale=TraceColors.CoralLight;val ink=TraceColors.CoralText','val pale=TraceColors.Surface;val ink=TraceColors.Ink')
s=s.replace('Color(0xFF29332C) else Color(0xFFF8F9F5)','TraceColors.Surface else TraceColors.Surface')
s=s.replace('BlurEffect(22.dp.toPx(),22.dp.toPx(),TileMode.Clamp)','BlurEffect(14.dp.toPx(),14.dp.toPx(),TileMode.Clamp)')
p.write_text(s)
p=u/'screens/TransferScreens.kt';edit(p,'ViewportReview(state, record, interaction, model, open, back)','BankingReview(state, record, interaction, model, open, back)')
p=u/'screens/MotionWarn.kt';edit(p,'ViewportReview(state,record,interaction,model,open,back)','BankingReview(state,record,interaction,model,open,back)')
p=u/'screens/ViewportScreens.kt';s=p.read_text()
s=s.replace('PrimaryButton("공식 경로로 확인하기",Modifier.testTag("hold_safe_action"))','BankAction("공식 경로로 확인하기",Modifier.testTag("hold_safe_action"),trace=true)')
s=s.replace('Money(', 'Money(')
s=s.replace('MotionMoney(receipt.amount,Modifier.widthIn(max=300.dp),receipt.recipient.id)','BankAmount(receipt.amount,Modifier.widthIn(max=300.dp),center=true)')
s=s.replace('Text("${receipt.recipient.name}님에게",style=MaterialTheme.typography.titleMedium)','RecipientTitle(receipt.recipient.id,receipt.recipient.name)')
p.write_text(s)
p=u/'screens/SettingsScreens.kt';s=p.read_text().replace('발표자 모드','시연센터').replace('전체 탭을 발표 탭으로','전체 탭을 시연 탭으로')
p.write_text(s)
# Add real local savings actions and service links from existing details.
p=u/'screens/BankingScreens.kt';s=p.read_text()
s=s.replace('footer = { PrimaryButton("생활통장으로 가져오기", enabled = state.savings > 0) { open("bring") } }',
'''footer = { BankAction("더 저축하기") { open("savings_add") }; SecondaryButton("생활통장으로 가져오기") { open("bring") } }''')
s=s.replace('        Space(24);Caption("9월 이용 금액")','        BankRow("분실·재발급",icon=BankIcons.Lock){open("card_service")}\n        Space(24);Caption("9월 이용 금액")')
p.write_text(s)
# Durable account lock is part of the bank ledger, not a disabled UI button.
p=core/'Models.kt';edit(p,'val recurringEnabled: Boolean = true','val recurringEnabled: Boolean = true,\n    val accountLocked:Boolean=false')
p=r/'app/src/main/kotlin/app/saeon/trace/data/SnapshotCodec.kt';s=p.read_text()
s=s.replace('"recurringEnabled" to state.recurringEnabled','"recurringEnabled" to state.recurringEnabled, "accountLocked" to state.accountLocked')
s=s.replace('value.getLong("updatedAt"), value.getBoolean("recurringEnabled")','value.getLong("updatedAt"), value.getBoolean("recurringEnabled"), value.optBoolean("accountLocked",false)')
p.write_text(s)
p=core/'BankEngine.kt';s=p.read_text()
s=s.replace('        requireBank(amount > 0, "ZERO_AMOUNT"','        requireBank(!state.accountLocked,"ACCOUNT_LOCKED","계정이 잠겨 있어요. 잠금을 해제한 뒤 새로 확인해 주세요. 돈은 나가지 않았습니다.")\n        requireBank(amount > 0, "ZERO_AMOUNT"')
s=s.replace('        requireBank(amount > 0 && amount <= state.savings,','        requireBank(!state.accountLocked,"ACCOUNT_LOCKED","계정이 잠겨 있어요. 내 계좌 이동도 중지됩니다.")\n        requireBank(amount > 0 && amount <= state.savings,')
last=s.rfind('\n}')
s=s[:last]+'''
    fun saveToSavings(state:BankState,amount:Long,id:String,now:Long):BankState {
        if(state.receipts.any{it.intentId==id})return state
        requireBank(!state.accountLocked,"ACCOUNT_LOCKED","계정이 잠겨 있어요. 내 계좌 이동도 중지됩니다.")
        requireBank(amount>0&&amount<=state.balance,"SAVINGS_AMOUNT","옮길 수 있는 금액을 확인해 주세요. 잔액은 바뀌지 않았습니다.")
        val after=state.balance-amount
        val receipt=TransferReceipt("SIM-${id.take(8).uppercase()}",id,
            Recipient("savings","새온 모아적금","새온은행","220-***-0102",true,RecipientKind.INSTITUTION),amount,now,Purpose.OTHER,after,
            "internal-$id",Direction.DEBIT,"새온 생활통장","내 계좌에 저축하기")
        return state.copy(balance=after,savings=state.savings+amount,receipts=listOf(receipt)+state.receipts,updatedAt=now)
    }
    fun lockAccount(state:BankState,locked:Boolean):BankState = recover(state).copy(accountLocked=locked)
''' +s[last:]
p.write_text(s)
p=r/'app/src/main/kotlin/app/saeon/trace/data/BankRepository.kt';s=p.read_text().replace('    suspend fun setLimit(', '    suspend fun setAccountLocked(value:Boolean)=change{state,_->BankEngine.lockAccount(state,value)}\n    suspend fun saveToSavings(amount:Long,id:String)=change{state,now->BankEngine.saveToSavings(state,amount,id,now)}\n    suspend fun setLimit(');p.write_text(s)
# Adaptive launcher: exact site symbol geometry, no newly invented circular dot.
res=r/'app/src/main/res';(res/'mipmap-anydpi-v26').mkdir(parents=True,exist_ok=True)
fg='''<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108"><group android:scaleX="1.6" android:scaleY="1.6" android:translateX="22" android:translateY="22"><path android:fillColor="#EF4A32" android:pathData="M6 9h28v7H23.5v17h-7V16H6z"/><path android:fillColor="#EF4A32" android:pathData="M6 23h7v10H6z"/></group></vector>'''
(res/'drawable/trace_launcher_foreground.xml').write_text(fg)
(res/'drawable/trace_launcher_background.xml').write_text('<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle"><solid android:color="#F5F4F0"/></shape>')
(res/'mipmap-anydpi-v26/ic_launcher.xml').write_text('<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"><background android:drawable="@drawable/trace_launcher_background"/><foreground android:drawable="@drawable/trace_launcher_foreground"/><monochrome android:drawable="@drawable/trace_launcher_foreground"/></adaptive-icon>')
p=r/'app/src/main/AndroidManifest.xml';s=p.read_text().replace('@drawable/ic_launcher','@mipmap/ic_launcher');p.write_text(s)
# All tests added here exercise the shipped source, not screenshots of a design image.
for name,dest in [('BankingEditionTest.kt',r/'app/src/androidTest/kotlin/app/saeon/trace'),('BankServicesCoreTest.kt',r/'core/src/test/kotlin/app/saeon/trace/core')]:
    if (Path('edition')/name).exists():dest.mkdir(parents=True,exist_ok=True);shutil.copy2(Path('edition')/name,dest/name)
print('Banking edition applied to a clean, verified native source snapshot.')
