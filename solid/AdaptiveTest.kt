package app.saeon.trace

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.saeon.trace.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AdaptiveTest:UiHarness(){

    private fun reset(){
        fresh()
        Thread.sleep(800)
        compose.waitForIdle()
    }
    private fun shot(route:String,name:String){
        navigate(route);Thread.sleep(380);compose.waitForIdle();capture(name,audit=true)
    }

    @Test fun homeActionsAndNavigationGeometry(){
        reset()
        val dm=context.resources.displayMetrics
        val widthDp=dm.widthPixels/dm.density
        val heightDp=dm.heightPixels/dm.density
        val bring=compose.onNodeWithTag("home_bring").fetchSemanticsNode().boundsInRoot
        val send=compose.onNodeWithTag("home_transfer").fetchSemanticsNode().boundsInRoot
        assertEquals("Equal home action width",bring.width,send.width,1.5f)
        assertEquals("Aligned home actions",bring.center.y,send.center.y,1f)
        assertTrue("Distinct actions",bring.right<send.left)
        if(widthDp>=600f||heightDp<480f){
            compose.onNodeWithTag("root_navigation_rail").assertExists()
            compose.onAllNodesWithTag("root_navigation").assertCountEquals(0)
        }else{
            compose.onNodeWithTag("root_navigation").assertExists()
            compose.onAllNodesWithTag("root_navigation_rail").assertCountEquals(0)
        }
        capture("adaptive_home",audit=true)
        listOf("assets","transfer","safety","more").forEach{r->tap("nav_$r");Thread.sleep(320);capture("adaptive_$r",audit=true)}
    }

    @Test fun tabletAndLandscapeContentDoesNotStretchIntoWhitespace(){
        reset()
        val dm=context.resources.displayMetrics
        val widthDp=dm.widthPixels/dm.density
        if(widthDp>=600f){
            val body=compose.onNodeWithTag("home_body",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            assertTrue("Wide body should use available width",body.width>500f*dm.density)
        }
        listOf("home","assets","safety","more").forEach{shot(it,"formfactor_$it")}
    }

    @Test fun rotationPreservesDraftAndCurrentTask(){
        reset()
        tap("home_transfer");tap("recipient_seoyeon");waitScreen("transfer_amount")
        tap("key_clear");listOf("3","2","0","0","0").forEach{tap("key_$it")}
        assertEquals(32000L,state.draft!!.amount)
        capture("rotation_before",audit=true)
        device.setOrientationLeft()
        Thread.sleep(1200);compose.waitForIdle()
        assertEquals("Draft survives rotation",32000L,state.draft!!.amount)
        compose.onNodeWithTag("transfer_amount").assertExists()
        capture("rotation_landscape",audit=true)
        device.setOrientationNatural()
        Thread.sleep(1200);compose.waitForIdle()
        assertEquals(32000L,state.draft!!.amount)
        compose.onNodeWithTag("transfer_amount").assertExists()
        capture("rotation_restored",audit=true)
        device.unfreezeRotation()
    }

    @Test fun policyScreensRemainReadableAcrossWindowShapes(){
        reset()
        fun evaluated(s:DemoScenario){
            runBlocking{repository.reset(s)}
            createReview(s);authorizeOnly();finish();Thread.sleep(500)
        }
        evaluated(DemoScenario.IMPERSONATION)
        waitScreen("trace_hold");capture("policy_hold",audit=true)
        tap("hold_reasons_open");waitScreen("trace_timeline");compose.onNodeWithTag("timeline_cards").assertExists();capture("policy_context_cards",audit=true)

        evaluated(DemoScenario.WARN)
        waitScreen("trace_warn");capture("policy_warn",audit=true)

        evaluated(DemoScenario.LOAN)
        compose.waitUntil(10000){state.current?.route!=null}
        waitScreen("trace_official_route");compose.onNodeWithTag("verified_account").assertExists();capture("policy_verify",audit=true)

        evaluated(DemoScenario.UNKNOWN)
        waitScreen("trace_unknown");capture("policy_unknown",audit=true)
    }

    @Test fun importantTaskViewportsDoNotRequireDefaultScaleScrolling(){
        reset()
        fun fit(tag:String){
            waitScreen(tag);Thread.sleep(250)
            val nodes=compose.onAllNodesWithTag("${tag}_body",useUnmergedTree=true).fetchSemanticsNodes()
            if(nodes.isNotEmpty()){
                val range=nodes.first().config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.maxValue?.invoke()?:0f
                File(output,"adaptive-layout.tsv").appendText("$tag\t$range\n")
                if(context.resources.displayMetrics.heightPixels/context.resources.displayMetrics.density>=480f) assertEquals(tag,0f,range,1f)
            }
            capture("fit_$tag",audit=true)
        }
        fit("home")
        tap("home_transfer");fit("transfer_recipient")
        tap("recipient_seoyeon");fit("transfer_amount")
        tap("key_clear");listOf("3","2","0","0","0").forEach{tap("key_$it")};tap("amount_next");fit("transfer_review")
        tap("transfer_confirm");tap("auth_confirm");fit("trace_evaluating")
        waitScreen("transfer_complete");fit("transfer_complete")
    }
}
