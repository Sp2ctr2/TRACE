from pathlib import Path
root=Path('android-native')
def edit(p,old,new):
    s=p.read_text(); assert old in s, (str(p),old);p.write_text(s.replace(old,new))
p=root/'app/src/main/kotlin/app/saeon/trace/MainActivity.kt'
edit(p,'enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)','''enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }''')
p=root/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design/Theme.kt'
edit(p,'reduced: Boolean = false, content:', 'reduced: Boolean = false, palette: TracePalette? = null, content:')
edit(p,'LocalTracePalette provides if (dark) TracePalettes.Dark else TracePalettes.Light','LocalTracePalette provides (palette ?: if (dark) TracePalettes.Dark else TracePalettes.Light)')
edit(p,'scrim = TraceColors.Ink','scrim = Color.Black')
p=root/'app/src/main/kotlin/app/saeon/trace/ui/screens/SettingsScreens.kt'
edit(p,'Text((index + 1).toString().padStart(2, \'0\'), style = MaterialTheme.typography.labelMedium, color = TraceColors.Muted)','AppIcon(BankIcons.Info, size = 20, tint = TraceColors.Muted)')
edit(p,'기기에 저장된 시연 거래와 위험 신호, 설정을 지웁니다.','기기에 저장된 시연 거래와 위험 신호를 지웁니다. 화면 모드와 동작 설정은 유지합니다.')
p=root/'app/src/androidTest/kotlin/app/saeon/trace/ExperienceTest.kt'
edit(p,'evaluated(scenario)\n            runBlocking', '''evaluated(scenario)
            if (scenario == DemoScenario.UNKNOWN) {
                runBlocking { repository.resolveRoute(state.currentTransferId!!) }
                waitScreen("trace_unknown")
                assertEquals(TransferStage.UNKNOWN, state.records.first { it.intent.id == state.currentTransferId }.stage)
            }
            runBlocking''')
edit(p,'capture("experience_dark_appearance",audit=false)', '''compose.runOnIdle {
            val c = androidx.core.view.WindowCompat.getInsetsController(compose.activity.window, compose.activity.window.decorView)
            assertFalse("Dark status icons must be light", c.isAppearanceLightStatusBars)
            assertFalse("Dark navigation icons must be light", c.isAppearanceLightNavigationBars)
        }
        capture("experience_dark_appearance",audit=false)''')
edit(p,'capture("experience_light_${scenario.name}",audit=false)', '''compose.runOnIdle {
                assertTrue(androidx.core.view.WindowCompat.getInsetsController(compose.activity.window, compose.activity.window.decorView).isAppearanceLightStatusBars)
            }
            capture("experience_light_${scenario.name}",audit=false)''')
print('Applied visual inspection fixes and stronger UI assertions.')
