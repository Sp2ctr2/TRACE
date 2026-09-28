package app.saeon.trace

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.saeon.trace.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CriticalSingle11Test:UiHarness(){
    private fun reset(){
        fresh()
        compose.runOnIdle{compose.activity.model.uiTestAuthentication=true}
        Thread.sleep(700)
    }
    @Test fun review(){
        reset();createReview(DemoScenario.NORMAL);capture("04_transfer_review",audit=true)
    }
    @Test fun gate(){
        reset();createReview(DemoScenario.NORMAL);authorizeOnly();Thread.sleep(180);capture("05_trace_check",audit=false)
    }
    @Test fun complete(){
        reset();createReview(DemoScenario.NORMAL);authorizeOnly();finish();waitScreen("transfer_complete");capture("06_allow_complete",audit=true)
    }
    @Test fun demo(){
        reset();navigate("app_info");repeat(7){tap("app_version",scroll=true)};tap("stage_unlock_confirm");waitScreen("demo_center");capture("11_demo_center",audit=true)
    }
}
