from pathlib import Path
import shutil, json, re
r=Path('android-native');u=r/'app/src/main/kotlin/app/saeon/trace/ui';d=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design';c=r/'core/src/main/kotlin/app/saeon/trace/core'
def edit(p,a,b):
 s=p.read_text();assert a in s,(str(p),a[:100]);p.write_text(s.replace(a,b))
for name,dest in [('BankVisuals.kt',u/'screens'),('ServiceScreens.kt',u/'screens'),('QuickDemo.kt',u),('BankMotion.kt',d)]:shutil.copy2(Path('bank7')/name,dest/name)
# Previous version UI has diverged from the test names. This revision has its own
# executed suite; historical tests remain source-only regression references.
if Path('bank7/Bank7Test.kt').exists():shutil.copy2('bank7/Bank7Test.kt',r/'app/src/androidTest/kotlin/app/saeon/trace/Bank7Test.kt')
if Path('bank7/Bank7CoreTest.kt').exists():shutil.copy2('bank7/Bank7CoreTest.kt',r/'core/src/test/kotlin/app/saeon/trace/core/Bank7CoreTest.kt')
edit(r/'app/build.gradle.kts','versionCode = 60','versionCode = 70')
edit(r/'app/build.gradle.kts','6.0.0-final-demo','7.0.0-banking-demo')
# Accurate neutral palette, the website coral remains unchanged. AA text colors
# are semantic channels, not approximated brand colors.
p=d/'Components.kt'
edit(p,'containerColor = TraceColors.Deep, contentColor = TraceColors.White','containerColor = TraceColors.Ink, contentColor = TraceColors.Paper')
edit(p,'copy(fontSize = 17.sp)','copy(fontSize = 16.sp)')
edit(p,'.heightIn(min = 60.dp)', '.heightIn(min = 52.dp)')
s=p.read_text().replace('padding(horizontal = 24.dp)','padding(horizontal = 20.dp)');p.write_text(s)
p=d/'Theme.kt';s=p.read_text().replace('titleLarge = style(24, FontWeight.Bold, 34)','titleLarge = style(22, FontWeight.SemiBold, 30)').replace('titleMedium = style(if (easy) 21 else 19, FontWeight.SemiBold, 29)','titleMedium = style(if (easy) 21 else 18, FontWeight.SemiBold, 26)');p.write_text(s)
# Compact home is not achieved by clipping text or shrinking hit areas.
p=u/'screens/BankVisuals.kt';s=p.read_text()
s=s.replace('vertical=if(compact)10.dp else 18.dp','vertical=if(compact)8.dp else 12.dp')
s=s.replace('Space(if(compact)8 else 16)','Space(if(compact)6 else 12)')
s=s.replace('Space(if(compact)6 else 14)','Space(if(compact)4 else 10)')
s=s.replace('min=if(compact)48.dp else 64.dp','min=if(compact)48.dp else 56.dp')
s=s.replace('Space(if(compact)0 else 6)','Space(0)')
s=s.replace('min=if(compact)48.dp else 58.dp','min=48.dp')
s=s.replace('if(!compact){Space(10);Rule();Space(6)}','if(!compact){Space(6);Rule();Space(0)}')
s=s.replace('if(!short){Space(6);BankRow("다가오는 일정",if(state.recurringEnabled)"통신비 68,000원 · 매월 25일"else"예약 알림이 없어요",BankIcons.Calendar,tag="home_scheduled"){open("schedules")}}',
'''if(!short){Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clickable(role=Role.Button){open("schedules")}.testTag("home_scheduled"),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){
                    AppIcon(BankIcons.Calendar,size=17,tint=TraceColors.Muted);Text(if(state.recurringEnabled)"통신비 68,000원 · 매월 25일"else"예약 알림 관리",Modifier.weight(1f),style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted);AppIcon(BankIcons.Chevron,size=14,tint=TraceColors.Muted)
                }}''')
p.write_text(s)
# The actual dot-orbit is in the reusable library; no connecting line renderer
# is used for transaction evaluation anymore.
p=d/'BankExperience.kt';s=p.read_text().replace('OrbitContextLoading(cancel)','BankContextLoading(cancel)').replace('ContextWeave(cancel)','BankContextLoading(cancel)');p.write_text(s)
# Replace routes with the new banking presentation and complete service inventory.
p=u/'SaeonApp.kt';s=p.read_text()
s=s.replace('FinalHome(state, preferences, model, open)','BankHome(state, preferences, model, open)')
s=s.replace('AssetsScreen(state, open)','BankAssets(state, open)')
s=s.replace('MoreScreen(preferences, open)','BankMore(preferences, open)')
s=s.replace('DemoCenterScreen(model,open)','QuickDemoCenter(model,open)')
s=s.replace('motionScreen("recurring") { RecurringScreen(state, model, back) }','motionScreen("recurring") { SchedulesScreen(state, model, open, back) }')
anchor='                        motionScreen("support")'
assert anchor in s
routes='''                        motionScreen("documents") { DocumentsScreen(state,open,back) }
                        motionScreen("document/{id}") { target -> DocumentScreen(target.arguments?.getString("id").orEmpty(),state,back) }
                        motionScreen("schedules") { SchedulesScreen(state,model,open,back) }
                        motionScreen("card_service") { CardServiceScreen(preferences,model,back) }
                        motionScreen("account_lock") { AccountLockScreen(state,model,back) }
                        motionScreen("spending") { SpendingScreen(back) }
                        motionScreen("goals") { GoalsScreen(state,model,back) }
                        motionScreen("investments") { NotesScreen("investments",back) }
                        motionScreen("financial_notes") { NotesScreen("financial_notes",back) }
                        motionScreen("support_request") { SupportRequestsScreen(back) }
                        motionScreen("report") { ReportScreen(open,back) }
                        motionScreen("terms") { TermsScreen(back) }
'''
s=s.replace(anchor,routes+anchor)
# No timer or idle-loop controller remains mounted in the normal/demo UI.
s=s.replace('    val view=LocalView.current\n    DisposableEffect(stage.clock.started){view.keepScreenOn=stage.clock.started!=null;onDispose{view.keepScreenOn=false}}\n','')
p.write_text(s)
for name in ['TransferScreens.kt','MotionWarn.kt']:
 p=u/'screens'/name;s=p.read_text().replace('FinalReview(state, record, interaction, model, open, back)','BankReview(state, record, interaction, model, open, back)').replace('ViewportReview(state,record,interaction,model,open,back)','BankReview(state,record,interaction,model,open,back)');p.write_text(s)
p=u/'screens/SettingsScreens.kt';s=p.read_text()
s=s.replace('MenuRow("도움말 보기"','MenuRow("문의 남기기", "기기에 저장되는 시연 문의", BankIcons.Message, tag="support_request_open") { open("support_request") }\n        MenuRow("도움말 보기"')
# Append actual support entry at a stable location if the older wording differed.
a=s.index('@Composable fun SupportScreen');b=s.index('@Composable fun HelpScreen',a)
part=s[a:b]
if 'support_request_open' not in part:
 part=part.replace('Space(16); Headline', 'BankRow("문의 남기기", "기기에 저장되는 시연 문의", BankIcons.Message, tag="support_request_open") { open("support_request") }\n        Space(16); Headline')
 if 'support_request_open' not in part:
  opening=part.index(' {',part.index('Page('))+2
  part=part[:opening]+'\n        BankRow("문의 남기기", "기기에 저장되는 시연 문의", BankIcons.Message, tag="support_request_open") { open("support_request") }\n'+part[opening:]
 s=s[:a]+part+s[b:]
p.write_text(s)
# Account lock is a persisted bank state, never only a switch in the UI.
edit(c/'Models.kt','val recurringEnabled: Boolean = true','val recurringEnabled: Boolean = true,\n    val accountLocked: Boolean = false')
edit(c/'BankEngine.kt','        requireBank(amount > 0,','        requireBank(!state.accountLocked, "ACCOUNT_LOCKED", "송금이 잠겨 있어요. 계정 보호에서 잠금을 확인해 주세요. 돈은 나가지 않았습니다.")\n        requireBank(amount > 0,')
p=r/'app/src/main/kotlin/app/saeon/trace/data/SnapshotCodec.kt'
edit(p,'"recurringEnabled" to state.recurringEnabled','"recurringEnabled" to state.recurringEnabled, "accountLocked" to state.accountLocked')
edit(p,'value.getBoolean("recurringEnabled")','value.getBoolean("recurringEnabled"), value.optBoolean("accountLocked", false)')
p=r/'app/src/main/kotlin/app/saeon/trace/data/BankRepository.kt'
edit(p,'    suspend fun setLimit(limit: Long)', '''    suspend fun depositToSavings(amount:Long,requestId:String) = change { state,now ->
        val id="own-saving-$requestId"
        if(state.receipts.any{it.intentId==id})state else {
            BankEngine.validateAmount(state,amount,Purpose.GENERAL,now)
            val recipient=Recipient("own-savings","모아적금","새온은행","210-***-4401",true,RecipientKind.INSTITUTION)
            val receipt=TransferReceipt("SIM-$requestId",id,recipient,amount,now,Purpose.OTHER,state.balance-amount,requestId,memo="내 계좌로 모으기")
            state.copy(balance=state.balance-amount,savings=state.savings+amount,receipts=listOf(receipt)+state.receipts)
        }
    }
    suspend fun setLimit(limit: Long)''')
# Opening overlay shares the same palette and original mark. No network wait,
# fake account values, or per-navigation splash replay.
p=r/'app/src/main/kotlin/app/saeon/trace/MainActivity.kt';s=p.read_text()
s=s.replace('            SaeonTheme(preferences.easyMode, dark, noMotion) {','            val ready by model.bank.collectAsStateWithLifecycle()\n            val fatal by model.fatal.collectAsStateWithLifecycle()\n            SaeonTheme(preferences.easyMode, dark, noMotion) {\n                androidx.compose.foundation.layout.Box {')
s=s.replace('onStopVoice = ::stopVoice, voiceActive = voiceActive)\n            }','onStopVoice = ::stopVoice, voiceActive = voiceActive)\n                BankEntrance(ready != null || fatal != null)\n                }\n            }')
p.write_text(s)
# Platform adaptive launcher icon: use both actual website paths; the lower-left
# point is rectangular in the source, not an invented circular logo.
res=r/'app/src/main/res'
(res/'mipmap-anydpi-v26').mkdir(exist_ok=True);(res/'mipmap-anydpi-v33').mkdir(exist_ok=True)
foreground='''<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108"><group android:scaleX="1.8" android:scaleY="1.8" android:translateX="18" android:translateY="16"><path android:fillColor="#EF4A32" android:pathData="M6 9h28v7H23.5v17h-7V16H6z"/><path android:fillColor="#EF4A32" android:pathData="M6 23h7v10H6z"/></group></vector>'''
(res/'drawable/trace_icon_foreground.xml').write_text(foreground)
(res/'values/icon_colors.xml').write_text('<resources><color name="trace_icon_background">#F5F4F0</color></resources>')
base='<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"><background android:drawable="@color/trace_icon_background"/><foreground android:drawable="@drawable/trace_icon_foreground"/>{}</adaptive-icon>'
(res/'mipmap-anydpi-v26/ic_launcher.xml').write_text(base.format(''))
(res/'mipmap-anydpi-v33/ic_launcher.xml').write_text(base.format('<monochrome android:drawable="@drawable/trace_icon_foreground"/>'))
p=r/'app/src/main/AndroidManifest.xml';s=p.read_text().replace('@drawable/ic_launcher','@mipmap/ic_launcher')
s=s.replace('        <activity','''        <provider android:name="androidx.core.content.FileProvider" android:authorities="${applicationId}.documents" android:exported="false" android:grantUriPermissions="true"><meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/document_paths"/></provider>
        <activity''');p.write_text(s)
(res/'xml/document_paths.xml').write_text('<paths xmlns:android="http://schemas.android.com/apk/res/android"><cache-path name="documents" path="documents/"/></paths>')
# Export original token evidence (not a hand-picked recolor).
Path('delivery').mkdir(exist_ok=True)
Path('delivery/brand-tokens.json').write_text(json.dumps({'source':'Sp2ctr2/TRACE/index.html :root and #trace-mark','paper':'#F5F4F0','surface':'#FBFAF7','ink':'#20211F','coral':'#EF4A32','coralDeep':'#D93B25','coralWash':'#FAE7DF','divider':'#DDDDD5','night':'#202321','nightDeep':'#171B19','logoPaths':['M6 9h28v7H23.5v17h-7V16H6z','M6 23h7v10H6z']},ensure_ascii=False,indent=2))
print('Bank 7 integrated: native banking flows, scoped services, explicit simulation boundary, no WebView.')
