package app.saeon.trace

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.saeon.trace.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoldenScreensTest : UiHarness() {
    @Test fun captureAllCanonicalScreens() {
        val layoutFailures = mutableListOf<String>()
        // Preserve every screenshot for diagnosis. A layout defect still fails
        // this test at the end; navigation and transaction failures stop at once.
        fun snap(name: String) {
            try { capture(name) }
            catch (failure: AssertionError) { layoutFailures += "$name: ${failure.message}" }
        }
        fresh(); snap("01_Home")
        navigate("assets"); snap("02_Assets")
        navigate("account"); snap("03_Account_Detail")
        navigate("transfer"); snap("04_Transfer_Recipient")
        runBlocking { repository.setDraft(TransferDraft(Fixtures.seoyeon, 32_000, Purpose.SETTLEMENT)) }
        navigate("amount"); snap("05_Transfer_Amount")
        runBlocking { repository.reviewDraft() }
        navigate("transfer_state"); waitScreen("transfer_review"); snap("06_Transfer_Review")
        authorizeOnly(); snap("07_Evaluating")
        finish(); waitScreen("transfer_complete"); snap("08_Normal_Complete")
        evaluated(DemoScenario.WARN); waitScreen("trace_warn"); snap("09_WARN")
        evaluated(DemoScenario.IMPERSONATION); waitScreen("trace_hold"); snap("10_HOLD")
        tap("hold_reasons_open", scroll = true); waitScreen("trace_hold_reason"); snap("11_HOLD_Reason_Sheet")
        compose.onNodeWithText("확인", substring = false).performScrollTo().performClick()
        compose.waitForIdle()
        navigate("safety_guide"); snap("12_Safety_Guide")
        navigate("timeline"); snap("13_Risk_Timeline")
        evaluated(DemoScenario.LOAN); waitScreen("trace_verify"); snap("14_VERIFY")
        runBlocking { repository.resolveRoute(state.currentTransferId!!) }
        waitScreen("trace_official_route"); snap("15_Official_Route")
        evaluated(DemoScenario.UNKNOWN)
        runBlocking { repository.resolveRoute(state.currentTransferId!!) }
        waitScreen("trace_unknown"); snap("16_UNKNOWN")
        navigate("safety"); snap("17_Safety_Center")
        fresh()
        share("계좌가 위험합니다. 지금 바로 안전계좌로 300만 원을 보내세요.")
        snap("18_Shared_Text_Review")
        tap("share_cancel")
        navigate("privacy"); snap("19_Privacy")
        evaluated(DemoScenario.EASY); waitScreen("trace_hold"); snap("20_Easy_Mode")
        evaluated(DemoScenario.NORMAL)
        navigate("history"); snap("21_History")
        navigate("more"); snap("22_Settings")
        navigate("demo_lab"); snap("23_Demo_Lab")
        assertTrue(layoutFailures.joinToString("\n\n"), layoutFailures.isEmpty())
    }
}
