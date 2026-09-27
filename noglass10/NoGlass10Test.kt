package app.saeon.trace

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import app.saeon.trace.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class NoGlass10Test:UiHarness(){
    private val theme get()=InstrumentationRegistry.getArguments().getString("theme")?:"light"
    private fun reset(){
        fresh();runBlocking{graph.preferences.theme(theme);compose.activity.model.services.resetFixtures()}
        compose.runOnIdle{compose.activity.model.uiTestAuthentication=true}
        compose.waitUntil(10000){compose.activity.model.preferences.value.themeMode==theme}
        Thread.sleep(850)
    }
    private fun shot(route:String,name:String){navigate(route);Thread.sleep(380);capture(name,audit=true)}
    private fun scenario(s:DemoScenario){
        runBlocking{repository.reset(s)};createReview(s);authorizeOnly();finish()
        waitScreen(when(s){DemoScenario.NORMAL,DemoScenario.EDUCATION,DemoScenario.UNRELATED,DemoScenario.NEW_ACCOUNT->"transfer_complete";DemoScenario.LOAN->"trace_official_route";DemoScenario.UNKNOWN->"trace_unknown";DemoScenario.WARN->"trace_warn";else->"trace_hold"})
    }
    private fun hideIme(){compose.runOnIdle{(compose.activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(compose.activity.window.decorView.windowToken,0);compose.activity.currentFocus?.clearFocus()};Thread.sleep(400)}
    private fun measure(tag:String){
        waitScreen(tag);Thread.sleep(300)
        val n=compose.onAllNodesWithTag("${tag}_body",useUnmergedTree=true).fetchSemanticsNodes().firstOrNull()?:return
        val scroll=n.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.maxValue?.invoke()?:0f
        File(output,"layout.tsv").appendText("$tag\t${context.resources.configuration.screenWidthDp}\t${context.resources.configuration.screenHeightDp}\t$scroll\n")
        capture("fit_$tag",audit=true)
    }
    @Test fun aAllDestinations(){
        reset()
        val routes=listOf("home","assets","account","savings","card","loan","bring","history","transfer","recipients_all","recipient_entry","safety","pending","timeline","safety_guide","privacy","manual","more","profile","security","transfer_settings","favorites","recurring","schedule_new","notifications","appearance","accessibility","support","inquiry","cases","help","app_info","investments","investment_detail","bond_detail","insurance","insurance_detail","credit","certificates","card_service","account_protection","terms")
        routes.forEachIndexed{i,r->shot(r,"screen_${i.toString().padStart(2,'0')}_$r")}
        shot("receipt/${state.receipts.first().id}","screen_receipt")
        runBlocking{repository.setDraft(TransferDraft(Fixtures.seoyeon,32000,Purpose.SETTLEMENT))};shot("amount","screen_amount")
        tap("transfer_purpose",scroll=true);capture("sheet_purpose",audit=false);device.pressBack()
        navigate("help");tap("help_0",scroll=true);capture("help_expanded",audit=true)
        runBlocking{compose.activity.model.services.document("잔액 확인서","새온은행\n시연용 확인서\n12,840,000원\n증빙 효력 없음")}
        shot("document","screen_document")
    }
    @Test fun bHomeNavigationAndPrivacy(){
        reset();measure("home")
        val left=compose.onNodeWithTag("home_bring").fetchSemanticsNode().boundsInRoot
        val right=compose.onNodeWithTag("home_transfer").fetchSemanticsNode().boundsInRoot
        assertEquals(left.width,right.width,1.5f);assertTrue(left.right<right.left)
        tap("home_hide_balance");compose.onAllNodesWithText("12,840,000원").assertCountEquals(0);capture("home_hidden",audit=true)
        tap("home_hide_balance");tap("home_account_picker");capture("home_accounts",audit=false);device.pressBack()
        listOf("home","assets","transfer","safety","more").forEach{r->
            tap("nav_$r");Thread.sleep(800)
            val selection=compose.onNodeWithTag("bank_selection",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            val tab=compose.onNodeWithTag("nav_$r",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            assertEquals(r,selection.center.x,tab.center.x,1.5f);assertEquals(r,selection.center.y,tab.center.y,1.5f)
            File(output,"nav-centers.tsv").appendText("$r\t${selection.center.x-tab.center.x}\t${selection.center.y-tab.center.y}\n")
            capture("tab_$r",audit=true)
        }
        runBlocking{graph.preferences.easy(true)};compose.waitUntil(10000){compose.activity.model.preferences.value.easyMode}
        shot("home","easy_home");shot("safety_guide","easy_guide")
    }
    @Test fun cPolicyScenariosAndContext(){
        reset();scenario(DemoScenario.NORMAL);capture("policy_allow",audit=true);assertEquals(12808000L,state.balance)
        scenario(DemoScenario.IMPERSONATION);capture("policy_hold",audit=true);tap("hold_reasons_open",scroll=true);waitScreen("trace_timeline");capture("context_panels",audit=true)
        compose.onNodeWithTag("context_transfer").assertExists()
        scenario(DemoScenario.WARN);capture("policy_warn",audit=false);tap("warn_check",scroll=true);tap("warn_acknowledge",scroll=true);waitScreen("transfer_review");assertNull(state.current!!.authorization)
        scenario(DemoScenario.LOAN);capture("policy_verify",audit=true)
        val original=state.currentTransferId;tap("official_route_use",scroll=true);waitScreen("transfer_review");assertNotEquals(original,state.currentTransferId);assertNull(state.current!!.authorization)
        scenario(DemoScenario.UNKNOWN);capture("policy_unknown",audit=true);assertEquals(Fixtures.START_BALANCE,state.balance)
        DemoScenario.entries.filter{it !in listOf(DemoScenario.NORMAL,DemoScenario.IMPERSONATION,DemoScenario.LOAN,DemoScenario.WARN,DemoScenario.UNKNOWN)}.forEach{s->scenario(s);capture("case_${s.name.lowercase()}",audit=false)}
        navigate("app_info");repeat(7){tap("app_version",scroll=true)};tap("stage_unlock_confirm");waitScreen("demo_center");capture("demo_center",audit=true)
        tap("demo_more",scroll=true);capture("demo_expanded",audit=true);shot("comparison","same_amount_compare")
    }
    @Test fun dRealSystemAuthentication(){
        reset();createReview(DemoScenario.NORMAL)
        compose.runOnIdle{compose.activity.model.uiTestAuthentication=false}
        tap("transfer_confirm")
        assertTrue("System credential UI did not appear",device.wait(Until.hasObject(By.clazz("android.widget.EditText")),10000))
        capture("system_device_auth",audit=false)
        assertEquals(Fixtures.START_BALANCE,state.balance)
        val input=device.findObject(By.clazz("android.widget.EditText"))
        input.text="2468"
        device.pressEnter()
        waitScreen("transfer_complete")
        assertEquals(12808000L,state.balance)
        assertEquals(AuthMethod.DEVICE_CREDENTIAL,state.current!!.authorization!!.method)
        assertEquals(1,state.receipts.count{!it.seed})
        capture("device_auth_complete",audit=true)
        compose.activityRule.scenario.recreate();awaitReady();assertEquals(12808000L,state.balance)
    }
    @Test fun eRotationAndMeasuredTasks(){
        reset();createReview(DemoScenario.NORMAL);val id=state.currentTransferId
        measure("transfer_review")
        device.setOrientationLeft();Thread.sleep(1700);awaitReady()
        assertEquals(id,state.currentTransferId);measure("transfer_review")
        shot("home","rotated_home");shot("assets","rotated_assets");shot("transfer","rotated_transfer")
        createReview(DemoScenario.NORMAL);authorizeOnly();measure("trace_evaluating");finish();measure("transfer_complete")
        scenario(DemoScenario.LOAN);measure("trace_official_route")
        scenario(DemoScenario.IMPERSONATION);measure("trace_hold")
        device.setOrientationNatural();device.unfreezeRotation();Thread.sleep(1200)
        assertEquals(TransferStage.HOLD,state.current!!.stage)
        capture("rotation_restored_hold",audit=true)
    }
    @Test fun fServicesAndSlowLookup(){
        reset();navigate("bring");tap("bring_source",scroll=true);compose.onNodeWithTag("bring_amount").performTextInput("100000");hideIme();tap("bring_confirm",scroll=true)
        compose.waitUntil(10000){state.balance==12940000L};assertEquals(2300000L,state.savings);capture("bring_complete",audit=true)
        navigate("schedule_new");compose.onNodeWithTag("schedule_title").performTextInput("생활비");compose.onNodeWithTag("schedule_amount").performTextInput("200000");hideIme();tap("schedule_save",scroll=true)
        compose.waitUntil(10000){compose.activity.model.services.state.value.schedules.size==2};shot("recurring","schedule_saved")
        navigate("account_protection");tap("account_lock",scroll=true);tap("service_confirm");compose.waitUntil(10000){state.accountLocked};capture("account_locked",audit=true)
        compose.activityRule.scenario.recreate();awaitReady();assertTrue(state.accountLocked)
        compose.runOnIdle{compose.activity.model.routeDelayForTest=2000}
        runBlocking{repository.reset(DemoScenario.LOAN)};createReview(DemoScenario.LOAN);authorizeOnly();finish();waitScreen("loading_skeleton");capture("pending_real_lookup",audit=false)
        waitScreen("trace_official_route");compose.onAllNodesWithTag("loading_skeleton").assertCountEquals(0)
    }
    @Test fun gMotion(){
        reset();tap("nav_assets");Thread.sleep(300);tap("nav_home");tap("home_transfer");tap("recipient_seoyeon",scroll=true)
        tap("key_clear",scroll=true);listOf("3","2","0","0","0").forEach{tap("key_$it",scroll=true)};tap("amount_next",scroll=true)
        waitScreen("transfer_review");tap("transfer_confirm");tap("auth_confirm");waitScreen("transfer_complete");assertEquals(12808000L,state.balance)
        capture("motion_result",audit=true)
    }
}
