from pathlib import Path
import json,sys,runpy
root=Path(sys.argv[1] if len(sys.argv)>1 else 'android-native')
p=root/'trace-design/src/main/kotlin/dev/trace/scene/TraceScene.kt'
s=p.read_text()
s=s.replace('fun px(v:Float)=v/100f*width;fun py(v:Float)=v/100f*height','fun px(v:Float)=v/100f*width\n    fun py(v:Float)=v/100f*height')
s=s.replace('.coerceIn(0f,min(width,height)/2));path.addRoundRect(','.coerceIn(0f,min(width,height)/2))\n            path.addRoundRect(')
s=s.replace('.put("fit","contain").put("alt",d.name))}}else Canvas','.put("fit","contain").put("alt",d.name)))}}else Canvas')
p.write_text(s)
project=json.loads((root/'app/src/main/assets/trace/design.json').read_text())
out=root/'app/src/main/kotlin/trace/generated';out.mkdir(parents=True,exist_ok=True)
def quote(s):return '"'+s.replace('\\','\\\\').replace('"','\\"').replace('$','\\$').replace('\r','\\r').replace('\n','\\n').replace('\t','\\t')+'"'
for screen in project['screens']:
 name=screen['id'][0].upper()+screen['id'][1:]
 raw=json.dumps(screen,ensure_ascii=False,separators=(',',':'))
 chunks=[raw[i:i+10000] for i in range(0,len(raw),10000)]
 source='package trace.generated\n\nimport androidx.compose.runtime.*\nimport androidx.compose.ui.platform.LocalContext\nimport dev.trace.scene.*\n\n@Composable\nfun '+name+'Screen(data: SceneData, senior: Boolean = false, onAction: (String) -> Unit) {\n    val context = LocalContext.current\n    val design = remember(context) {\n        val pageJson = listOf(\n'+',\n'.join('            '+quote(c) for c in chunks)+'\n        ).joinToString("")\n        SceneDocument.fromAssets(context).withPage(pageJson)\n    }\n    TraceScene(design, '+quote(screen['id'])+', data, senior = senior, onAction = onAction)\n}\n'
 (out/(screen['id']+'Screen.kt')).write_text(source)
runpy.run_path(str(Path(__file__).with_name('hardening.py')),run_name='__main__')
p=root/'app/src/androidTest/kotlin/app/saeon/trace/SceneDeviceTest.kt';s=p.read_text();pos=s.rfind('}')
s=s[:pos]+'''    @Test fun appliedDesignPreservesBalancePrivacyAndRestoration() {
        fresh()
        runBlocking {
            BankDesignStore.get(context).apply(raw())
            graph.preferences.hideBalance(true)
        }
        compose.waitUntil(10_000) { compose.activity.model.preferences.value.hideBalance }
        navigate("home"); waitScreen("studio_native_home")
        compose.onNodeWithText("잔액 숨김").assertExists()
        val balance = state.balance
        val receipts = state.receipts
        compose.activityRule.scenario.recreate(); awaitReady(); waitScreen("studio_native_home")
        compose.onNodeWithText("잔액 숨김").assertExists()
        Assert.assertEquals(balance, state.balance)
        Assert.assertEquals(receipts, state.receipts)
        runBlocking { graph.preferences.hideBalance(false) }
    }
'''+s[pos:];p.write_text(s)
print('Native source and',len(project['screens']),'generated page specs installed with privacy regression')
