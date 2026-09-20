package app.saeon.trace

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.saeon.trace.core.*
import app.saeon.trace.data.ReadingMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoCenterTest : UiHarness() {
    @Test fun scenarioPreviewDoesNotMutateBankingState() {
        fresh(); tap("home_demo_center"); waitScreen("demo_center")
        val before = state
        tap("demo_IMPERSONATION", scroll = true); waitScreen("demo_preview")
        assertEquals(before, state)
        device.pressBack(); waitScreen("demo_center")
        assertEquals(before, state)
        tap("demo_IMPERSONATION", scroll = true); tap("demo_start")
        waitScreen("home"); assertEquals(DemoScenario.IMPERSONATION, state.scenario)
        assertTrue(state.receipts.none { !it.seed })
        assertEquals(Fixtures.START_BALANCE, state.balance)
    }
    @Test fun childModeKeepsHoldAndDoesNotImplyGuardianAuthorization() {
        evaluated(DemoScenario.IMPERSONATION)
        val binding = state.current!!.intent.binding
        runBlocking { graph.preferences.readingMode(ReadingMode.CHILD) }
        compose.waitUntil(10_000) { compose.activity.model.preferences.value.childMode }
        waitScreen("trace_hold")
        compose.onNodeWithText("지금은 돈을\n보내지 마세요.").assertExists()
        tap("hold_safe_action"); waitScreen("trace_safety_guide")
        repeat(2) { tap("safety_official_channel") }
        compose.onNodeWithText("이 앱이 어른에게 연락하거나 허락을 받은 것은 아니에요. 직접 함께 확인해 주세요.").assertExists()
        tap("safety_official_channel"); waitScreen("safety_center")
        assertEquals(binding, state.current!!.intent.binding)
        assertEquals(TransferStage.HOLD, state.current!!.stage)
        assertNull(state.current!!.authorization)
        assertTrue(state.receipts.none { !it.seed })
    }
    @Test fun readingModePersistsAcrossRecreationAndScenarioChanges() {
        fresh(); navigate("accessibility"); tap("mode_CHILD", scroll = true)
        compose.waitUntil(10_000) { compose.activity.model.preferences.value.readingMode == ReadingMode.CHILD }
        compose.activityRule.scenario.recreate(); awaitReady()
        assertEquals(ReadingMode.CHILD, compose.activity.model.preferences.value.readingMode)
        navigate("demo_center"); tap("demo_NORMAL", scroll = true); tap("demo_start"); waitScreen("home")
        assertEquals(ReadingMode.CHILD, compose.activity.model.preferences.value.readingMode)
        assertEquals(Fixtures.START_BALANCE, state.balance)
    }
    @Test fun modesNeverChangeTheLedgerOrReleaseHeldTransfers() {
        evaluated(DemoScenario.IMPERSONATION)
        val before = state
        for (mode in ReadingMode.entries) {
            runBlocking { graph.preferences.readingMode(mode) }
            compose.waitUntil(10_000) { compose.activity.model.preferences.value.readingMode == mode }
            assertEquals(before, state)
        }
    }
}
