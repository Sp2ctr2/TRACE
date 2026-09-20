package app.saeon.trace

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.saeon.trace.core.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContextSafetyDeviceTest : UiHarness() {
    @Test fun reviewIsBoundToSubmittedDraftDespiteConcurrentDraftSaves() {
        fresh()
        val expected = TransferDraft(Fixtures.seoyeon, 128_000, Purpose.FAMILY)
        runBlocking {
            coroutineScope {
                repeat(12) { index -> launch(Dispatchers.IO) {
                    repository.setDraft(TransferDraft(Fixtures.kim, 1_000L + index, Purpose.OTHER))
                } }
                val reviewed = repository.review(expected)
                assertEquals(expected.amount, reviewed.current!!.intent.amount)
                assertEquals(expected.recipient, reviewed.current!!.intent.recipient)
                assertEquals(expected.purpose, reviewed.current!!.intent.purpose)
            }
        }
        assertEquals(expected.amount, state.current!!.intent.amount)
        assertEquals(expected.recipient, state.current!!.intent.recipient)
        assertEquals(Fixtures.START_BALANCE, state.balance)
    }
    @Test fun purposePickerCannotEraseRepaymentRequest() {
        fresh(DemoScenario.LOAN)
        tap("home_transfer"); tap("recipient_park", scroll = true)
        tap("transfer_purpose", scroll = true)
        compose.onNodeWithText("정산", substring = false).performScrollTo().performClick()
        tap("amount_next"); waitScreen("transfer_review")
        assertEquals(Purpose.SETTLEMENT, state.current!!.intent.purpose)
        confirmThroughUi("trace_verify")
        val previous = state.currentTransferId
        assertEquals(Fixtures.START_BALANCE, state.balance)
        tap("verify_route"); waitScreen("trace_official_route")
        tap("official_route_use"); waitScreen("transfer_review")
        assertNotEquals(previous, state.currentTransferId)
        assertEquals(Purpose.LOAN, state.current!!.intent.purpose)
        assertNull(state.current!!.authorization)
        assertTrue(state.receipts.none { !it.seed })
    }
    @Test fun newSharedContentCannotBeClearedByAnOlderAnalysis() {
        fresh()
        compose.runOnIdle {
            val model = compose.activity.safetyModel
            model.receive("경찰입니다. 지금 입금하세요.")
            model.analyze()
            model.receive("어제 저녁값 32,000원 보내줄래?")
        }
        waitScreen("shared_text_review")
        assertEquals("어제 저녁값 32,000원 보내줄래?", compose.activity.safetyModel.state.value.text)
        assertNull(compose.activity.safetyModel.state.value.result)
        assertFalse(compose.activity.safetyModel.state.value.busy)
        assertTrue(state.events.isEmpty())
        tap("share_consent"); waitScreen("manual_result")
        assertFalse(state.events.any { it.type == RiskType.IMPERSONATION })
    }
    @Test fun warnCheckExposesRealToggleState() {
        evaluated(DemoScenario.WARN)
        compose.onNodeWithTag("warn_check").performScrollTo().assertIsOff()
        tap("warn_check")
        compose.onNodeWithTag("warn_check").assertIsOn()
        assertEquals(Fixtures.START_BALANCE, state.balance)
        assertTrue(state.receipts.none { !it.seed })
    }
    @Test fun oversizedPastedAmountIsNotSilentlyReduced() {
        fresh(); tap("home_transfer"); tap("recipient_seoyeon", scroll = true)
        tap("amount_edit", scroll = true)
        compose.onNodeWithTag("amount_direct_input").performTextReplacement("1000000000")
        compose.onNodeWithTag("amount_input_done").assertIsNotEnabled()
        assertEquals(Fixtures.START_BALANCE, state.balance)
        compose.onNodeWithTag("amount_direct_input").performTextReplacement("0000320000")
        tap("amount_input_done"); tap("amount_next"); waitScreen("transfer_review")
        assertEquals(320_000L, state.current!!.intent.amount)
        assertTrue(state.receipts.none { !it.seed })
    }
    @Test fun cancellingHoldAndChoosingSavedRecipientCannotClearPressure() {
        evaluated(DemoScenario.IMPERSONATION)
        tap("hold_cancel"); waitScreen("home")
        tap("home_transfer"); tap("recipient_family", scroll = true)
        tap("amount_edit", scroll = true)
        compose.onNodeWithTag("amount_direct_input").performTextReplacement("32000")
        tap("amount_input_done"); tap("amount_next")
        confirmThroughUi("trace_hold")
        compose.onNodeWithText("저장된 수취인").assertExists()
        assertEquals(Fixtures.START_BALANCE, state.balance)
        assertTrue(state.receipts.none { !it.seed })
    }
}
