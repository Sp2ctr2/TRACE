from pathlib import Path
r=Path('android-native')
for p in r.rglob('*.kt'):
    if 'build' in p.parts: continue
    lines=p.read_text().splitlines();seen=set();out=[]
    for line in lines:
        if line.startswith('import '):
            if line in seen:continue
            seen.add(line)
        out.append(line)
    p.write_text('\n'.join(out)+'\n')
p=r/'app/src/main/kotlin/app/saeon/trace/ui/screens/SettingsScreens.kt'
s=p.read_text().replace('fun MoreScreen(open:', 'fun MoreScreen(preferences: BankPreferences, open:').replace('MenuRow("한지우님",','MenuRow("${preferences.displayName}님",')
p.write_text(s)
p=r/'app/src/main/kotlin/app/saeon/trace/ui/SaeonApp.kt'
s=p.read_text().replace('MoreScreen(open)','MoreScreen(preferences, open)');p.write_text(s)
p=r/'app/src/main/kotlin/app/saeon/trace/ui/screens/TransferScreens.kt'
s=p.read_text().replace('val reasons = listOf(RiskType.IMPERSONATION, RiskType.URGENCY,','val reasons = listOf(RiskType.REMOTE_ACCESS, RiskType.FAMILY_CLAIM, RiskType.PROFIT_PROMISE, RiskType.SECRECY, RiskType.IMPERSONATION, RiskType.URGENCY,')
s=s.replace('상대가 기관을 사칭했을 가능성이 있습니다.','앞선 요청과 지금의 송금에서 위험 정황이 이어졌습니다.')
s=s.replace('Space(8); TraceSignature(); Space(24)','Space(6); TraceSignature(); Space(18)')
s=s.replace('Space(8); Body("앞선 외부 요청과 지금의 송금이 연결돼, 잠시 멈췄어요.", subdued = true)','Space(8); Body("방금 전의 요청이 이 송금과 이어져 있어요.", subdued = true)')
s=s.replace('Caption(if (record.intent.recipient.known) "저장된 수취인" else "처음 보내는 계좌"); Space(8); Money(record.intent.amount, hero = false)\n                Space(6); Text("${record.intent.recipient.name}님에게", style = MaterialTheme.typography.titleSmall)\n                Space(6); Caption("${record.intent.recipient.bank} · ${record.intent.recipient.account}")','''Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    PersonBadge(record.intent.recipient.name,44,true)
                    Column(Modifier.weight(1f)) {
                        Text("${record.intent.recipient.name}님에게",style=MaterialTheme.typography.titleSmall)
                        Caption(if(record.intent.recipient.known) "저장된 수취인" else "처음 보내는 계좌")
                    }
                }
                Space(14);AmountDisplay(record.intent.amount)''')
p.write_text(s)
p=r/'app/src/androidTest/kotlin/app/saeon/trace/RedesignTest.kt'
s=p.read_text();pos=s.rfind('\n}')
s=s[:pos]+'''
    @Test fun officialRouteWarningAndUnknownAreNotShortcuts() {
        evaluated(DemoScenario.LOAN)
        val original=state.currentTransferId
        val balance=state.balance
        tap("verify_route");waitScreen("trace_official_route")
        capture("v3_official_route",audit=false)
        tap("official_route_use");waitScreen("transfer_review")
        assertNotEquals(original,state.currentTransferId)
        assertEquals(balance,state.balance)
        confirmThroughUi("transfer_complete")
        assertEquals(balance-8_000_000,state.balance)
        assertEquals(0,state.loanBalance)
        evaluated(DemoScenario.WARN)
        compose.onNodeWithTag("warn_acknowledge").assertIsNotEnabled()
        tap("warn_check",scroll=true);tap("warn_acknowledge")
        waitScreen("transfer_review")
        assertEquals(Fixtures.START_BALANCE,state.balance)
        confirmThroughUi("transfer_complete")
        assertEquals(Fixtures.START_BALANCE-Fixtures.amount(DemoScenario.WARN),state.balance)
        evaluated(DemoScenario.UNKNOWN)
        tap("verify_route");waitScreen("trace_unknown")
        assertEquals(Fixtures.START_BALANCE,state.balance)
        capture("v3_unknown",audit=false)
        tap("unknown_retry");waitScreen("trace_unknown")
        assertEquals(Fixtures.START_BALANCE,state.balance)
    }
    @Test fun compactAndLargeLayoutsRemainNavigable() {
        fresh()
        try {
            device.executeShellCommand("wm size 640x1136")
            device.executeShellCommand("wm density 320")
            Thread.sleep(900);navigate("home")
            compose.onNodeWithTag("glass_dock").assertIsDisplayed()
            compose.onNodeWithTag("home_transfer").assertIsDisplayed()
            capture("v3_320_home",audit=false)
            createReview(DemoScenario.NORMAL)
            compose.onNodeWithTag("transfer_confirm").assertIsDisplayed()
            capture("v3_320_review",audit=false)
            confirmThroughUi("transfer_complete")
            compose.onNodeWithTag("complete_confirm").assertIsDisplayed()
            capture("v3_320_complete",audit=false)
            fresh(DemoScenario.EASY);createReview(DemoScenario.EASY)
            confirmThroughUi("trace_hold")
            compose.onNodeWithTag("hold_safe_action").assertIsDisplayed()
            capture("v3_320_easy_hold",audit=false)
        } finally {
            device.executeShellCommand("wm size reset")
            device.executeShellCommand("wm density reset")
        }
    }
''' +s[pos:]
p.write_text(s)
print('Kotlin imports, evidence explanations, profile and extended flow verification prepared')
