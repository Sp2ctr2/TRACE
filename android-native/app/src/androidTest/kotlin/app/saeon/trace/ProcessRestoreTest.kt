package app.saeon.trace

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.saeon.trace.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeedHoldProcessTest : UiHarness() {
    @Test fun saveHoldBeforeProcessTermination() {
        evaluated(DemoScenario.IMPERSONATION)
        assertEquals(TransferStage.HOLD, state.current!!.stage)
        capture("before_process_death")
    }
}
@RunWith(AndroidJUnit4::class)
class RestoreHoldProcessTest : UiHarness() {
    @Test fun processRestartPreservesHoldAndCannotCommit() {
        awaitReady()
        assertEquals(TransferStage.HOLD, state.current!!.stage)
        assertEquals(Fixtures.START_BALANCE, state.balance)
        assertTrue(state.receipts.none { !it.seed })
        navigate("transfer_state"); waitScreen("trace_hold")
        try { runBlocking { repository.prepare(state.currentTransferId!!) }; fail("Restored HOLD authorized") }
        catch (error: BankFailure) { assertEquals("STATE_NOT_REVIEW", error.code) }
        capture("after_process_death")
    }
}
