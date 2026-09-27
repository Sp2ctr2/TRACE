package app.saeon.trace

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.saeon.trace.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RefinedTest:UiHarness(){
    private val theme get()=InstrumentationRegistry.getArguments().getString("theme")?:"light"
    private fun reset(s:DemoScenario=DemoScenario.NORMAL){
        fresh(s)
        runBlocking{graph.preferences.theme(theme);compose.activity.model.services.resetFixtures()}
        compose.waitUntil(10000){compose.activity.model.preferences.value.themeMode==theme}
        Thread.sleep(1000);compose.waitForIdle()
    }
    private fun shot(route:String,name:String){navigate(route);Thread.sleep(450);capture(name,audit=false)}
    private fun scenario(s:DemoScenario){
        runBlocking{repository.reset(s)};createReview(s);authorizeOnly();finish()
        val tag=when(s){
            DemoScenario.NORMAL,DemoScenario.EDUCATION,DemoScenario.UNRELATED,DemoScenario.NEW_ACCOUNT->"transfer_complete"
            DemoScenario.LOAN->"trace_official_route"
            DemoScenario.UNKNOWN->"trace_unknown"
            DemoScenario.WARN->"trace_warn"
            else->"trace_hold"
        };waitScreen(tag);Thread.sleep(350)
    }
    private fun hideKeyboard(){compose.runOnIdle{
        val view=compose.activity.window.decorView
        (compose.activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(view.windowToken,0)
        compose.activity.currentFocus?.clearFocus()
    };Thread.sleep(500)}
    private fun fit(tag:String){
        waitScreen(tag);Thread.sleep(350)
        val n=compose.onAllNodesWithTag("${tag}_body",useUnmergedTree=true).fetchSemanticsNodes().firstOrNull()
        assertNotNull("Missing body $tag",n)
        val range=n!!.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.maxValue?.invoke()?:0f
        File(output,"viewports.tsv").appendText("$tag\t${context.resources.displayMetrics.widthPixels}\t${context.resources.displayMetrics.heightPixels}\t$range\n")
        capture("fit_$tag",audit=false)
        assertEquals("Unexpected default-scale scroll in $tag",0f,range,1f)
    }

    @Test fun allBankScreensAndDetails(){
        reset()
        val paths=listOf("home","assets","account","savings","card","loan","bring","history","transfer","recipients_all","recipient_entry","safety","pending","timeline","safety_guide","privacy","manual","more","profile","security","transfer_settings","favorites","recurring","schedule_new","notifications","appearance","accessibility","support","inquiry","cases","help","app_info","investments","investment_detail","bond_detail","insurance","insurance_detail","credit","certificates","card_service","account_protection","terms")
        paths.forEachIndexed{i,p->shot(p,"screen_${i.toString().padStart(2,'0')}_$p")}
        shot("receipt/${state.receipts.first().id}","screen_42_receipt")
        runBlocking{repository.setDraft(TransferDraft(Fixtures.seoyeon,32000,Purpose.SETTLEMENT))};shot("amount","screen_43_amount")
        tap("transfer_purpose");capture("screen_44_purpose",audit=false);device.pressBack()
        navigate("help");tap("help_0",scroll=true);capture("screen_45_help_expanded",audit=false)
        navigate("appearance");compose.onAllNodesWithText("12,840,000원").assertCountEquals(0)
        capture("screen_46_appearance",audit=false)
        runBlocking{compose.activity.model.services.document("잔액 확인서","새온은행\n시연용 잔액 확인서\n12,840,000원\n증빙 효력 없음")}
        shot("document","screen_47_document")
        shot("more","screen_48_more_top");compose.onNodeWithTag("settings_body").performTouchInput{swipeUp()};Thread.sleep(250);capture("screen_49_more_lower",audit=false)
    }

    @Test fun measuredControlsAndHome(){
        reset();fit("home")
        val left=compose.onNodeWithTag("home_bring").fetchSemanticsNode().boundsInRoot
        val right=compose.onNodeWithTag("home_transfer").fetchSemanticsNode().boundsInRoot
        assertEquals(left.width,right.width,1.5f);assertEquals(left.center.y,right.center.y,1f);assertTrue(left.right<right.left)
        tap("home_hide_balance");compose.onAllNodesWithText("12,840,000원").assertCountEquals(0);capture("home_hidden",audit=false)
        tap("home_hide_balance");tap("home_account_picker");capture("home_accounts",audit=false);device.pressBack()
        listOf("home","assets","transfer","safety","more").forEach{r->
            tap("nav_$r");Thread.sleep(850)
            val lens=compose.onNodeWithTag("glass_lens",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            val tab=compose.onNodeWithTag("nav_$r",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            assertEquals(r,tab.center.x,lens.center.x,1.5f);assertEquals(r,tab.center.y,lens.center.y,1.5f)
            File(output,"dock-centers.tsv").appendText("$r\t${tab.center.x}\t${lens.center.x}\t${tab.center.y}\t${lens.center.y}\n")
            capture("tab_$r",audit=false)
        }
        runBlocking{graph.preferences.easy(true)};compose.waitUntil(10000){compose.activity.model.preferences.value.easyMode}
        navigate("home");compose.onNodeWithText("돈 보내기").assertExists();compose.onNodeWithText("도움 받기").assertExists();capture("easy_home",audit=false)
        navigate("help");tap("help_0",scroll=true);capture("easy_help",audit=false)
    }

    @Test fun decisionsAndIndependentRoute(){
        reset();scenario(DemoScenario.NORMAL);capture("policy_allow",audit=false);assertEquals(12_808_000L,state.balance)
        tap("complete_receipt");capture("complete_receipt",audit=false);tap("receipt_collapse")
        scenario(DemoScenario.IMPERSONATION);capture("policy_hold",audit=false);assertEquals(Fixtures.START_BALANCE,state.balance)
        tap("hold_reasons_open");waitScreen("trace_timeline");capture("hold_context",audit=false)
        compose.onAllNodesWithTag("trace_hold_reason").assertCountEquals(0)
        scenario(DemoScenario.WARN);capture("policy_warn",audit=false)
        tap("warn_check",scroll=true);capture("warn_checked",audit=false);tap("warn_acknowledge",scroll=true)
        waitScreen("transfer_review");assertNull(state.current!!.authorization);assertEquals(Fixtures.START_BALANCE,state.balance)
        scenario(DemoScenario.LOAN);capture("policy_verify_registered",audit=false)
        val prior=state.currentTransferId;val route=state.current!!.route
        assertNotNull(route);assertEquals(prior,route!!.sourceIntentId)
        compose.onNodeWithTag("verified_account").assertExists()
        tap("official_route_use");waitScreen("transfer_review")
        assertNotEquals(prior,state.currentTransferId);assertNull(state.current!!.authorization)
        capture("official_fresh_review",audit=false)
        scenario(DemoScenario.UNKNOWN);capture("policy_unknown",audit=false);assertEquals(Fixtures.START_BALANCE,state.balance)
    }

    @Test fun everyScenarioAndHiddenCenter(){
        reset();navigate("app_info");repeat(7){tap("app_version",scroll=true)};tap("stage_unlock_confirm");waitScreen("demo_center")
        capture("demo_center",audit=false);tap("demo_more",scroll=true);capture("demo_expanded",audit=false)
        shot("comparison","demo_comparison")
        DemoScenario.entries.forEach{s->
            scenario(s)
            capture("case_${s.name.lowercase()}",audit=false)
            val expected=when(s){DemoScenario.NORMAL,DemoScenario.EDUCATION,DemoScenario.UNRELATED,DemoScenario.NEW_ACCOUNT->TransferStage.COMPLETE;DemoScenario.LOAN->TransferStage.ROUTE;DemoScenario.UNKNOWN->TransferStage.UNKNOWN;DemoScenario.WARN->TransferStage.WARN;else->TransferStage.HOLD}
            assertEquals(s.name,expected,state.current!!.stage)
            assertEquals(if(expected==TransferStage.COMPLETE)Fixtures.START_BALANCE-Fixtures.amount(s)else Fixtures.START_BALANCE,state.balance)
        }
        navigate("presenter");tap("demo_hide",scroll=true);compose.waitUntil(10000){!compose.activity.model.stage.value.unlocked};waitScreen("home")
    }

    @Test fun realPendingSkeletonAndTaskBounds(){
        reset();fit("home");navigate("transfer");fit("transfer_recipient")
        createReview(DemoScenario.NORMAL);fit("transfer_review");authorizeOnly();fit("trace_evaluating");finish();fit("transfer_complete")
        scenario(DemoScenario.IMPERSONATION);fit("trace_hold")
        compose.runOnIdle{compose.activity.model.routeDelayForTest=1600}
        runBlocking{repository.reset(DemoScenario.LOAN)};createReview(DemoScenario.LOAN);authorizeOnly();finish()
        waitScreen("loading_skeleton");capture("route_pending_skeleton",audit=false)
        waitScreen("trace_official_route");compose.onAllNodesWithTag("loading_skeleton").assertCountEquals(0);fit("trace_official_route")
        compose.runOnIdle{compose.activity.model.routeDelayForTest=0}
        scenario(DemoScenario.UNKNOWN);fit("trace_unknown")
        runBlocking{graph.preferences.motion(true);graph.preferences.transparency(true);graph.preferences.easy(true)}
        compose.waitUntil(10000){compose.activity.model.preferences.value.reducedMotion}
        scenario(DemoScenario.IMPERSONATION);capture("easy_hold",audit=false);compose.onNodeWithTag("hold_safe_action").assertIsDisplayed()
        navigate("safety_guide");capture("easy_guide",audit=false)
    }

    @Test fun bringTransferAndPersistence(){
        reset();tap("home_bring");waitScreen("bring");capture("bring_sources",audit=false)
        tap("bring_source");compose.onNodeWithTag("bring_amount").performTextInput("100000");hideKeyboard();capture("bring_amount",audit=false)
        tap("bring_confirm");compose.waitUntil(10000){state.balance==12_940_000L};assertEquals(2_300_000L,state.savings)
        capture("bring_receipt",audit=false)
        val total=state.balance+state.savings
        compose.activityRule.scenario.recreate();awaitReady();assertEquals(total,state.balance+state.savings)
        assertEquals(1,state.receipts.count{!it.seed})
    }

    @Test fun liveTransferMotion(){
        reset();capture("motion_home",audit=false)
        tap("nav_assets");Thread.sleep(400);tap("nav_home");tap("home_transfer");tap("recipient_seoyeon");waitScreen("transfer_amount")
        tap("key_clear");listOf("3","2","0","0","0").forEach{tap("key_$it")}
        tap("amount_next");waitScreen("transfer_review");capture("motion_review",audit=false)
        tap("transfer_confirm");capture("motion_auth",audit=false)
        val start=android.os.SystemClock.elapsedRealtime();tap("auth_confirm");Thread.sleep(250);capture("motion_gate",audit=false)
        waitScreen("transfer_complete");val elapsed=android.os.SystemClock.elapsedRealtime()-start
        assertTrue(elapsed>=1290);File(output,"gate_elapsed_ms.txt").writeText(elapsed.toString())
        assertEquals(12_808_000L,state.balance);assertEquals(1,state.receipts.count{!it.seed})
        capture("motion_complete",audit=false)
        compose.activityRule.scenario.recreate();awaitReady();assertEquals(12_808_000L,state.balance);assertEquals(1,state.receipts.count{!it.seed})
    }
}
