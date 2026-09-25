from pathlib import Path
r=Path('android-native');u=r/'app/src/main/kotlin/app/saeon/trace/ui'
def edit(p,a,b):
 s=p.read_text();assert a in s,(str(p),a[:100]);p.write_text(s.replace(a,b))
p=r/'app/src/main/kotlin/app/saeon/trace/MainActivity.kt'
s=p.read_text();a=s.index('            SideEffect {');b=s.index('            SaeonTheme(',a)
s=s[:a]+'''            DisposableEffect(dark) {
                val decor=window.decorView
                val update=Runnable {
                    val style=if(dark)SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        else SystemBarStyle.light(android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT)
                    enableEdgeToEdge(statusBarStyle=style,navigationBarStyle=style)
                    androidx.core.view.WindowCompat.getInsetsController(window,decor).apply {
                        isAppearanceLightStatusBars=!dark
                        isAppearanceLightNavigationBars=!dark
                    }
                    if(Build.VERSION.SDK_INT>=29)window.isNavigationBarContrastEnforced=false
                }
                val focus=android.view.ViewTreeObserver.OnWindowFocusChangeListener { focused -> if(focused)decor.post(update) }
                decor.viewTreeObserver.addOnWindowFocusChangeListener(focus)
                decor.post(update)
                onDispose {
                    decor.removeCallbacks(update)
                    if(decor.viewTreeObserver.isAlive)decor.viewTreeObserver.removeOnWindowFocusChangeListener(focus)
                }
            }
''' +s[b:];p.write_text(s)
p=u/'screens/ViewportScreens.kt'
edit(p,'val events=state.context.relevant(record.intent,record.intent.createdAt).filter{it.type in record.reasons}.sortedBy{it.createdAt}.distinctBy{it.type}',
'''val allEvents=state.context.relevant(record.intent,record.intent.createdAt).filter{it.type in record.reasons}.sortedBy{it.createdAt}.distinctBy{it.type}
        val events=allEvents.filter{it.type!=RiskType.SUSPICIOUS_LINK}.ifEmpty{allEvents}''')
s=p.read_text();a=s.index('    if(receiptOpen)ModalBottomSheet');b=s.index('\n}\n@Composable fun ComparisonScreen',a)
s=s[:a]+'''    if(receiptOpen)ModalBottomSheet(onDismissRequest={receiptOpen=false},
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=TraceColors.Surface){
        val limit=(androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp*.82f).dp
        Column(Modifier.fillMaxWidth().heightIn(max=limit).padding(horizontal=20.dp).padding(bottom=12.dp).testTag("receipt_expanded")){
            Column(Modifier.weight(1f,false).fillMaxWidth().verticalScroll(rememberScrollState())){
                Text("송금 내역",style=MaterialTheme.typography.headlineSmall);TaskGap(16)
                TaskDetail("받는 분",receipt.recipient.name)
                TaskDetail("받는 계좌","${receipt.recipient.bank} ${receipt.recipient.account}")
                TaskDetail("출금 계좌",receipt.fromAccount);TaskDetail("금액","${won(receipt.amount)}원")
                TaskDetail("거래번호",receipt.id);TaskGap(12)
                Caption("실제 자금 이동이 없는 시연 영수증입니다.")
                QuietButton("전체 내역",Modifier.fillMaxWidth()){receiptOpen=false;open("receipt/${receipt.id}")}
            }
            Space(8);PrimaryButton("닫기",Modifier.testTag("receipt_collapse")){receiptOpen=false}
        }
    }
''' +s[b:];p.write_text(s)
p=r/'app/src/androidTest/kotlin/app/saeon/trace/StageTest.kt'
edit(p,'picture("expanded_receipt");tap("receipt_collapse")', 'picture("expanded_receipt");compose.onNodeWithTag("receipt_collapse").assertIsDisplayed();tap("receipt_collapse")')
edit(p,'picture("dark_hold");navigate("home");fit("home");picture("dark_home")', '''Thread.sleep(1200)
        compose.runOnIdle {
            val control=androidx.core.view.WindowCompat.getInsetsController(compose.activity.window,compose.activity.window.decorView)
            assertFalse(control.isAppearanceLightStatusBars);assertFalse(control.isAppearanceLightNavigationBars)
        }
        picture("dark_hold");navigate("home");fit("home");Thread.sleep(1200);picture("dark_home")''')
# The gate is a bounded task too, not a decorative scene that may push labels offscreen.
p=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design/TraceMotion.kt'
edit(p,'        Spacer(Modifier.height((LocalTaskHeight.current.value*.18f).coerceIn(12f,100f).dp))', '''        val compact=LocalTaskHeight.current<550.dp
        Spacer(Modifier.height(if(compact)12.dp else (LocalTaskHeight.current.value*.18f).coerceIn(12f,100f).dp))''')
edit(p,'.fillMaxWidth().height(168.dp)', '.fillMaxWidth().height(if(compact)104.dp else 168.dp)')
edit(p,'.padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically)', '.padding(vertical=if(compact)2.dp else 5.dp),verticalAlignment=Alignment.CenterVertically)')
p=r/'app/src/androidTest/kotlin/app/saeon/trace/StageTest.kt'
s=p.read_text().replace('import androidx.compose.ui.test.*','import androidx.compose.ui.test.*\nimport androidx.activity.compose.setContent\nimport androidx.compose.foundation.layout.*\nimport androidx.compose.ui.Modifier\nimport app.saeon.trace.ui.design.*')
s=s.replace('class StageTest:UiHarness(){','''class StageTest:UiHarness(){
    @Test fun checkingComponentFitsAvailableSpace(){
        fresh()
        compose.runOnUiThread {
            compose.activity.setContent {
                SaeonTheme {
                    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) { ContextWeave {} }
                }
            }
        }
        fit("trace_evaluating");picture("checking_layout")
    }
''')
p.write_text(s)
print('Final visual fixes: modal-safe system bars, fixed receipt close, relevant timeline and bounded checking scene.')
