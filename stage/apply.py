from pathlib import Path
import shutil,re
r=Path('android-native');u=r/'app/src/main/kotlin/app/saeon/trace/ui';d=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design'
def edit(p,a,b):
 s=p.read_text();assert a in s,(str(p),a[:100]);p.write_text(s.replace(a,b))
shutil.copy2('stage/TraceMotion.kt',d/'TraceMotion.kt');shutil.copy2('stage/Presenter.kt',u/'Presenter.kt')
edit(u/'Presenter.kt','DemoScenario.EXPIRED','DemoScenario.EDUCATION')
edit(r/'app/build.gradle.kts','versionCode = 40','versionCode = 50');edit(r/'app/build.gradle.kts','4.0.0-viewport-demo','5.0.0-stage-demo')
# Shared press behavior retains Material semantics, ripple, keyboard and cancellation.
p=d/'Components.kt';s=p.read_text();s=s.replace('import androidx.compose.foundation.*','import androidx.compose.foundation.*\nimport androidx.compose.foundation.interaction.MutableInteractionSource')
s=s.replace('    IconButton(onClick, modifier.sizeIn','    val press=remember{MutableInteractionSource()}\n    IconButton(onClick, modifier.tracePress(press).sizeIn')
s=s.replace('contentDescription = label }) {','contentDescription = label }, interactionSource=press) {')
s=s.replace('    Button(onClick = onClick, modifier = modifier.fillMaxWidth()', '    val press=remember{MutableInteractionSource()}\n    Button(onClick = onClick, interactionSource=press, modifier = modifier.tracePress(press).fillMaxWidth()')
s=s.replace('    TextButton(onClick = onClick, modifier = modifier.heightIn','    val press=remember{MutableInteractionSource()}\n    TextButton(onClick = onClick, interactionSource=press, modifier = modifier.tracePress(press).heightIn')
s=s.replace('style = MaterialTheme.typography.titleMedium)\n        if (action', 'style = MaterialTheme.typography.titleSmall)\n        if (action')
p.write_text(s)
# Accessibility and renderer cost control are independent settings.
p=r/'app/src/main/kotlin/app/saeon/trace/data/Preferences.kt'
edit(p,'val cardFrozen: Boolean = false','val cardFrozen: Boolean = false, val reducedTransparency:Boolean=false')
edit(p,'private val motion =','private val transparency=booleanPreferencesKey("reduced_transparency")\n    private val motion =')
edit(p,'it[cardFrozen] ?: false)','it[cardFrozen] ?: false,it[transparency] ?: false)')
edit(p,'suspend fun motion(value: Boolean)','suspend fun transparency(value:Boolean){store.edit{it[transparency]=value}}\n    suspend fun motion(value: Boolean)')
p=d/'BankExperience.kt';s=p.read_text();a=s.index('@Composable fun ContextLoading');s=s[:a]+'@Composable fun ContextLoading(cancel:()->Unit) { ContextWeave(cancel) }\n'
s=s.replace('val reduced=LocalReducedMotion.current','val reduced=LocalReducedMotion.current')
s=s.replace('drawCircle(if(kind=="complete")coral else pale)','drawCircle(if(kind=="complete")pale else pale)')
s=s.replace('drawPath(drawn,on,','drawPath(drawn,ink,')
s=s.replace('if(Build.VERSION.SDK_INT>=31)', 'if(Build.VERSION.SDK_INT>=31 && !solid)')
s=s.replace('val dark=', 'val solid=LocalReducedTransparency.current\n    val dark=',1)
s=s.replace('if(Build.VERSION.SDK_INT<31).97f','if(solid||Build.VERSION.SDK_INT<31)1f')
s=s.replace('spring(dampingRatio=.82f,stiffness=450f)','spring(dampingRatio=1f,stiffness=520f)')
p.write_text(s)
p=u/'BankViewModel.kt';s=p.read_text();a=s.index('    val graph =')
s=s[:a]+'''    private val _stage=MutableStateFlow(StageState())
    val stage:StateFlow<StageState> = _stage.asStateFlow()
    private val stageSeen=mutableSetOf<String>()
    fun stageUnlock(){_stage.update{it.copy(unlocked=true)}}
    fun stageLock(){_stage.update{it.copy(unlocked=false,clock=it.clock.pause(android.os.SystemClock.elapsedRealtime()))}}
    fun stagePreset(seconds:Int){if(!_stage.value.unlocked)return;require(seconds==180||seconds==55);stageSeen.clear();_stage.update{it.copy(clock=RehearsalClock(seconds,if(seconds==55)75 else 0),selected=if(seconds==55)5 else 0,notes=emptyList())}}
    fun stageTimerToggle(){if(!_stage.value.unlocked)return;val now=android.os.SystemClock.elapsedRealtime();_stage.update{it.copy(clock=if(it.clock.started==null)it.clock.start(now)else it.clock.pause(now))}}
    fun stageSelect(index:Int){if(_stage.value.unlocked)_stage.update{it.copy(selected=index.coerceIn(0,StageCues.lastIndex))}}
    fun stageObserve(record:TransferRecord){
        if(!_stage.value.unlocked||record.stage !in setOf(TransferStage.COMPLETE,TransferStage.HOLD,TransferStage.WARN,TransferStage.VERIFY,TransferStage.UNKNOWN))return
        if(!stageSeen.add(record.intent.id+record.stage.name))return
        _stage.update{it.copy(notes=(it.notes+StageNote((it.clock.elapsed(android.os.SystemClock.elapsedRealtime())/1000).toInt(),it.scene,record.stage.name,record.intent.amount)).takeLast(200))}
    }
    fun stageScenario(scenario:DemoScenario,done:()->Unit){if(!_stage.value.unlocked||_interaction.value.busy)return;_stage.update{it.copy(scene=scenario.label)};startScenario(scenario,done)}
    fun stageContext(done:()->Unit){if(!_stage.value.unlocked)return;act{repository.reset(DemoScenario.IMPERSONATION);_stage.update{it.copy(scene="사건 맥락")};done()}}
''' +s[a:]
s=s.replace('graph.preferences.motion(appearance.reducedMotion)','graph.preferences.motion(appearance.reducedMotion)\n        graph.preferences.transparency(appearance.reducedTransparency)')
p.write_text(s)
# Main app: session-only hidden fifth tab. Explicit mode, not a sixth crowded dock.
p=u/'SaeonApp.kt';s=p.read_text().replace('import androidx.compose.animation.fadeIn','import androidx.compose.animation.*\nimport androidx.navigation.NavGraphBuilder\nimport androidx.navigation.NavBackStackEntry\nimport androidx.compose.ui.platform.LocalView\nimport androidx.compose.animation.fadeIn')
s=s.replace('@OptIn(ExperimentalMaterial3Api::class)','@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)')
s=s.replace('    val bank by', '    val stage by model.stage.collectAsStateWithLifecycle()\n    val view=LocalView.current\n    DisposableEffect(stage.clock.started){view.keepScreenOn=stage.clock.started!=null;onDispose{view.keepScreenOn=false}}\n    val bank by',1)
s=s.replace('val roots = setOf("home", "assets", "transfer", "safety", "more")','val roots = setOf("home", "assets", "transfer", "safety", "more", "presenter")')
s=s.replace('            val current = record.intent.id to record.stage','            model.stageObserve(record)\n            val current = record.intent.id to record.stage')
s=s.replace('if (lastState != null && current != lastState)', 'if (!reduced && lastState != null && current != lastState)')
s=s.replace('val home: () -> Unit = { nav.navigate("home")', 'val home: () -> Unit = { nav.navigate(if(stage.unlocked)"presenter"else"home")')
s=s.replace('CompositionLocalProvider(LocalPageBottomInset provides', 'CompositionLocalProvider(LocalReducedTransparency provides preferences.reducedTransparency, LocalPageBottomInset provides')
s=s.replace('                    NavHost(', '                    SharedTransitionLayout { CompositionLocalProvider(LocalMotionShared provides this) {\n                    NavHost(')
s=s.replace('fadeIn(tween(if (reduced) 0 else 180))','fadeIn(tween(if(reduced)0 else TraceMotion.Spatial))+slideInHorizontally(tween(if(reduced)0 else TraceMotion.Spatial,easing=TraceMotion.Enter)){if(reduced)0 else 24}')
s=s.replace('fadeOut(tween(if (reduced) 0 else 120))','fadeOut(tween(if(reduced)0 else TraceMotion.Micro))')
s=s.replace('                        composable(', '                        motionScreen(')
s=s.replace('motionScreen("app_info") { AppInfoScreen(open, back) }','motionScreen("app_info") { StageAppInfoScreen(model, open, back) }\n                        motionScreen("presenter") { PresenterScreen(model,open) }\n                        motionScreen("stage_context") { if(stage.unlocked)StageContextScreen(model,open,back)else StageLocked(back) }')
s=s.replace('motionScreen("comparison") { ComparisonScreen(model, open, back) }','motionScreen("comparison") { if(stage.unlocked)ComparisonScreen(model, open, back)else StageLocked(back) }')
s=s.replace('motionScreen("demo_lab") { DemoLabScreen(state, model, open, home, back) }\n                    }','motionScreen("demo_lab") { if(stage.unlocked)DemoLabScreen(state, model, open, home, back)else StageLocked(back) }\n                    }\n                    }}')
s=s.replace('motionScreen("scenario/{id}") { target -> ScenarioStoryScreen(', 'motionScreen("scenario/{id}") { target -> if(stage.unlocked)ScenarioStoryScreen(')
s=s.replace('Triple("more","전체",BankIcons.More))','if(stage.unlocked)Triple("presenter","발표",BankIcons.Play)else Triple("more","전체",BankIcons.More))')
s += '''
@Composable private fun StageLocked(back:()->Unit){Page(title="앱 정보",back=back){Body("발표자 모드를 먼저 활성화해 주세요.")}}
private fun NavGraphBuilder.motionScreen(route:String,content:@Composable (NavBackStackEntry)->Unit){
    composable(route){entry->CompositionLocalProvider(LocalMotionVisibility provides this){content(entry)}}
}
'''
p.write_text(s)
# Home: one quiet account block, aligned rows, no feature cards competing for attention.
p=u/'screens/ViewportScreens.kt';s=p.read_text();a=s.index('@Composable fun ViewportHome');b=s.index('@Composable fun ViewportRecipient',a)
s=s[:a]+'''@Composable fun ViewportHome(state:BankState,preferences:BankPreferences,model:BankViewModel,open:(String)->Unit) {
    TaskPage("새온은행","home",root=true,actions={IconAction(BankIcons.Bell,"알림"){open("notifications")};IconAction(BankIcons.Profile,"내 정보"){open("profile")}}) {
        val small=LocalTaskHeight.current<450.dp
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f).heightIn(min=48.dp).clickable(role=Role.Button){open("account")},verticalArrangement=Arrangement.Center) {
                Text("새온 생활통장",style=MaterialTheme.typography.bodyMedium,color=TraceColors.Muted)
                if(!small)Caption("110-***-0001")
            }
            IconAction(BankIcons.Eye,if(preferences.hideBalance)"잔액 보이기"else"잔액 숨기기"){model.preference{hideBalance(!preferences.hideBalance)}}
        }
        if(preferences.hideBalance)Text("잔액 숨김",style=MaterialTheme.typography.displaySmall)
        else MotionMoney(state.balance,Modifier.fillMaxWidth().testTag("home_balance"))
        Space(if(small)8 else 20)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1.6f)){PrimaryButton("송금",Modifier.testTag("home_transfer")){open("transfer")}}
            Box(Modifier.weight(1f)){SecondaryButton("가져오기",Modifier.testTag("home_bring")){open("bring")}}
        }
        Space(if(small)8 else 24)
        if(small)Row(Modifier.fillMaxWidth()){
            listOf(Triple("이번 달 쓴 돈","382,400원","card"),Triple("모아둔 돈","${won(state.savings)}원","savings")).forEach{(label,value,route)->
                Column(Modifier.weight(1f).heightIn(min=48.dp).clickable(role=Role.Button){open(route)},verticalArrangement=Arrangement.Center){Caption(label);Text(value,style=MaterialTheme.typography.titleSmall)}
            }
        } else listOf(Triple("이번 달 쓴 돈","382,400원","card"),Triple("모아둔 돈","${won(state.savings)}원","savings")).forEach{(label,value,route)->
            Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clickable(role=Role.Button){open(route)},verticalAlignment=Alignment.CenterVertically){
                Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,color=TraceColors.Muted)
                Text(value,style=MaterialTheme.typography.titleSmall);Spacer(Modifier.width(9.dp));AppIcon(BankIcons.Chevron,size=15,tint=TraceColors.Muted)
            }
        }
        Space(if(small)4 else 16);Rule()
        Row(Modifier.fillMaxWidth().heightIn(min=if(small)48.dp else 66.dp).clickable(role=Role.Button){open("safety")},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
            AppIcon(BankIcons.Trace,tint=TraceColors.Coral,size=26)
            Column(Modifier.weight(1f)){Text(if(state.pending.isEmpty())"보내기 전, 한 번 더."else"보류한 송금 ${state.pending.size}건",style=MaterialTheme.typography.titleSmall);if(!small)Caption("송금 앞의 맥락을 확인해요")}
            AppIcon(BankIcons.Chevron,size=15,tint=TraceColors.Muted)
        }
        Rule();Space(if(small)2 else 10)
        if(!small)SectionTitle("최근 거래","전체"){open("history")}
        state.receipts.take(if(small)1 else 2).forEach{receipt->
            Row(Modifier.fillMaxWidth().heightIn(min=56.dp).clickable(role=Role.Button){open(if(small)"history"else"receipt/${receipt.id}")},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                if(!small)AppIcon(if(receipt.direction==Direction.CREDIT)BankIcons.Download else BankIcons.Card,size=20,tint=TraceColors.Muted)
                Column(Modifier.weight(1f)){if(small)Caption("최근 거래 · 전체 보기");Text(receipt.recipient.name,style=MaterialTheme.typography.bodyMedium);if(!small)Caption(receipt.memo.ifEmpty{receipt.purpose.label})}
                Text("${if(receipt.direction==Direction.DEBIT)"−"else"+"}${won(receipt.amount)}원",style=MaterialTheme.typography.titleSmall)
            }
        }
    }
}

''' +s[b:]
s=s.replace('Money(amount,Modifier.weight(1f).testTag("amount_value"))','MotionMoney(amount,Modifier.weight(1f).testTag("amount_value"),draft.recipient.id)')
s=s.replace('Text("${draft.recipient.name}님에게",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)','RecipientTitle(draft.recipient.id,draft.recipient.name,Modifier.weight(1f))')
s=s.replace('Text("${i.recipient.name}님에게",style=MaterialTheme.typography.titleMedium,textAlign=TextAlign.Center)','RecipientTitle(i.recipient.id,i.recipient.name)')
s=s.replace('Text("${won(i.amount)}원",style=MaterialTheme.typography.displaySmall,modifier=Modifier.semantics{heading()})','MotionMoney(i.amount,Modifier.widthIn(max=300.dp),i.recipient.id)')
s=s.replace('Modifier.size(18.dp),tint=TraceColors.Coral','Modifier.size(18.dp).traceShared("trace-mark"),tint=TraceColors.Coral')
s=s.replace('fun ViewportHold(record:TransferRecord','fun ViewportHold(state:BankState,record:TransferRecord')
a=s.index('        reasons.take(if(LocalTaskDense.current)2 else 3).forEach');b=s.index('        Row{QuietButton("이유 자세히"',a)
s=s[:a]+'''        val events=state.context.relevant(record.intent,record.intent.createdAt).filter{it.type in record.reasons}.sortedBy{it.createdAt}.distinctBy{it.type}
        val thread=events.take(if(LocalTaskDense.current)1 else 2).map{ThreadNode(timeLabel(it.createdAt),it.type.label)}+
            ThreadNode(timeLabel(record.intent.createdAt),if(record.intent.recipient.known)"이 계좌로 송금 시도"else"새로운 계좌로 송금 시도")
        ContextThread(thread,record.intent.id,LocalTaskDense.current)
''' +s[b:]
# Expand the real receipt without leaving the completion experience.
s=s.replace('    val receipt=state.receipts.find{it.intentId==record.intent.id&&!it.seed}', '    var receiptOpen by rememberSaveable(record.intent.id){mutableStateOf(false)}\n    val receipt=state.receipts.find{it.intentId==record.intent.id&&!it.seed}')
s=s.replace('Text("${receipt.recipient.name}님에게",style=MaterialTheme.typography.titleMedium)','RecipientTitle(receipt.recipient.id,receipt.recipient.name)')
s=s.replace('Text("${won(receipt.amount)}원을",style=MaterialTheme.typography.headlineLarge,modifier=Modifier.semantics{heading()})','MotionMoney(receipt.amount,Modifier.widthIn(max=300.dp),receipt.recipient.id)')
s=s.replace('QuietButton("송금 내역 보기",Modifier.testTag("complete_receipt")){open("receipt/${receipt.id}")}','QuietButton("송금 내역 보기",Modifier.testTag("complete_receipt")){receiptOpen=true}')
s=s.replace('if(state.scenario==DemoScenario.NEW_ACCOUNT)QuietButton','if(modelStageVisible())QuietButton') if False else s
anchor='\n@Composable fun ComparisonScreen';idx=s.index(anchor);pre=s[:idx];cut=pre.rfind('\n}')
pre=pre[:cut]+'''
    if(receiptOpen)ModalBottomSheet(onDismissRequest={receiptOpen=false},containerColor=TraceColors.Surface){
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp).testTag("receipt_expanded")){
            Text("송금 내역",style=MaterialTheme.typography.headlineSmall);TaskGap(16)
            TaskDetail("받는 분",receipt.recipient.name);TaskDetail("받는 계좌","${receipt.recipient.bank} ${receipt.recipient.account}")
            TaskDetail("출금 계좌",receipt.fromAccount);TaskDetail("금액","${won(receipt.amount)}원")
            TaskDetail("거래번호",receipt.id);TaskGap(16);Caption("실제 자금 이동이 없는 시연 영수증입니다.")
            QuietButton("전체 내역",Modifier.fillMaxWidth()){receiptOpen=false;open("receipt/${receipt.id}")}
            PrimaryButton("닫기",Modifier.testTag("receipt_collapse")){receiptOpen=false}
        }
    }
''' +pre[cut:]
s=pre+s[idx:];s='@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)\n'+s
p.write_text(s)
# Record transitions are keyed by transaction and visual state. Authentication
# remains a modal and is not an authorization to pay.
p=u/'screens/TransferScreens.kt';s=p.read_text();s=s.replace('import androidx.activity.compose.BackHandler','import androidx.activity.compose.BackHandler\nimport androidx.compose.animation.*\nimport androidx.compose.animation.core.tween')
a=s.index('    when (record.stage) {',s.index('@Composable fun TransferStateScreen'));b=s.index('\n}\n\n@Composable private fun ReviewScreen',a)
part=s[a:b];part=part.replace('ViewportHold(record,','ViewportHold(state,record,')
part=part.replace('    when (record.stage)', '    when (record.stage)')
s=s[:a]+'''    val reduced=LocalReducedMotion.current
    AnimatedContent(targetState=record,contentKey={it.intent.id+if(it.stage==TransferStage.AUTHORIZING)"REVIEW"else it.stage.name},
        transitionSpec={if(reduced)EnterTransition.None togetherWith ExitTransition.None else
            (fadeIn(tween(TraceMotion.Spatial))+slideInVertically(tween(TraceMotion.Spatial,easing=TraceMotion.Enter)){it/70}) togetherWith fadeOut(tween(120))},label="transaction_state") { snapshot ->
        CompositionLocalProvider(LocalMotionVisibility provides this) {
        val record=snapshot
''' +part+ '\n        }\n    }'+s[b:];p.write_text(s)
# Replace public lab entry with a seven-tap, opt-in session gate.
p=u/'screens/SettingsScreens.kt';s=p.read_text()
s=s.replace('        MenuRow("시연 센터", "12가지 맥락과 거래의 차이", BankIcons.Trace, tag = "demo_center_open") { open("demo_lab") }\n','')
s=s.replace('        Space(20); Caption("시연의 기본 화면은', '        OptionRow("투명 효과 줄이기",preferences.reducedTransparency,"유리 배경을 불투명하게 표시해요."){model.preference{transparency(it)}}\n        Space(20); Caption("시연의 기본 화면은')
s+='''
@Composable fun StageAppInfoScreen(model:BankViewModel,open:(String)->Unit,back:()->Unit){
    var taps by remember{mutableIntStateOf(0)};var last by remember{mutableLongStateOf(0)};var ask by remember{mutableStateOf(false)}
    Page("앱 정보","app_info",back=back){
        TaskGap(20);TraceSignature("새온은행 × TRACE");TaskGap(24);Headline("송금 앞의 맥락을\\n연결합니다.");TaskGap(24)
        Row(Modifier.fillMaxWidth().heightIn(min=64.dp).clickable(role=Role.Button){
            val now=android.os.SystemClock.elapsedRealtime();taps=if(now-last>8000)1 else taps+1;last=now
            if(taps>=7){taps=0;ask=true}
        }.testTag("app_version"),verticalAlignment=Alignment.CenterVertically){Text("버전",Modifier.weight(1f));Text(BuildConfig.VERSION_NAME,style=MaterialTheme.typography.bodySmall)}
        if(taps in 4..6)Caption("발표자 모드까지 ${7-taps}번 남았어요.")
        TaskDetail("실행 방식","네이티브 오프라인 시연");TaskDetail("은행 연결","실제 금융망 연결 없음");TaskDetail("학습 가중치","미연결 · 로컬 시연 정책")
        TaskGap(20);Body("금융 데이터와 위험 맥락은 모두 시연용입니다.",subdued=true)
    }
    if(ask)AlertDialog(onDismissRequest={ask=false},title={Text("발표자 모드를 켤까요?")},text={Text("하단의 전체 탭을 발표 탭으로 바꿉니다. 시연 준비 기능은 가상 계좌를 초기화할 수 있어요. 앱을 완전히 종료하면 다시 숨겨집니다.")},
        confirmButton={QuietButton("활성화",Modifier.testTag("stage_unlock_confirm")){model.stageUnlock();ask=false;open("presenter")}},dismissButton={QuietButton("취소"){ask=false}})
}
'''
p.write_text(s)
# Avoid presenting the comparison tool to ordinary bank users.
p=u/'screens/ViewportScreens.kt';edit(p,'if(state.scenario==DemoScenario.NEW_ACCOUNT)QuietButton','if(false)QuietButton')
# Fixed runtime inputs and tests are copied only into the demo app module.
if Path('stage/StageTest.kt').exists():shutil.copy2('stage/StageTest.kt',r/'app/src/androidTest/kotlin/app/saeon/trace/StageTest.kt')
if Path('stage/ClockTest.kt').exists():
 t=r/'app/src/test/kotlin/app/saeon/trace';t.mkdir(parents=True,exist_ok=True);shutil.copy2('stage/ClockTest.kt',t/'ClockTest.kt')
print('Stage motion integrated. No policy engine, signal thresholds, authentication or ledger bypass changes.')
