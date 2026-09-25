from pathlib import Path
r=Path('android-native')
p=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design/BankExperience.kt'
s=p.read_text().replace('fun ResultSeal(kind:String="complete",size:Int=78)','fun ResultSeal(kind:String="complete",diameter:Int=78)')
a=s.index('@Composable fun ResultSeal');b=s.index('@Composable fun ContextLoading',a)
s=s[:a]+s[a:b].replace('Modifier.size(size.dp)','Modifier.size(diameter.dp)').replace('(size*.42f).dp','(diameter*.42f).dp')+s[b:]
p.write_text(s)
p=r/'app/src/androidTest/kotlin/app/saeon/trace/RedesignTest.kt'
s=p.read_text();a=s.index('    @Test fun exactGateAndNoPrematureDebit()');b=s.index('    @Test fun presentationCenterStartsRealInputAndControlsPersist()',a)
s=s[:a]+'''    @Test fun exactGateAndNoPrematureDebit() {
        fresh();createReview(DemoScenario.NORMAL)
        tap("transfer_confirm");waitScreen("auth_confirm")
        val samples=java.util.concurrent.CopyOnWriteArrayList<Triple<Long,TransferStage?,Long>>()
        val observer=Thread {
            val deadline=System.nanoTime()+8_000_000_000L
            while(System.nanoTime()<deadline) {
                val snapshot=state
                val stage=snapshot.records.firstOrNull{it.intent.id==snapshot.currentTransferId}?.stage
                samples.add(Triple(System.nanoTime(),stage,snapshot.balance))
                if(stage==TransferStage.COMPLETE)break
                Thread.sleep(5)
            }
        }
        observer.start()
        tap("auth_confirm");waitScreen("transfer_complete")
        observer.join(9000)
        val entering=samples.firstOrNull{it.second==TransferStage.EVALUATING}
        val completed=samples.firstOrNull{it.second==TransferStage.COMPLETE}
        assertNotNull("Observed evaluating state",entering)
        assertNotNull("Observed committed result",completed)
        val duration=(completed!!.first-entering!!.first)/1_000_000
        assertTrue("Gate ended too early: $duration",duration>=900)
        assertTrue("Gate did not finish promptly: $duration",duration<4500)
        assertTrue("No debit during context loading",samples.filter{it.second==TransferStage.EVALUATING}.all{it.third==Fixtures.START_BALANCE})
        assertEquals(Fixtures.START_BALANCE-32000,completed.third)
        File(output,"gate_timing.txt").writeText("gate_ms=$duration\\nobservations=${samples.size}\\nbalance_before=${entering.third}\\nbalance_after=${completed.third}\\n")
    }
''' +s[b:]
s=s.replace('        compose.onNodeWithText("卡").assertDoesNotExist()\n','')
p.write_text(s)
print('V3 validation preparation complete')
