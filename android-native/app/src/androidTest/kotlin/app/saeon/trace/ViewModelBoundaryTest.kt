package app.saeon.trace

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.saeon.trace.core.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ViewModelBoundaryTest : UiHarness() {
    @Test fun submittedFormIsBoundAtomicallyDespiteStaleAutosaves() {
        fresh()
        val submitted = TransferDraft(Fixtures.seoyeon, 32_000, Purpose.SETTLEMENT)
        val stale = TransferDraft(Fixtures.family, 99_000, Purpose.FAMILY)
        runBlocking { repository.setDraft(stale) }
        compose.runOnIdle { compose.activity.model.review(submitted) {} }
        compose.waitUntil(10_000) { state.current != null && !compose.activity.model.interaction.value.busy }
        val binding = state.current!!.intent.binding
        runBlocking { repository.setDraft(stale) }
        assertEquals(submitted.amount, state.current!!.intent.amount)
        assertEquals(submitted.recipient, state.current!!.intent.recipient)
        assertEquals(submitted.purpose, state.current!!.intent.purpose)
        assertEquals(binding, state.current!!.intent.binding)
        assertNull(state.current!!.authorization)
        assertTrue(state.receipts.none { !it.seed })
    }
    @Test fun newShareCannotReceiveThePreviousAnalysisResult() {
        fresh()
        blockedLedger {
            compose.runOnIdle {
                compose.activity.safetyModel.receive("검찰입니다. 지금 바로 이체하세요.")
                compose.activity.safetyModel.analyze()
            }
            compose.waitUntil(10_000) { compose.activity.safetyModel.state.value.busy }
            compose.runOnIdle { compose.activity.safetyModel.receive("새로 받은 내용은 아직 확인하지 않았습니다.") }
        }
        val pending = compose.activity.safetyModel.state.value
        assertEquals("새로 받은 내용은 아직 확인하지 않았습니다.", pending.text)
        assertTrue(pending.shared)
        assertNull(pending.result)
        assertFalse(pending.busy)
        assertTrue(state.events.isEmpty())
    }
    @Test fun cancelledPendingAnalysisDoesNotResurfaceAfterLeaving() {
        fresh()
        blockedLedger {
            compose.runOnIdle {
                compose.activity.safetyModel.edit("경찰입니다. 지금 입금하세요.")
                compose.activity.safetyModel.analyze()
            }
            compose.waitUntil(10_000) { compose.activity.safetyModel.state.value.busy }
            compose.runOnIdle { compose.activity.safetyModel.clear() }
        }
        val cleared = compose.activity.safetyModel.state.value
        assertEquals("", cleared.text)
        assertNull(cleared.result)
        assertFalse(cleared.busy)
        assertTrue(state.events.isEmpty())
    }
    @Test fun sharedPreviewSurvivesRecreationWithoutLosingLaunchIdentity() {
        fresh()
        val launchAction = compose.activity.intent.action
        val launchCategories = compose.activity.intent.categories?.toSet()
        share("경찰입니다. 지금 이체하세요.")
        val preview = compose.activity.safetyModel.state.value
        assertEquals(launchAction, compose.activity.intent.action)
        assertEquals(launchCategories, compose.activity.intent.categories?.toSet())
        assertNull(compose.activity.intent.getStringExtra(android.content.Intent.EXTRA_TEXT))
        compose.activityRule.scenario.recreate()
        awaitReady(); waitScreen("shared_text_review")
        assertEquals(preview, compose.activity.safetyModel.state.value)
        assertTrue(state.events.isEmpty())
    }
    private fun blockedLedger(action: () -> Unit) {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val blocker = scope.async {
            repository.change { current, _ ->
                entered.countDown()
                check(release.await(15, TimeUnit.SECONDS)) { "Test ledger barrier timed out" }
                current
            }
        }
        try {
            assertTrue("Ledger barrier did not start", entered.await(10, TimeUnit.SECONDS))
            action()
        } finally {
            release.countDown()
            runBlocking { blocker.await(); repository.change { current, _ -> current } }
            scope.cancel()
        }
        compose.waitForIdle()
    }
}
