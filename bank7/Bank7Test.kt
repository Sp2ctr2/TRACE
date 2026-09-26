package app.saeon.trace

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.saeon.trace.core.*
import app.saeon.trace.data.SnapshotCodec
import app.saeon.trace.ui.design.*
import app.saeon.trace.ui.screens.ServiceStore
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Before
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class Bank7Test:UiHarness() {
    @Before fun clearServices(){context.getSharedPreferences("saeon_services_v7",0).edit().clear().commit()}
    private fun base(s:DemoScenario=DemoScenario.NORMAL){fresh(s);Thread.sleep(800);compose.runOnIdle{compose.activity.model.stageLock()}}
    private fun theme(value:String){runBlocking{graph.preferences.theme(value)};compose.waitUntil(10000){compose.activity.model.preferences.value.themeMode==value};Thread.sleep(350)}
    private fun shot(name:String){Thread.sleep(220);capture(name,audit=false);File(output,"screens.tsv").appendText("$name\t${context.resources.configuration.screenWidthDp}\t${compose.activity.model.preferences.value.themeMode}\n")}
    private fun fit(tag:String){
        waitScreen(tag);val nodes=compose.onAllNodesWithTag("${tag}_body",useUnmergedTree=true).fetchSemanticsNodes();assertTrue("Missing bounded body $tag",nodes.isNotEmpty())
        val range=nodes[0].config.getOrNull(SemanticsProperties.VerticalScrollAxisRange);val max=range?.maxValue?.invoke()?:0f
        File(output,"fit.tsv").appendText("$tag\t${context.resources.configuration.screenWidthDp}\t$max\n")
        assertEquals("Unnecessary default scroll in $tag",0f,max,1f)
    }
    private fun fill(tag:String,text:String){compose.onNodeWithTag(tag).performTextReplacement(text)}
    private fun unlock(){navigate("app_info");repeat(7){tap("app_version",scroll=true)};tap("stage_unlock_confirm");waitScreen("demo_center")}

    @Test fun aRealNormalTransferAndReceipt(){
        base();fit("home");shot("functional_home")
        tap("home_transfer");fit("transfer_recipient");tap("recipient_seoyeon");fit("transfer_amount")
        tap("key_clear");tap("key_3");tap("key_2");repeat(3){tap("key_0")};tap("amount_next");fit("transfer_review")
        val before=state.balance;tap("transfer_confirm");waitScreen("auth_confirm");assertEquals(before,state.balance)
        shot("functional_authentication");tap("auth_confirm");waitScreen("transfer_complete");fit("transfer_complete")
        assertEquals(before-32000,state.balance);assertEquals(1,state.receipts.count{!it.seed})
        shot("functional_complete");tap("complete_receipt");waitScreen("receipt_expanded");compose.onNodeWithTag("receipt_collapse").assertIsDisplayed();shot("functional_receipt_sheet");tap("receipt_collapse")
        navigate("document/${state.receipts.first{!it.seed}.id}");waitScreen("document_detail");compose.onNodeWithTag("document_export").assertIsEnabled();shot("functional_document")
    }
    @Test fun bHiddenQuickDemosAndAllDecisions(){
        base();navigate("more");compose.onAllNodesWithTag("nav_presenter").assertCountEquals(0);unlock();shot("functional_demo_center")
        tap("demo_IMPERSONATION",scroll=true);fit("transfer_review");confirmThroughUi("trace_hold");fit("trace_hold");assertEquals(Fixtures.START_BALANCE,state.balance)
        compose.onAllNodesWithTag("transfer_confirm").assertCountEquals(0);shot("functional_hold");tap("hold_reasons_open",scroll=true);shot("functional_hold_reasons");tap("hold_reasons_close")
        evaluated(DemoScenario.WARN);shot("functional_warn");compose.onNodeWithTag("warn_acknowledge").assertIsNotEnabled();tap("warn_check",scroll=true);tap("warn_acknowledge",scroll=true);waitScreen("transfer_review");assertEquals(Fixtures.START_BALANCE,state.balance);confirmThroughUi("transfer_complete");assertEquals(Fixtures.START_BALANCE-120000,state.balance)
        evaluated(DemoScenario.LOAN);val old=state.currentTransferId;shot("functional_verify");tap("verify_route");waitScreen("trace_official_route");shot("functional_official_route");tap("official_new_transfer");waitScreen("transfer_review");assertNotEquals(old,state.currentTransferId);assertNull(state.current!!.authorization);confirmThroughUi("transfer_complete");assertEquals(0L,state.loanBalance)
        evaluated(DemoScenario.UNKNOWN);tap("verify_route");waitScreen("trace_unknown");shot("functional_unknown");assertEquals(Fixtures.START_BALANCE,state.balance)
        navigate("app_info");unlock();tap("demo_hide",scroll=true);tap("demo_hide_confirm");waitScreen("home");assertFalse(compose.activity.model.stage.value.unlocked)
    }
    @Test fun cLocalServicesAndPersistence(){
        base();navigate("schedules");tap("schedule_add");fill("schedule_name","연습 월세");fill("schedule_amount","200000");fill("schedule_day","26");tap("schedule_save");assertEquals(1,ServiceStore(context).rows("schedules").size);shot("functional_schedules")
        navigate("goals");val sum=state.balance+state.savings;tap("goal_deposit");fill("goal_amount","100000");tap("goal_confirm");compose.waitUntil(10000){state.savings==2500000L};assertEquals(sum,state.balance+state.savings);shot("functional_goal_saved")
        val stateBefore=state;runBlocking{repository.depositToSavings(100000,"duplicate-test");repository.depositToSavings(100000,"duplicate-test")};assertEquals(stateBefore.balance-100000,state.balance)
        navigate("account_lock");tap("account_lock_toggle");tap("account_lock_confirm");compose.waitUntil(10000){state.accountLocked};assertTrue(SnapshotCodec.decode(SnapshotCodec.encode(state)).accountLocked)
        try{runBlocking{repository.review(TransferDraft(Fixtures.seoyeon,1))};fail("Locked send accepted")}catch(e:BankFailure){assertEquals("ACCOUNT_LOCKED",e.code)}
        shot("functional_account_locked");tap("account_lock_toggle");tap("account_lock_confirm");compose.waitUntil(10000){!state.accountLocked}
        navigate("card_service");tap("card_lost");tap("card_lost_confirm");compose.waitUntil(10000){compose.activity.model.preferences.value.cardFrozen};tap("card_reissue");assertEquals("reissue",ServiceStore(context).text("card_status"));shot("functional_card_reissue")
        navigate("support_request");fill("support_body","시연 문의를 저장하고 내역을 확인합니다.");tap("support_save");assertEquals(1,ServiceStore(context).rows("requests").size);shot("functional_saved_inquiry")
        navigate("investments");tap("note_add");fill("note_text","장기 투자 공부");tap("note_save");assertEquals(1,ServiceStore(context).rows("watchlist").size)
        compose.activityRule.scenario.recreate();awaitReady();navigate("schedules");assertEquals(1,ServiceStore(context).rows("schedules").size);shot("functional_restored_schedules")
    }
    @Test fun dEveryDestinationLightAndDark(){
        base()
        val destinations=listOf("home","assets","account","savings","card","loan","bring","history","receipt/SIM-OPEN-03","transfer","recipients_all","recipient_entry","safety","pending","timeline","safety_guide","privacy","manual","more","profile","security","transfer_settings","favorites","schedules","notifications","appearance","accessibility","support","help","app_info","documents","document/balance","document/account","card_service","account_lock","spending","goals","investments","financial_notes","support_request","report","terms")
        for(mode in listOf("light","dark")) {
            theme(mode)
            destinations.forEachIndexed{index,route->
                navigate(route);shot("${mode}_${index.toString().padStart(2,'0')}_${route.replace('/','_')}")
                val scrollNodes=compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollBy),useUnmergedTree=true).fetchSemanticsNodes()
                val node=scrollNodes.firstOrNull{(it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.maxValue?.invoke()?:0f)>1f}
                if(node!=null){node.config.getOrNull(SemanticsActions.ScrollBy)?.action?.let{action->compose.runOnIdle{action(0f,100000f)}};Thread.sleep(200);shot("${mode}_${index.toString().padStart(2,'0')}_${route.replace('/','_')}_bottom")}
            }
            runBlocking{repository.setDraft(TransferDraft(Fixtures.seoyeon,32000,Purpose.SETTLEMENT))};navigate("amount");shot("${mode}_amount")
            tap("amount_edit");shot("${mode}_amount_edit");compose.onNodeWithText("취소").performClick()
            tap("transfer_purpose");shot("${mode}_purpose");compose.onNodeWithText("닫기").performClick()
            compose.runOnIdle{compose.activity.model.stageUnlock()};navigate("presenter");shot("${mode}_demo_center");tap("demo_more",scroll=true);shot("${mode}_demo_center_expanded");navigate("comparison");shot("${mode}_comparison");navigate("demo_lab");shot("${mode}_scenario_catalog")
        }
    }
    @Test fun ePolicyAndScenarioInventory(){
        for(mode in listOf("light","dark"))for(scenario in DemoScenario.entries) {
            evaluated(scenario);theme(mode)
            shot("${mode}_scenario_${scenario.name.lowercase()}_result")
            if(scenario==DemoScenario.IMPERSONATION){tap("hold_reasons_open",scroll=true);shot("${mode}_hold_reason_sheet");tap("hold_reasons_close");navigate("timeline");shot("${mode}_hold_timeline")}
            if(scenario==DemoScenario.LOAN){tap("verify_route");waitScreen("trace_official_route");shot("${mode}_official_route")}
            if(scenario==DemoScenario.UNKNOWN){tap("verify_route");waitScreen("trace_unknown");shot("${mode}_unknown")}
        }
        base();createReview(DemoScenario.NORMAL);theme("dark");shot("dark_review");theme("light");shot("light_review")
    }
    @Test fun fOrbitLayoutAndReducedMotion(){
        base()
        compose.runOnUiThread{compose.activity.setContent{SaeonTheme{Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)){BankContextLoading{}}}}}
        Thread.sleep(1100);fit("trace_evaluating");shot("light_orbit_loading_component")
        compose.runOnUiThread{compose.activity.setContent{SaeonTheme(dark=true,reduced=true){Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)){BankContextLoading{}}}}}
        Thread.sleep(500);fit("trace_evaluating");shot("dark_orbit_reduced_component")
    }
}
