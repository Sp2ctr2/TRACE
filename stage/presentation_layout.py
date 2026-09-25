from pathlib import Path
r=Path('android-native');u=r/'app/src/main/kotlin/app/saeon/trace/ui';d=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design'
def edit(p,a,b):
 s=p.read_text();assert a in s,(str(p),a[:120]);p.write_text(s.replace(a,b))
p=u/'Presenter.kt';s=p.read_text()
s=s.replace('    var tab by rememberSaveable','    val compact=androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp<650\n    var tab by rememberSaveable',1)
s=s.replace('LaunchedEffect(stage.clock.started){while(true){now=SystemClock.elapsedRealtime();delay(250)}}','LaunchedEffect(stage.clock){now=SystemClock.elapsedRealtime();while(stage.clock.started!=null){delay(500);now=SystemClock.elapsedRealtime()}}')
a=s.index('            if(selected.action!=null)PrimaryButton');b=s.index('\n            TaskGap(8);Caption',a)
action=s[a:b]
s=s[:a]+s[b:]
anchor='    TaskPage("발표자 모드","presenter",root=true,actions={IconAction(BankIcons.More,"전체 메뉴"){open("more")}}) {'
assert anchor in s
s=s.replace(anchor,'    TaskPage("발표자 모드","presenter",root=true,actions={IconAction(BankIcons.More,"전체 메뉴"){open("more")}},footer={\n        if(tab==0){\n'+action+'\n        }\n    }) {')
s=s.replace('FilterChip(tab==i,{tab=i},label={Text(label)},modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("stage_tab_$i"))','''TextButton(onClick={tab=i},modifier=Modifier.weight(1f).heightIn(min=48.dp).background(if(tab==i)TraceColors.Surface else androidx.compose.ui.graphics.Color.Transparent,RoundedCornerShape(14.dp)).testTag("stage_tab_$i"),colors=ButtonDefaults.textButtonColors(contentColor=if(tab==i)TraceColors.Ink else TraceColors.Muted)) { Text(label,style=MaterialTheme.typography.labelLarge) }''')
s=s.replace('            TaskGap(12)\n            Row(Modifier.fillMaxWidth()', '            Space(if(compact)0 else 12)\n            Row(Modifier.fillMaxWidth()',1)
s=s.replace('                    Caption(if(stage.clock.budget==180)', '                    if(!compact)Caption(if(stage.clock.budget==180)')
s=s.replace('fontSize=42.sp','fontSize=if(compact)32.sp else 42.sp')
s=s.replace('            TaskGap(12)\n            Caption("시간표 기준: ${current.title}")\n            TaskGap(14)','            Space(if(compact)6 else 12)\n            if(!compact)Caption("시간표 기준: ${current.title}")\n            if(!compact)TaskGap(14)')
s=s.replace('            SurfaceBox {\n                Caption("${stageTime(selected.start)}–${stageTime(selected.end)}")\n                Text(selected.title,style=MaterialTheme.typography.headlineSmall)\n                TaskGap(8);Body(selected.line,subdued=true)\n            }', '''            Column(Modifier.fillMaxWidth().background(TraceColors.Surface,RoundedCornerShape(18.dp)).padding(if(compact)12.dp else 20.dp)) {
                Caption("${stageTime(selected.start)}–${stageTime(selected.end)}")
                Text(selected.title,style=if(compact)MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall)
                Space(if(compact)4 else 8)
                Text(selected.line,style=if(compact)MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,color=TraceColors.Muted)
            }''')
s=s.replace('            TaskGap(8);Caption("장면은 수동으로 넘깁니다. 시간이 지나도 송금하지 않아요.")','            if(!compact){TaskGap(8);Caption("장면은 수동으로 넘깁니다. 시간이 지나도 송금하지 않아요.")}')
s=s.replace('model.stageScenario(DemoScenario.IMPERSONATION){open("transfer_state")}}}) {','model.stageSelect(7);model.stageScenario(DemoScenario.IMPERSONATION){open("transfer_state")}}}) {')
p.write_text(s)
# Pass the user's material preference to the component that draws the glass.
p=d/'BankExperience.kt'
edit(p,'modifier:Modifier=Modifier,onSelect:', 'modifier:Modifier=Modifier,reduceTransparency:Boolean=LocalReducedTransparency.current,onSelect:')
edit(p,'val solid=LocalReducedTransparency.current','val solid=reduceTransparency')
edit(p,'spring(dampingRatio=.86f,stiffness=500f)','spring(dampingRatio=1f,stiffness=520f)')
p=u/'SaeonApp.kt'
edit(p,'GlassDock(tabs,route,backdrop,contentOrigin,Modifier.align(Alignment.BottomCenter).padding(horizontal=18.dp,vertical=10.dp))', 'GlassDock(tabs,if(stage.unlocked&&route=="more")"presenter"else route,backdrop,contentOrigin,Modifier.align(Alignment.BottomCenter).padding(horizontal=18.dp,vertical=10.dp),reduceTransparency=preferences.reducedTransparency)')
s=p.read_text();s=s.replace('popEnterTransition = { fadeIn(tween(if(reduced)0 else TraceMotion.Spatial))+slideInHorizontally(tween(if(reduced)0 else TraceMotion.Spatial,easing=TraceMotion.Enter)){if(reduced)0 else 24}', 'popEnterTransition = { fadeIn(tween(if(reduced)0 else TraceMotion.Spatial))+slideInHorizontally(tween(if(reduced)0 else TraceMotion.Spatial,easing=TraceMotion.Enter)){if(reduced)0 else -24}')
s=s.replace('}.getOrDefault(DemoScenario.NORMAL), model, open, back) }','}.getOrDefault(DemoScenario.NORMAL), model, open, back)else StageLocked(back) }')
p.write_text(s)
p=u/'screens/ViewportScreens.kt';s=p.read_text()
s='\n'.join(line for line in s.splitlines() if 'if(false)QuietButton' not in line)+'\n'
s=s.replace('Text(person.name,style=MaterialTheme.typography.titleSmall)','Text(person.name,Modifier.traceShared("payee/${person.id}"),style=MaterialTheme.typography.titleSmall)')
p.write_text(s)
# Test system windows as well as Compose semantics. Only the separately identified
# emulator launcher may be stopped; never suppress a bank crash or bank ANR.
p=r/'app/src/androidTest/kotlin/app/saeon/trace/StageTest.kt'
s=p.read_text().replace('    private fun picture(name:String){Thread.sleep(450);capture(name,audit=false)}', '''    @org.junit.Before fun dismissEmulatorLauncherOnly(){
        val dev=androidx.test.uiautomator.UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation())
        dev.executeShellCommand("am force-stop com.google.android.apps.nexuslauncher")
        Thread.sleep(400)
    }
    private fun picture(name:String){
        Thread.sleep(450)
        val dev=androidx.test.uiautomator.UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation())
        assertFalse("System ANR dialog must not contaminate evidence",dev.hasObject(androidx.test.uiautomator.By.textContains("isn't responding")))
        capture(name,audit=false)
    }''')
s=s.replace('tap("stage_tab_0");tap("stage_launch")', 'tap("stage_tab_0");compose.onNodeWithTag("stage_launch").assertIsDisplayed();tap("stage_launch")')
p.write_text(s)
print('Fixed compact stage action placement, glass preference scope, backward navigation and system-window evidence validation.')
