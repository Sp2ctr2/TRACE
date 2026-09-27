from pathlib import Path
root=Path('android-native')
u=root/'app/src/main/kotlin/app/saeon/trace/ui'
def edit(p,old,new):
 s=p.read_text()
 if old not in s: raise RuntimeError(f'Missing finish anchor {p}: {old[:80]}')
 p.write_text(s.replace(old,new))
p=u/'screens/StudioScreens.kt'
edit(p,'            PersonBadge(tx.recipient.name,if(compact)38 else 48,false)\n            Space(if(compact)6 else 14)', '            if(!compact){ PersonBadge(tx.recipient.name,48,false);Space(14) }')
edit(p,'        Space(if(compact)14 else 28)','        Space(if(compact)10 else 28)')
edit(p,'            Rule();StudioDetail("출금 계좌","새온 생활통장","110-***-0001")','            Rule();StudioDetail("출금 계좌","새온 생활통장",if(compact)null else "110-***-0001")')
edit(p,'if(secondary==null)46.dp else 58.dp','if(LocalTaskDense.current){if(secondary==null)40.dp else 50.dp}else{if(secondary==null)46.dp else 58.dp}')
# Avoid a stale outgoing page in evidence; the gate remains under repository state control.
p=root/'app/src/androidTest/kotlin/app/saeon/trace/UiHarness.kt'
s=p.read_text()
start=s.index('fun capture(')
brace=s.index('{',start)
s=s[:brace+1]+'\n        compose.waitForIdle(); Thread.sleep(380)\n'+s[brace+1:]
p.write_text(s)
# Audit all primary task layouts, not only the first failure, with one final failing assertion.
p=root/'app/src/androidTest/kotlin/app/saeon/trace/StudioTest.kt'
edit(p,'class StudioTest:UiHarness() {','class StudioTest:UiHarness() {\n    private val overflowFailures=mutableListOf<String>()')
edit(p,'        assertEquals("Task $tag must fit at default type scale",0f,range,1f)','        if(range>1f) overflowFailures.add("$tag: $range px")')
edit(p,'navigate("home");capture("accessibility_02_easy_home",audit=false)','navigate("home");capture("accessibility_02_easy_home",audit=false)\n        assertTrue("Task overflow: ${overflowFailures.joinToString()}",overflowFailures.isEmpty())')
print('Finished compact review and settled-frame capture checks.')
