from pathlib import Path
r=Path('android-native');c=r/'core/src/main/kotlin/app/saeon/trace/core';u=r/'app/src/main/kotlin/app/saeon/trace/ui';d=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design'
p=r/'app/src/androidTest/kotlin/app/saeon/trace/Bank7Test.kt';s=p.read_text().replace('official_new_transfer','official_route_use');s=s.replace('tap("card_lost")','tap("card_lost",scroll=true)').replace('tap("card_reissue")','tap("card_reissue",scroll=true)');s=s.replace('navigate("app_info");unlock();tap("demo_hide",scroll=true);tap("demo_hide_confirm")','navigate("presenter");waitScreen("demo_center");shot("functional_demo_before_hide");tap("demo_hide",scroll=true);shot("functional_demo_hide_dialog");tap("demo_hide_confirm")');p.write_text(s)
p=c/'BankEngine.kt';s=p.read_text();a='        requireBank(amount > 0 && amount <= state.savings,';assert a in s;s=s.replace(a,'        requireBank(!state.accountLocked, "ACCOUNT_LOCKED", "계정이 잠겨 있어요. 먼저 계정 보호를 확인해 주세요.")\n'+a);p.write_text(s)
p=u/'screens/MotionWarn.kt';s=p.read_text().replace('ViewportReview(state,record,interaction,model,open,back)','BankReview(state,record,interaction,model,open,back)');p.write_text(s)
p=u/'screens/ServiceScreens.kt';s=p.read_text().replace('SecondaryButton("재발급 요청 저장",Modifier.testTag("card_reissue"),enabled=status=="lost")','QuietButton("재발급 요청 저장",Modifier.fillMaxWidth().testTag("card_reissue"),enabled=status=="lost")');p.write_text(s)
p=u/'screens/BankVisuals.kt';s=p.read_text().replace('            Text("보낼까요?",style=MaterialTheme.typography.headlineSmall.copy','            if(!compact)Text("보낼까요?",style=MaterialTheme.typography.headlineSmall.copy').replace('Space(if(compact)18 else 28)','Space(if(compact)14 else 28)');p.write_text(s)
p=d/'Components.kt';s=p.read_text();a='Column(Modifier.widthIn(max = 600.dp).fillMaxSize()';assert a in s;s=s.replace(a,a+'\n            .padding(bottom = if (LocalPageBottomInset.current >= 100) 84.dp else 0.dp)');s=s.replace('bottom = LocalPageBottomInset.current.dp','bottom = 28.dp');p.write_text(s)
p=r/'app/src/main/kotlin/app/saeon/trace/MainActivity.kt';s=p.read_text();a='                decor.viewTreeObserver.addOnWindowFocusChangeListener(focus)';assert a in s;s=s.replace(a,'''                val preDraw=android.view.ViewTreeObserver.OnPreDrawListener {
                    if(decor.hasWindowFocus()) {
                        val control=androidx.core.view.WindowCompat.getInsetsController(window,decor)
                        if(control.isAppearanceLightStatusBars==dark)control.isAppearanceLightStatusBars=!dark
                        if(control.isAppearanceLightNavigationBars==dark)control.isAppearanceLightNavigationBars=!dark
                    }
                    true
                }
                decor.viewTreeObserver.addOnPreDrawListener(preDraw)
'''+a);s=s.replace('if(decor.viewTreeObserver.isAlive)decor.viewTreeObserver.removeOnWindowFocusChangeListener(focus)','''if(decor.viewTreeObserver.isAlive) {
                        decor.viewTreeObserver.removeOnWindowFocusChangeListener(focus)
                        decor.viewTreeObserver.removeOnPreDrawListener(preDraw)
                    }''');p.write_text(s)
import runpy
runpy.run_path('bank7/consistency.py',run_name='__main__')
print('Fixed compact review fit, navigation reservation, reissue binding, system contrast and cross-screen consistency.')
