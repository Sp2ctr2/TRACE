package app.saeon.trace

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.saeon.trace.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoldenScreensTest : UiHarness() {
    @Test fun captureAllCanonicalScreens() {
        fresh(); capture("01_Home")
        navigate("assets"); capture("02_Assets")
        navigate("account"); capture("03_Account_Detail")
        navigate("transfer"); capture("04_Transfer_Recipient")
        runBlocking { repository.setDraft(TransferDraft(Fixtures.seoyeon, 32_000, Purpose.SETTLEMENT)) }
        navigate("amount"); capture("05_Transfer_Amount")
        runBlocking { repository.reviewDraft() }
        navigate("transfer_state"); waitScreen("transfer_review"); capture("06_Transfer_Review")
        authorizeOnly(); capture("07_Evaluating")
        finish(); waitScreen("transfer_complete"); capture("08_Normal_Complete")
        evaluated(DemoScenario.WARN); waitScreen("trace_warn"); capture("09_WARN")
        evaluated(DemoScenario.IMPERSONATION); waitScreen("trace_hold"); capture("10_HOLD")
        tap("hold_reasons_open", scroll = true); waitScreen("trace_hold_reason"); capture("11_HOLD_Reason_Sheet")
        compose.onNodeWithText("확인", substring = false).performScrollTo().performClick()
        compose.waitForIdle()
        navigate("safety_guide"); capture("12_Safety_Guide")
        navigate("timeline"); capture("13_Risk_Timeline")
        evaluated(DemoScenario.LOAN); waitScreen("trace_verify"); capture("14_VERIFY")
        runBlocking { repository.resolveRoute(state.currentTransferId!!) }
        waitScreen("trace_official_route"); capture("15_Official_Route")
        evaluated(DemoScenario.UNKNOWN)
        runBlocking { repository.resolveRoute(state.currentTransferId!!) }
        waitScreen("trace_unknown"); capture("16_UNKNOWN")
        navigate("safety"); capture("17_Safety_Center")
        fresh()
        share("계좌가 위험합니다. 지금 바로 안전계좌로 300만 원을 보내세요.")
        capture("18_Shared_Text_Review")
        tap("share_cancel")
        navigate("privacy"); capture("19_Privacy")
        evaluated(DemoScenario.EASY); waitScreen("trace_hold"); capture("20_Easy_Mode")
        evaluated(DemoScenario.NORMAL)
        navigate("history"); capture("21_History")
        navigate("more"); capture("22_Settings")
        navigate("demo_lab"); capture("23_Demo_Lab")
    }
}
