package app.saeon.trace

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.saeon.trace.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BankUiFlowTest : UiHarness() {
    @Test fun normalTransferThroughRealControls() {
        fresh()
        tap("home_transfer")
        tap("recipient_seoyeon", scroll = true)
        waitScreen("transfer_amount")
        tap("amount_next")
        waitScreen("transfer_review")
        confirmThroughUi("transfer_complete")
        assertEquals(12_808_000L, state.balance)
        assertEquals(1, state.receipts.count { !it.seed })
        tap("complete_confirm")
        waitScreen("home")
    }
    @Test fun holdFromDemoLabHasNoBypassAndSurvivesRotation() {
        fresh()
        navigate("app_info")
        repeat(5) { tap("app_version", scroll = true) }
        waitScreen("demo_lab")
        tap("demo_IMPERSONATION", scroll = true)
        tap("demo_start")
        waitScreen("home")
        tap("home_transfer")
        tap("recipient_kim", scroll = true)
        tap("amount_next")
        confirmThroughUi("trace_hold")
        assertEquals(Fixtures.START_BALANCE, state.balance)
        assertTrue(state.receipts.none { !it.seed })
        listOf("그래도 계속", "무시하고 송금", "위험 감수").forEach { text ->
            compose.onAllNodesWithText(text, substring = true).assertCountEquals(0)
        }
        val id = state.currentTransferId
        compose.activityRule.scenario.recreate()
        awaitReady(); waitScreen("trace_hold")
        assertEquals(id, state.currentTransferId)
        assertEquals(TransferStage.HOLD, state.current!!.stage)
        assertEquals(Fixtures.START_BALANCE, state.balance)
        capture("rotation_restored_hold")
    }
    @Test fun warningRequiresReviewAndFreshAuthorization() {
        evaluated(DemoScenario.WARN)
        waitScreen("trace_warn")
        compose.onNodeWithTag("warn_acknowledge").assertIsNotEnabled()
        tap("warn_check", scroll = true)
        tap("warn_acknowledge")
        waitScreen("transfer_review")
        assertNull(state.current!!.authorization)
        assertTrue(state.receipts.none { !it.seed })
        confirmThroughUi("transfer_complete")
        assertEquals(12_720_000L, state.balance)
    }
    @Test fun loanRouteRequiresANewTransactionAndAuthentication() {
        evaluated(DemoScenario.LOAN)
        waitScreen("trace_verify")
        val oldId = state.currentTransferId!!
        tap("verify_route")
        waitScreen("trace_official_route")
        assertEquals(Fixtures.START_BALANCE, state.balance)
        tap("official_route_use")
        waitScreen("transfer_review")
        assertNotEquals(oldId, state.currentTransferId)
        assertEquals(oldId, state.current!!.intent.originIntentId)
        assertNull(state.current!!.authorization)
        assertTrue(state.receipts.none { !it.seed })
        confirmThroughUi("transfer_complete")
        assertEquals(4_840_000L, state.balance)
        assertEquals(0L, state.loanBalance)
    }
    @Test fun unavailableOfficialRouteCannotCreateReceipt() {
        evaluated(DemoScenario.UNKNOWN)
        tap("verify_route")
        waitScreen("trace_unknown")
        tap("unknown_retry")
        compose.waitUntil(10_000) { !compose.activity.model.interaction.value.busy }
        waitScreen("trace_unknown")
        assertEquals(Fixtures.START_BALANCE, state.balance)
        assertTrue(state.receipts.none { !it.seed })
    }
    @Test fun sharedTextNeedsConsentAndRawTextIsNotPersisted() {
        fresh()
        val secret = "시연검증전용문구_AB91"
        share("경찰입니다. 지금 http://example.invalid 로 확인하고 입금하세요. $secret")
        assertTrue(state.events.isEmpty())
        assertNull(compose.activity.intent.getStringExtra(android.content.Intent.EXTRA_TEXT))
        tap("share_consent")
        waitScreen("manual_result")
        assertEquals(4, state.events.size)
        assertEquals("", compose.activity.safetyModel.state.value.text)
        val row = runBlocking { graph.database.snapshots().read()!! }
        assertFalse(row.payload.contains(secret))
        assertFalse(row.payload.contains("http://example.invalid"))
        assertTrue(state.receipts.none { !it.seed })
    }
    @Test fun cancellingSharedTextDoesNotAnalyzeIt() {
        fresh()
        share("검찰입니다. 즉시 이체하세요.")
        tap("share_cancel")
        assertTrue(state.events.isEmpty())
        assertEquals("", compose.activity.safetyModel.state.value.text)
    }
    @Test fun cancellingAuthenticationNeverSends() {
        fresh(); createReview(DemoScenario.NORMAL)
        tap("transfer_confirm"); waitScreen("auth_confirm")
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitUntil(10_000) { state.current?.stage == TransferStage.REVIEW }
        assertEquals(Fixtures.START_BALANCE, state.balance)
        assertTrue(state.receipts.none { !it.seed })
    }
    @Test fun allRootTabsHaveRealContentAndBackWorks() {
        fresh()
        listOf("assets" to "assets", "transfer" to "transfer_recipient", "safety" to "safety_center", "more" to "settings", "home" to "home").forEach { (destination, tag) ->
            tap("nav_$destination"); waitScreen(tag)
        }
        navigate("history")
        compose.onNodeWithTag("history_search").performTextInput("아무도없는검색")
        compose.onNodeWithText("해당하는 거래가 없어요.").assertExists()
        device.pressBack(); waitScreen("home")
    }
    @Test fun zeroAndInsufficientAmountsCannotAdvance() {
        fresh(); tap("home_transfer"); tap("recipient_seoyeon", scroll = true)
        tap("key_clear")
        compose.onNodeWithTag("amount_next").assertIsNotEnabled()
        tap("amount_edit")
        compose.onNodeWithTag("amount_direct_input").performTextReplacement("99999999")
        compose.onNodeWithText("입력 완료").performClick()
        compose.onNodeWithTag("amount_next").assertIsNotEnabled()
        assertEquals(Fixtures.START_BALANCE, state.balance)
    }
}
