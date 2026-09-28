package app.saeon.trace

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.saeon.trace.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class CriticalShots11Test:UiHarness(){
    private fun reset(){
        fresh()
        compose.runOnIdle{compose.activity.model.uiTestAuthentication=true}
        Thread.sleep(900)
        compose.waitForIdle()
    }
    private fun scenario(s:DemoScenario){
        runBlocking{repository.reset(s)}
        createReview(s);authorizeOnly();finish()
        val tag=when(s){
            DemoScenario.NORMAL,DemoScenario.EDUCATION,DemoScenario.UNRELATED,DemoScenario.NEW_ACCOUNT->"transfer_complete"
            DemoScenario.LOAN->"trace_official_route"
            DemoScenario.UNKNOWN->"trace_unknown"
            DemoScenario.WARN->"trace_warn"
            else->"trace_hold"
        }
        waitScreen(tag);Thread.sleep(300)
    }

    @Test fun mainTabs(){
        reset()
        capture("01_home",audit=true)
        navigate("assets");Thread.sleep(300);capture("02_assets",audit=true)
        navigate("safety");Thread.sleep(300);capture("03_safety",audit=true)
    }

    @Test fun allowFlow(){
        reset()
        createReview(DemoScenario.NORMAL)
        waitScreen("transfer_review");capture("04_transfer_review",audit=true)
        authorizeOnly()
        waitScreen("trace_evaluating");Thread.sleep(220);capture("05_trace_check",audit=true)
        finish()
        waitScreen("transfer_complete");capture("06_allow_complete",audit=true)
        assertEquals(12_808_000L,state.balance)
    }

    @Test fun warnAndVerify(){
        reset()
        scenario(DemoScenario.WARN);capture("07_warn",audit=true)
        scenario(DemoScenario.LOAN);capture("08_verify",audit=true)
        assertNotNull(state.current!!.route)
    }

    @Test fun holdAndContext(){
        reset()
        scenario(DemoScenario.IMPERSONATION);capture("09_hold",audit=true)
        tap("hold_reasons_open",scroll=true)
        waitScreen("trace_timeline");Thread.sleep(300);capture("10_context",audit=true)
    }

    @Test fun demoCenter(){
        reset()
        navigate("app_info")
        repeat(7){tap("app_version",scroll=true)}
        tap("stage_unlock_confirm")
        waitScreen("demo_center");Thread.sleep(300);capture("11_demo_center",audit=true)
    }
}
