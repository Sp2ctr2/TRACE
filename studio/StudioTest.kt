package app.saeon.trace

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.saeon.trace.core.*
import app.saeon.trace.data.SnapshotCodec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class StudioTest:UiHarness() {
    private val theme get()=InstrumentationRegistry.getArguments().getString("theme")?:"light"
    private fun freshStudio(s:DemoScenario=DemoScenario.NORMAL){
        fresh(s)
        runBlocking{graph.preferences.theme(theme);compose.activity.model.services.resetFixtures()}
        compose.waitUntil(10000){compose.activity.model.preferences.value.themeMode==theme}
        Thread.sleep(650);compose.waitForIdle()
    }
    private fun shot(route:String,name:String){navigate(route);Thread.sleep(420);capture(name,audit=false)}
    private fun fixture(s:DemoScenario){runBlocking{repository.reset(s)};createReview(s)}
    private fun evaluatedStudio(s:DemoScenario){fixture(s);authorizeOnly();finish();Thread.sleep(400)}
    private fun fit(tag:String,name:String=tag){
        waitScreen(tag);Thread.sleep(450);compose.waitForIdle()
        val nodes=compose.onAllNodesWithTag("${tag}_body",useUnmergedTree=true).fetchSemanticsNodes()
        assertTrue("Missing task viewport $tag",nodes.isNotEmpty())
        val range=nodes.first().config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.maxValue?.invoke()?:0f
        File(output,"layout.tsv").appendText("$name\t${context.resources.displayMetrics.widthPixels}\t${context.resources.displayMetrics.heightPixels}\t${context.resources.configuration.fontScale}\t$range\n")
        capture("fit_$name",audit=true)
        assertEquals("Task $tag must fit at default type scale",0f,range,1f)
    }
    private fun hideIme(){compose.runOnIdle{
        val v=compose.activity.window.decorView
        (compose.activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(v.windowToken,0)
        compose.activity.currentFocus?.clearFocus()
    };Thread.sleep(300)}
    private fun unlock(){
        navigate("app_info");repeat(7){tap("app_version",scroll=true)};tap("stage_unlock_confirm");waitScreen("demo_center")
        assertTrue(compose.activity.model.stage.value.unlocked)
    }

    @Test fun aEveryBankDestination(){
        freshStudio()
        val routes=listOf("home","assets","account","savings","card","loan","bring","history","transfer","recipients_all","recipient_entry","safety","pending","timeline","safety_guide","privacy","manual","more","profile","security","transfer_settings","favorites","recurring","schedule_new","notifications","appearance","accessibility","support","inquiry","cases","help","app_info","investments","investment_detail","bond_detail","insurance","insurance_detail","credit","certificates","card_service","account_protection","terms")
        routes.forEachIndexed{i,route->shot(route,"bank_${(i+1).toString().padStart(2,'0')}_$route")}
        shot("receipt/${state.receipts.first().id}","bank_43_receipt")
        runBlocking{repository.setDraft(TransferDraft(Fixtures.seoyeon,32000,Purpose.SETTLEMENT))}
        shot("amount","bank_44_amount")
        tap("transfer_purpose");Thread.sleep(350);capture("bank_45_purpose_sheet",audit=false)
        device.pressBack();navigate("more")
        compose.onNodeWithTag("settings_body").performTouchInput{swipeUp()};Thread.sleep(350);capture("bank_46_more_lower",audit=false)
        runBlocking{compose.activity.model.services.document("잔액 확인서","새온은행\n시연용 잔액 확인서\n12,840,000원\n법적 효력 없는 가상 데이터")}
        shot("document","bank_47_document")
    }

    @Test fun bAllPolicySurfaces(){
        freshStudio();createReview(DemoScenario.NORMAL)
        capture("transfer_01_review",audit=false)
        tap("transfer_confirm");capture("transfer_02_authentication",audit=false)
        compose.runOnIdle{compose.activity.model.cancelAuthorization()}
        compose.waitUntil(10000){state.current?.stage==TransferStage.REVIEW}
        authorizeOnly();Thread.sleep(300);capture("transfer_03_orbit_early",audit=false)
        Thread.sleep(650);capture("transfer_04_orbit_late",audit=false)
        finish();waitScreen("transfer_complete");Thread.sleep(500);capture("transfer_05_complete",audit=false)
        assertEquals(12_808_000L,state.balance)
        tap("complete_receipt");capture("transfer_06_receipt_sheet",audit=false);tap("receipt_collapse")
        evaluatedStudio(DemoScenario.IMPERSONATION);waitScreen("trace_hold");capture("policy_01_hold",audit=false)
        tap("hold_reasons_open");capture("policy_02_hold_reasons",audit=false);tap("hold_reasons_close")
        shot("timeline","policy_03_timeline");shot("safety_guide","policy_04_guide")
        evaluatedStudio(DemoScenario.WARN);waitScreen("trace_warn");capture("policy_05_warn",audit=false)
        tap("warn_check",scroll=true);capture("policy_06_warn_checked",audit=false)
        tap("warn_acknowledge",scroll=true);waitScreen("transfer_review");capture("policy_07_warn_new_review",audit=false)
        assertNull(state.current!!.authorization);assertEquals(Fixtures.START_BALANCE,state.balance)
        evaluatedStudio(DemoScenario.LOAN);waitScreen("trace_verify");capture("policy_08_verify",audit=false)
        tap("verify_route");waitScreen("trace_official_route");capture("policy_09_official_route",audit=false)
        val old=state.currentTransferId;tap("official_route_use");waitScreen("transfer_review");capture("policy_10_official_new_review",audit=false)
        assertNotEquals(old,state.currentTransferId);assertNull(state.current!!.authorization)
        evaluatedStudio(DemoScenario.UNKNOWN);tap("verify_route");waitScreen("trace_unknown");capture("policy_11_unknown",audit=false)
        assertEquals(Fixtures.START_BALANCE,state.balance)
    }

    @Test fun cHiddenDemoAndEveryScenario(){
        freshStudio();assertFalse(compose.activity.model.stage.value.unlocked);unlock();capture("demo_01_center",audit=false)
        tap("demo_more",scroll=true);capture("demo_02_center_expanded",audit=false)
        compose.onNodeWithTag("demo_center_body").performTouchInput{swipeUp()};Thread.sleep(300);capture("demo_03_center_lower",audit=false)
        shot("comparison","demo_04_comparison")
        DemoScenario.entries.forEachIndexed{i,s->
            compose.runOnIdle{compose.activity.model.stageScenario(s){compose.activity.navigation!!.navigate("transfer_state"){launchSingleTop=true}}}
            compose.waitUntil(10000){state.scenario==s&&state.current?.stage==TransferStage.REVIEW&&!compose.activity.model.interaction.value.busy}
            waitScreen("transfer_review");Thread.sleep(350);capture("scenario_${i.toString().padStart(2,'0')}_${s.name.lowercase()}_review",audit=false)
            authorizeOnly();finish()
            if(s==DemoScenario.UNKNOWN){runBlocking{repository.resolveRoute(state.currentTransferId!!)};waitScreen("trace_unknown")}
            Thread.sleep(400);capture("scenario_${i.toString().padStart(2,'0')}_${s.name.lowercase()}_result",audit=false)
            val result=state.current!!.stage
            val expected=if(s.expected=="ALLOW")TransferStage.COMPLETE else TransferStage.valueOf(s.expected)
            assertEquals("Scenario ${s.name}",expected,result)
            assertEquals(if(expected==TransferStage.COMPLETE)Fixtures.START_BALANCE-Fixtures.amount(s)else Fixtures.START_BALANCE,state.balance)
        }
        navigate("presenter");tap("demo_hide",scroll=true);assertFalse(compose.activity.model.stage.value.unlocked)
        waitScreen("home");capture("demo_05_hidden_again",audit=false)
    }

    @Test fun dBankServicesHaveDurableEffects(){
        freshStudio();navigate("schedule_new")
        compose.onNodeWithTag("schedule_title").performTextInput("생활비")
        compose.onNodeWithTag("schedule_amount").performTextInput("200000")
        hideIme();capture("service_01_schedule_form",audit=false);tap("schedule_save")
        compose.waitUntil(10000){compose.activity.model.services.state.value.schedules.size==2}
        shot("recurring","service_02_schedule_saved")
        navigate("inquiry");compose.onNodeWithTag("inquiry_body").performTextInput("송금 보류 내용을 다시 확인하고 싶어요.")
        hideIme();capture("service_03_inquiry_form",audit=false);tap("inquiry_save");waitScreen("cases");capture("service_04_inquiry_saved",audit=false)
        assertEquals(1,compose.activity.model.services.state.value.cases.size)
        navigate("card_service");tap("card_loss");capture("service_05_card_loss_confirm",audit=false);tap("service_confirm")
        compose.waitUntil(10000){compose.activity.model.services.state.value.cardLost&&compose.activity.model.preferences.value.cardFrozen}
        capture("service_06_card_lost",audit=false)
        compose.onNodeWithText("재발급 요청").performScrollTo().performClick();waitScreen("replacement");capture("service_07_replacement",audit=false)
        assertTrue(compose.activity.model.services.state.value.replacementRequested)
        navigate("account_protection");tap("account_lock");tap("service_confirm")
        compose.waitUntil(10000){state.accountLocked};capture("service_08_account_locked",audit=false)
        assertTrue(SnapshotCodec.decode(SnapshotCodec.encode(state)).accountLocked)
        try{runBlocking{repository.review(TransferDraft(Fixtures.seoyeon,32000,Purpose.SETTLEMENT))};fail("Locked account accepted review")}catch(e:BankFailure){assertEquals("ACCOUNT_LOCKED",e.code)}
        compose.activityRule.scenario.recreate();awaitReady();assertTrue(state.accountLocked)
        navigate("account_protection");tap("account_lock");tap("service_confirm");compose.waitUntil(10000){!state.accountLocked}
        capture("service_09_account_unlocked",audit=false)
        assertEquals(2,compose.activity.model.services.state.value.schedules.size)
    }

    @Test fun eCoreTasksFitAndRemainAccessible(){
        freshStudio();fit("home");compose.onNodeWithTag("home_transfer").assertIsDisplayed();compose.onNodeWithTag("glass_dock").assertIsDisplayed()
        navigate("transfer");fit("transfer_recipient")
        runBlocking{repository.setDraft(TransferDraft(Fixtures.seoyeon,32000,Purpose.SETTLEMENT))};navigate("amount");fit("transfer_amount");compose.onNodeWithTag("amount_next").assertIsDisplayed()
        createReview(DemoScenario.NORMAL);fit("transfer_review");authorizeOnly();fit("trace_evaluating");finish();fit("transfer_complete")
        evaluatedStudio(DemoScenario.IMPERSONATION);fit("trace_hold");compose.onNodeWithTag("hold_safe_action").assertIsDisplayed()
        evaluatedStudio(DemoScenario.LOAN);fit("trace_verify")
        evaluatedStudio(DemoScenario.UNKNOWN);tap("verify_route");fit("trace_unknown")
        runBlocking{graph.preferences.easy(true);graph.preferences.motion(true);graph.preferences.transparency(true)}
        compose.waitUntil(10000){compose.activity.model.preferences.value.easyMode}
        evaluatedStudio(DemoScenario.IMPERSONATION);capture("accessibility_01_easy_hold",audit=true)
        compose.onNodeWithTag("hold_safe_action").assertIsDisplayed()
        navigate("home");capture("accessibility_02_easy_home",audit=false)
    }

    @Test fun fLiveTransferAndPersistence(){
        freshStudio();capture("motion_01_home",audit=false)
        tap("home_transfer");tap("recipient_seoyeon");waitScreen("transfer_amount")
        tap("key_clear");listOf("3","2","0","0","0").forEach{tap("key_$it")}
        tap("amount_next");waitScreen("transfer_review");capture("motion_02_review",audit=false)
        tap("transfer_confirm");capture("motion_03_auth",audit=false)
        val start=android.os.SystemClock.elapsedRealtime();tap("auth_confirm");Thread.sleep(260);capture("motion_04_orbit",audit=false)
        waitScreen("transfer_complete");val elapsed=android.os.SystemClock.elapsedRealtime()-start
        assertTrue("Gate must not commit before minimum duration",elapsed>=1290)
        File(output,"gate_elapsed_ms.txt").writeText(elapsed.toString())
        assertEquals(12_808_000L,state.balance);assertEquals(1,state.receipts.count{!it.seed});capture("motion_05_complete",audit=false)
        compose.activityRule.scenario.recreate();awaitReady();assertEquals(12_808_000L,state.balance);assertEquals(1,state.receipts.count{!it.seed})
        capture("motion_06_restored",audit=false)
    }
}
