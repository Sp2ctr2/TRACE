package app.saeon.trace

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.saeon.trace.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File

@RunWith(AndroidJUnit4::class)
class StageTest:UiHarness(){
    private fun unlock(){
        compose.runOnIdle{compose.activity.model.stageLock()}
        navigate("app_info");repeat(7){tap("app_version")};tap("stage_unlock_confirm");waitScreen("presenter")
    }
    private fun picture(name:String){Thread.sleep(450);capture(name,audit=false)}
    private fun fit(tag:String){
        waitScreen(tag);val nodes=compose.onAllNodesWithTag("${tag}_body",useUnmergedTree=true).fetchSemanticsNodes()
        assertTrue("Missing body: $tag",nodes.isNotEmpty())
        val max=nodes[0].config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.maxValue?.invoke()?:0f
        File(output,"fit.tsv").appendText("$tag\t${context.resources.configuration.screenWidthDp}\t$max\n")
        assertEquals("Unexpected scroll: $tag",0f,max,1f)
    }
    @Test fun hiddenModeAndMonotonicClock(){
        fresh();compose.runOnIdle{compose.activity.model.stageLock()};navigate("home")
        compose.onAllNodesWithTag("nav_presenter").assertCountEquals(0)
        navigate("more");compose.onAllNodesWithTag("demo_center_open").assertCountEquals(0)
        unlock();picture("presenter_full")
        tap("stage_tab_2");tap("stage_preset_demo");tap("stage_tab_0")
        assertEquals(55,compose.activity.model.stage.value.clock.budget)
        assertEquals(5,compose.activity.model.stage.value.selected)
        tap("stage_timer_toggle");Thread.sleep(1250)
        val before=compose.activity.model.stage.value.clock.elapsed(android.os.SystemClock.elapsedRealtime())
        assertTrue(before>=1000)
        compose.activityRule.scenario.recreate();awaitReady();navigate("presenter")
        assertTrue(compose.activity.model.stage.value.clock.elapsed(android.os.SystemClock.elapsedRealtime())>=before)
        tap("stage_timer_toggle");assertNull(compose.activity.model.stage.value.clock.started)
        tap("stage_tab_2");picture("presenter_preflight");tap("stage_lock",scroll=true);tap("stage_lock_confirm")
        waitScreen("home");assertFalse(compose.activity.model.stage.value.unlocked)
        compose.onAllNodesWithTag("nav_presenter").assertCountEquals(0)
    }
    @Test fun preparedNormalNeedsAuthenticationAndRealReceipt(){
        fresh();unlock();tap("stage_tab_2");tap("stage_preset_demo");tap("stage_tab_0");tap("stage_launch")
        fit("transfer_review");assertEquals(Fixtures.START_BALANCE,state.balance)
        assertEquals(TransferStage.REVIEW,state.current!!.stage)
        picture("review");confirmThroughUi("transfer_complete");fit("transfer_complete")
        assertEquals(Fixtures.START_BALANCE-32000,state.balance);picture("complete")
        tap("complete_receipt");waitScreen("receipt_expanded");picture("expanded_receipt");tap("receipt_collapse")
        assertEquals(1,compose.activity.model.stage.value.notes.count{it.outcome=="COMPLETE"})
        tap("complete_confirm");waitScreen("presenter")
    }
    @Test fun contextAndHoldRemainBoundToRealScenario(){
        fresh();unlock();compose.runOnIdle{compose.activity.model.stageSelect(6)};tap("stage_launch");waitScreen("stage_context");picture("context_story")
        tap("stage_context_next");confirmThroughUi("trace_hold");fit("trace_hold");picture("hold")
        assertEquals(Fixtures.START_BALANCE,state.balance);assertEquals(TransferStage.HOLD,state.current!!.stage)
        compose.onAllNodesWithTag("transfer_confirm").assertCountEquals(0)
        tap("hold_reasons_open");waitScreen("trace_hold_reason");tap("hold_reasons_close")
        runBlocking{graph.preferences.theme("dark");graph.preferences.transparency(true);graph.preferences.motion(true)}
        compose.waitUntil(10000){compose.activity.model.preferences.value.reducedTransparency}
        picture("dark_hold");navigate("home");fit("home");picture("dark_home")
    }
    @Test fun polishedHomeAmountAndAllPolicies(){
        fresh();compose.runOnIdle{compose.activity.model.stageLock()};fit("home");picture("home")
        navigate("transfer");fit("transfer_recipient")
        tap("recipient_seoyeon");fit("transfer_amount");tap("key_clear");tap("key_3");tap("key_2");repeat(3){tap("key_0")}
        picture("amount");tap("amount_next");confirmThroughUi("transfer_complete")
        assertEquals(Fixtures.START_BALANCE-32000,state.balance)
        for(scenario in listOf(DemoScenario.WARN,DemoScenario.LOAN,DemoScenario.UNKNOWN)){
            evaluated(scenario)
            if(scenario==DemoScenario.UNKNOWN){tap("verify_route");waitScreen("trace_unknown")}
            picture("policy_${scenario.name}")
            assertEquals(Fixtures.START_BALANCE,state.balance)
        }
    }
}
