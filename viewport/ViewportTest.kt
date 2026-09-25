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
class ViewportTest : UiHarness() {
    private fun proof(tag:String,name:String=tag,requireFit:Boolean=true) {
        waitScreen(tag);Thread.sleep(400);compose.waitForIdle()
        val nodes=compose.onAllNodesWithTag("${tag}_body",useUnmergedTree=true).fetchSemanticsNodes()
        val max=nodes.firstOrNull()?.config?.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.maxValue?.invoke() ?: 0f
        val dm=context.resources.displayMetrics
        File(output,"viewport_measurements.tsv").appendText("$name\t${dm.widthPixels}\t${dm.heightPixels}\t${dm.density}\t${context.resources.configuration.fontScale}\t$max\n")
        capture(name,audit=false)
        if(requireFit)assertEquals("$name has unnecessary vertical overflow",0f,max,1f)
    }
    @Test fun firstViewAndTaskScreensFit() {
        fresh();proof("home");compose.onNodeWithTag("home_transfer").assertIsDisplayed();compose.onNodeWithTag("glass_dock").assertIsDisplayed()
        navigate("transfer");proof("transfer_recipient")
        runBlocking{repository.setDraft(TransferDraft(Fixtures.recipient(DemoScenario.NORMAL),32000,Purpose.SETTLEMENT))}
        navigate("amount");proof("transfer_amount");compose.onNodeWithTag("amount_next").assertIsDisplayed();compose.onNodeWithTag("key_0").assertIsDisplayed()
        createReview(DemoScenario.NORMAL);proof("transfer_review");confirmThroughUi("transfer_complete");proof("transfer_complete")
        for(s in listOf(DemoScenario.IMPERSONATION,DemoScenario.WARN,DemoScenario.LOAN)) {
            evaluated(s)
            proof(when(s){DemoScenario.IMPERSONATION->"trace_hold";DemoScenario.WARN->"trace_warn";else->"trace_verify"})
            assertEquals(Fixtures.START_BALANCE,state.balance)
        }
        evaluated(DemoScenario.UNKNOWN);tap("verify_route");proof("trace_unknown");assertEquals(Fixtures.START_BALANCE,state.balance)
    }
    @Test fun sameTransactionDifferentContextAndDarkScreens() {
        fresh();navigate("comparison");proof("comparison",requireFit=false)
        tap("compare_normal");tap("scenario_begin");waitScreen("transfer_review")
        val normal=state.current!!.intent
        assertEquals(3000000,normal.amount);confirmThroughUi("transfer_complete")
        assertEquals(Fixtures.START_BALANCE-3000000,state.balance)
        navigate("comparison");tap("compare_risk");tap("scenario_begin");waitScreen("transfer_review")
        val risk=state.current!!.intent
        assertEquals(normal.recipient,risk.recipient);assertEquals(normal.amount,risk.amount)
        confirmThroughUi("trace_hold");proof("trace_hold","comparison_hold");assertEquals(Fixtures.START_BALANCE,state.balance)
        runBlocking{graph.preferences.theme("dark")};compose.waitUntil(10000){compose.activity.model.preferences.value.themeMode=="dark"}
        proof("trace_hold","dark_hold")
        navigate("home");proof("home","dark_home")
        createReview(DemoScenario.NORMAL);proof("transfer_review","dark_review")
        navigate("demo_lab");proof("demo_lab","dark_demo_lab",false)
    }
    @Test fun accessibilityRetainsReachableActions() {
        fresh(DemoScenario.IMPERSONATION);createReview(DemoScenario.IMPERSONATION)
        runBlocking{graph.preferences.easy(true);graph.preferences.motion(true)}
        confirmThroughUi("trace_hold");proof("trace_hold","easy_hold",false)
        compose.onNodeWithTag("hold_safe_action").assertIsDisplayed();compose.onNodeWithTag("hold_cancel").assertIsDisplayed()
        tap("hold_reasons_open",scroll=true);waitScreen("trace_hold_reason");tap("hold_reasons_close")
        assertEquals(Fixtures.START_BALANCE,state.balance)
    }
}
