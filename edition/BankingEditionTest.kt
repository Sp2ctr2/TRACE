package app.saeon.trace

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.saeon.trace.core.*
import app.saeon.trace.data.SnapshotCodec
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BankingEditionTest:UiHarness() {
    @Before fun removeLauncherOnly(){device.executeShellCommand("am force-stop com.google.android.apps.nexuslauncher")}
    private fun idle(){compose.waitUntil(12000){!compose.activity.model.interaction.value.busy};compose.waitForIdle()}
    private fun screenshot(name:String){
        idle();Thread.sleep(280)
        assertFalse("System ANR must not contaminate screenshots",device.hasObject(androidx.test.uiautomator.By.textContains("isn't responding")))
        capture(name,audit=false)
        File(output,"screens.tsv").appendText("$name\t${context.resources.configuration.screenWidthDp}\t${compose.activity.model.preferences.value.themeMode}\n")
    }
    private fun bounds(tag:String){
        waitScreen(tag)
        val nodes=compose.onAllNodesWithTag("${tag}_body",useUnmergedTree=true).fetchSemanticsNodes()
        assertTrue("No bounded task body: $tag",nodes.isNotEmpty())
        val max=nodes[0].config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.maxValue?.invoke()?:0f
        File(output,"bounds.tsv").appendText("$tag\t${context.resources.configuration.screenWidthDp}\t$max\n")
        assertEquals("Task should fit: $tag",0f,max,1f)
    }
    private fun screen(route:String,tag:String,name:String){navigate(route);waitScreen(tag);screenshot(name)}
    private fun text(value:String){compose.onNodeWithText(value,useUnmergedTree=false).performScrollTo().performClick();idle()}
    private fun unlock(){
        compose.runOnIdle{compose.activity.model.stageLock()};navigate("app_info");repeat(7){tap("app_version")};tap("stage_unlock_confirm");waitScreen("demo_center")
    }
    @Test fun bankHomeAndQuickDemoActuallyWork(){
        fresh();compose.runOnIdle{compose.activity.model.stageLock()};bounds("home");screenshot("01_home")
        tap("home_balance_toggle");assertTrue(compose.activity.model.preferences.value.hideBalance);tap("home_balance_toggle")
        unlock();screenshot("02_hidden_demo_center")
        compose.onAllNodesWithTag("stage_clock").assertCountEquals(0)
        tap("demo_NORMAL");waitScreen("transfer_review");bounds("transfer_review");screenshot("03_review_normal")
        assertEquals(Fixtures.START_BALANCE,state.balance)
        confirmThroughUi("transfer_complete");bounds("transfer_complete");screenshot("04_complete")
        assertEquals(Fixtures.START_BALANCE-32000,state.balance)
        tap("complete_receipt");waitScreen("receipt_expanded");screenshot("05_receipt_sheet");tap("receipt_collapse")
        tap("complete_confirm");waitScreen("demo_center")
        tap("demo_IMPERSONATION");confirmThroughUi("trace_hold");bounds("trace_hold");screenshot("06_hold")
        assertEquals(Fixtures.START_BALANCE,state.balance)
        compose.onAllNodesWithTag("transfer_confirm").assertCountEquals(0)
        tap("hold_reasons_open");waitScreen("trace_hold_reason");screenshot("07_hold_reasons");tap("hold_reasons_close")
        navigate("presenter");tap("demo_lock",scroll=true);tap("demo_lock_confirm");waitScreen("home")
        assertFalse(compose.activity.model.stage.value.unlocked)
    }
    @Test fun distinguishWarnVerifyHoldAndUnknown(){
        evaluated(DemoScenario.WARN);waitScreen("trace_warn");screenshot("08_warn_unchecked")
        compose.onNodeWithTag("warn_acknowledge").assertIsNotEnabled()
        tap("warn_check",scroll=true);screenshot("09_warn_checked");tap("warn_acknowledge",scroll=true);waitScreen("transfer_review")
        assertEquals(Fixtures.START_BALANCE,state.balance)
        assertNull(state.current!!.authorization)
        confirmThroughUi("transfer_complete");assertEquals(Fixtures.START_BALANCE-120000,state.balance)
        evaluated(DemoScenario.LOAN);screenshot("10_verify")
        val old=state.currentTransferId!!;tap("verify_route");waitScreen("trace_official_route");screenshot("11_official_route")
        tap("official_route_use");waitScreen("transfer_review");screenshot("12_new_official_review")
        assertNotEquals(old,state.currentTransferId);assertNull(state.current!!.authorization)
        assertEquals(Fixtures.START_BALANCE,state.balance);confirmThroughUi("transfer_complete")
        assertEquals(0,state.loanBalance);screenshot("13_official_completion")
        evaluated(DemoScenario.UNKNOWN);tap("verify_route");waitScreen("trace_unknown");screenshot("14_unknown")
        tap("unknown_retry");waitScreen("trace_unknown");assertEquals(Fixtures.START_BALANCE,state.balance)
    }
    @Test fun localServicesPersistAndCannotSilentlySendMoney(){
        fresh();screen("certificates","certificates","15_certificates_empty")
        tap("issue_balance");waitScreen("certificate");screenshot("16_balance_certificate")
        val doc=compose.activity.model.serviceState.value.documents.first();assertEquals(Fixtures.START_BALANCE,doc.amount)
        screen("support","support","17_support")
        compose.onNodeWithTag("support_message").performScrollTo().performTextInput("송금 내역을 확인하고 싶어요.")
        tap("support_submit",scroll=true);waitScreen("support_receipt");screenshot("18_support_recorded")
        assertTrue(compose.activity.model.serviceState.value.requests.any{it.text=="송금 내역을 확인하고 싶어요."})
        screen("requests","requests","19_requests")
        screen("card_service","card_service","20_card_service")
        tap("card_lost",scroll=true);idle()
        assertTrue(compose.activity.model.preferences.value.cardFrozen)
        assertTrue(compose.activity.model.serviceState.value.cardLost)
        tap("card_reissue",scroll=true);screenshot("21_card_reissue_confirmation");tap("card_reissue_confirm");idle()
        assertTrue(compose.activity.model.serviceState.value.reissueRequested)
        screen("schedules","schedules","22_schedules")
        tap("schedule_add");waitScreen("schedule_form")
        compose.onNodeWithTag("schedule_name").performTextInput("정산 알림")
        compose.onNodeWithTag("schedule_amount").performTextInput("12000")
        compose.onNodeWithTag("schedule_day").performTextReplacement("26")
        device.pressBack();tap("schedule_save",scroll=true);idle();screenshot("23_schedule_saved")
        assertTrue(compose.activity.model.serviceState.value.schedules.any{it.name=="정산 알림"&&it.amount==12000L})
        assertEquals(Fixtures.START_BALANCE,state.balance)
        screen("savings_add","savings_add","24_savings_input")
        tap("savings_confirm");idle();screenshot("25_savings_moved")
        assertEquals(Fixtures.START_BALANCE-100000,state.balance);assertEquals(2500000,state.savings)
        screen("account_lock","account_lock","26_account_lock")
        tap("account_lock_action");screenshot("27_lock_confirmation");tap("account_lock_confirm");idle()
        assertTrue(state.accountLocked)
        assertTrue(SnapshotCodec.decode(SnapshotCodec.encode(state)).accountLocked)
        try{runBlocking{repository.review(TransferDraft(Fixtures.seoyeon,1000))};fail("Locked review accepted")}catch(e:BankFailure){assertEquals("ACCOUNT_LOCKED",e.code)}
        screenshot("28_account_locked")
        tap("account_lock_action");tap("account_lock_confirm");idle();assertFalse(state.accountLocked)
        compose.activityRule.scenario.recreate();awaitReady();assertFalse(state.accountLocked)
        assertTrue(compose.activity.model.serviceState.value.documents.any{it.id==doc.id})
    }
    @Test fun actualOrbitAndInterruptedEvaluation(){
        fresh();createReview(DemoScenario.NORMAL)
        tap("transfer_confirm");screenshot("29_authentication")
        compose.onNodeWithTag("auth_confirm").performClick()
        compose.waitUntil(5000){state.current?.stage==TransferStage.EVALUATING}
        // Capture from the real transaction while the ViewModel delay is active.
        val frame=instrumentation.uiAutomation.takeScreenshot()
        File(output,"30_orbit_actual.png").outputStream().use{frame.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};frame.recycle()
        compose.waitUntil(5000){state.current?.stage==TransferStage.COMPLETE}
        assertEquals(Fixtures.START_BALANCE-32000,state.balance)
        fresh();createReview(DemoScenario.NORMAL);authorizeOnly();bounds("trace_evaluating")
        screenshot("31_orbit_component_in_real_evaluating_state")
        val b=instrumentation.uiAutomation.takeScreenshot();Thread.sleep(220);val c=instrumentation.uiAutomation.takeScreenshot()
        assertFalse("Orbit frame must change while evaluating",b.sameAs(c));b.recycle();c.recycle()
        tap("gate_cancel");Thread.sleep(1600);assertEquals(Fixtures.START_BALANCE,state.balance)
    }
    @Test fun allScreensLightAndDarkInventory(){
        val routes=listOf(
            Triple("home","home","home"),Triple("assets","assets","assets"),Triple("account","account_detail","account"),
            Triple("savings","savings_detail","savings"),Triple("card","card_detail","card"),Triple("loan","loan_detail","loan"),
            Triple("bring","bring","bring"),Triple("history","history","history"),Triple("receipt/SIM-OPEN-03","receipt","receipt"),
            Triple("transfer","transfer_recipient","recipients"),Triple("recipients_all","transfer_recipient","recipient_directory"),
            Triple("recipient_entry","recipient_entry","account_entry"),Triple("safety","safety","safety"),
            Triple("pending","pending_history","pending"),Triple("timeline","trace_timeline","timeline"),Triple("safety_guide","trace_safety_guide","guide"),
            Triple("privacy","privacy_boundary","privacy"),Triple("manual","shared_text_review","manual"),Triple("more","settings","more"),
            Triple("profile","profile","profile"),Triple("security","security_settings","security"),Triple("transfer_settings","transfer_settings","transfer_settings"),
            Triple("favorites","favorite_accounts","favorites"),Triple("recurring","recurring","recurring"),Triple("notifications","notifications","notifications"),
            Triple("appearance","appearance","appearance"),Triple("accessibility","easy_mode_settings","accessibility"),Triple("help","help","help"),
            Triple("app_info","app_info","app_info"),Triple("certificates","certificates","certificates"),Triple("certificate","certificate","certificate"),
            Triple("account_lock","account_lock","account_lock"),Triple("card_service","card_service","card_service"),Triple("schedules","schedules","schedules"),
            Triple("savings_add","savings_add","savings_add"),Triple("support","support","support"),Triple("report","report","report"),Triple("requests","requests","requests"),
            Triple("investments","investments","investments"),Triple("investment_detail","investment_detail","investment_detail"),Triple("credit","credit","credit"),
            Triple("insurance","insurance","insurance"),Triple("spending","spending","spending"),Triple("terms","terms","terms"))
        for(theme in listOf("light","dark")) {
            fresh();runBlocking{graph.preferences.theme(theme);compose.activity.model.services.issue("balance",state.balance)}
            compose.waitUntil(10000){compose.activity.model.preferences.value.themeMode==theme}
            routes.forEachIndexed{index,(route,tag,name)->screen(route,tag,"inventory_${theme}_${index.toString().padStart(2,'0')}_$name")}
            compose.runOnIdle{compose.activity.model.stageUnlock()}
            screen("presenter","demo_center","inventory_${theme}_44_demo_center")
            text("모두 보기");screenshot("inventory_${theme}_45_demo_expanded")
            // Scroll to the actual lower section so all demo choices are inspectable.
            tap("demo_compare",scroll=true);waitScreen("comparison");screenshot("inventory_${theme}_46_comparison")
            screen("demo_lab","demo_lab","inventory_${theme}_47_scenario_details")
            for(s in DemoScenario.entries) {
                runBlocking{repository.reset(s);graph.preferences.theme(theme);graph.preferences.easy(s==DemoScenario.EASY)}
                createReview(s);screenshot("scenario_${theme}_${s.name}_review")
                authorizeOnly();finish()
                if(s==DemoScenario.UNKNOWN){runBlocking{repository.resolveRoute(state.currentTransferId!!)}}
                screenshot("scenario_${theme}_${s.name}_result")
                if(s in listOf(DemoScenario.NORMAL,DemoScenario.NEW_ACCOUNT,DemoScenario.EDUCATION,DemoScenario.UNRELATED))assertEquals(Fixtures.START_BALANCE-Fixtures.amount(s),state.balance)
                else assertEquals(Fixtures.START_BALANCE,state.balance)
            }
        }
    }
}
