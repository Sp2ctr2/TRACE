package dev.sp2ctr2.saeon

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dev.sp2ctr2.saeon.domain.*
import dev.sp2ctr2.saeon.data.BankRepository
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BankingJourneyTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val app get()=ui.activity.application as SaeonApplication
    private val device get()=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun ready(tag:String){ui.waitUntil(20_000){ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()};ui.waitForIdle()}
    private fun click(tag:String){ready(tag);val node=ui.onNodeWithTag(tag);runCatching{node.performScrollTo()};node.performClick();ui.waitForIdle()}
    private fun go(route:String,tag:String){ui.runOnIdle{ui.activity.model.go(route)};ready(tag)}
    private fun reset(scenario:Scenario=Scenario.NORMAL){runBlocking{app.repository.reset(scenario)};go("home","home_screen")}
    private fun capture(name:String){ui.waitForIdle();val dir=File(ui.activity.getExternalFilesDir(null),"screenshots").apply{mkdirs()};Assert.assertTrue(device.takeScreenshot(File(dir,"$name.png")))}
    private fun transfer(scenario:Scenario){reset(scenario);click("home_transfer");click("recipient_${Fixtures.defaultRecipient(scenario).id}");ready("transfer_amount");click("transfer_continue");ready("transfer_review")}
    private fun confirm(){click("transfer_confirm");click("auth_demo")}
    @Before fun prepare(){ui.waitUntil(20_000){app.repository.state.value!=null};runBlocking{app.preferences.set("easy",false);app.preferences.set("biometric",false);app.preferences.set("motion",true);app.preferences.set("haptics",false);app.preferences.set("hideBalance",false)};reset()}
    @Test fun a_normalBankingAndAllRootScreens(){
        capture("01_home");click("tab_assets");capture("02_assets");click("asset_primary_account");capture("03_account")
        go("transfer","transfer_recipient");capture("04_recipient");click("recipient_friend");capture("05_amount");click("transfer_continue");capture("06_review");confirm();ready("transfer_complete");capture("08_complete")
        val s=app.repository.state.value!!;Assert.assertEquals(12_808_000L,s.balance);Assert.assertEquals(1,s.receipts.size)
        click("complete_history");capture("21_history");go("receipt/${s.receipts.first().id}","receipt_detail");capture("24_receipt")
        go("home","home_screen");click("tab_more");capture("22_settings");go("demo","demo_lab");capture("23_demo_lab")
        go("notifications","notifications_screen");capture("25_notifications");go("schedules","schedules_screen");capture("26_schedules")
    }
    @Test fun b_holdReasonsTimelineAndRestoration(){
        transfer(Scenario.IMPERSONATION);confirm();ready("trace_hold");capture("10_hold")
        ui.onAllNodesWithText("그래도 계속하기").assertCountEquals(0)
        Assert.assertEquals(Fixtures.INITIAL_BALANCE,app.repository.state.value!!.balance)
        click("trace_hold_reason");capture("11_hold_reason");click("reason_timeline");capture("13_timeline")
        go("guide","trace_safety_guide");capture("12_safety_guide")
        go("safety","safety_center");capture("17_safety_center")
        ui.activityRule.scenario.recreate();ready("safety_center")
        Assert.assertTrue(app.repository.state.value!!.heldCase)
        go("result","trace_hold");ui.onNodeWithTag("trace_safety_action").assertIsDisplayed()
        val second=BankRepository(app);runBlocking{second.initialize()};Assert.assertTrue(second.state.value!!.heldCase);Assert.assertEquals(Phase.HOLD,second.state.value!!.draft!!.phase);second.close()
    }
    @Test fun c_loanUsesNewRecipientAndTransaction(){
        transfer(Scenario.LOAN);val old=app.repository.state.value!!.draft!!.id
        confirm();ready("trace_verify");capture("14_verify")
        click("route_lookup");ready("trace_official_route");capture("15_official_route")
        Assert.assertEquals(Fixtures.INITIAL_BALANCE,app.repository.state.value!!.balance)
        click("route_accept");ready("transfer_review");Assert.assertNotEquals(old,app.repository.state.value!!.draft!!.id)
        confirm();ready("transfer_complete");capture("27_loan_complete")
        Assert.assertEquals(4_840_000L,app.repository.state.value!!.balance);Assert.assertEquals(0L,app.repository.state.value!!.loan)
    }
    @Test fun d_unknownNeverDebits(){
        transfer(Scenario.LOOKUP_FAILURE);confirm();ready("trace_verify");click("route_lookup");ready("trace_unknown");capture("16_unknown")
        click("route_retry");ready("trace_unknown");Assert.assertEquals(Fixtures.INITIAL_BALANCE,app.repository.state.value!!.balance);Assert.assertTrue(app.repository.state.value!!.receipts.isEmpty())
    }
    @Test fun e_warnRechecksBeforeCompletion(){
        transfer(Scenario.WARN);confirm();ready("trace_warn");capture("09_warn");Assert.assertTrue(app.repository.state.value!!.receipts.isEmpty())
        click("warn_review");ready("transfer_review");confirm();ready("transfer_complete");Assert.assertEquals(1,app.repository.state.value!!.receipts.size)
    }
    @Test fun f_shareConsentPrivacyAndNoRawPersistence(){
        val raw="지금 안전계좌로 송금하세요 비밀원문123"
        ui.runOnIdle{ui.activity.model.receiveShared(raw)};ready("shared_text_review");capture("18_shared_text")
        Assert.assertTrue(app.repository.state.value!!.risks.isEmpty());click("share_analyze")
        ui.waitUntil(10_000){ui.activity.model.analysisResult.value!=null}
        Assert.assertFalse(app.repository.state.value.toString().contains("비밀원문123"));Assert.assertEquals("",ui.activity.model.sharedText.value)
        capture("28_analyzed_text");go("privacy","privacy_boundary");capture("19_privacy");click("privacy_bank");capture("29_privacy_bank")
        val permissions=app.packageManager.getPackageInfo(app.packageName,PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        Assert.assertFalse(permissions.contains("android.permission.INTERNET"));Assert.assertFalse(permissions.contains("android.permission.READ_SMS"));Assert.assertFalse(permissions.contains("android.permission.RECORD_AUDIO"))
    }
    @Test fun g_largeFontAndEasyMode(){
        transfer(Scenario.IMPERSONATION);confirm();ready("trace_hold")
        runBlocking{app.preferences.set("easy",true)};ui.waitForIdle();capture("20_easy_mode")
        device.executeShellCommand("settings put system font_scale 2.0")
        try {
            ui.activityRule.scenario.recreate();ready("trace_hold")
            ui.onNodeWithTag("trace_safety_action").assertIsDisplayed();capture("30_large_font_hold")
            go("home","home_screen");ui.onNodeWithTag("home_transfer").performScrollTo().assertIsDisplayed();capture("31_large_font_home")
        } finally {device.executeShellCommand("settings put system font_scale 1.0")}
    }
    @Test fun h_honestAnalysisProgressScreen(){
        val s=BankEngine.review(BankEngine.begin(Fixtures.seed(),Fixtures.friend,32_000,Fixtures.CLOCK))
        runBlocking{app.repository.update{ s.copy(draft=s.draft!!.copy(phase=Phase.EVALUATING)) }}
        go("result","trace_evaluating");capture("07_evaluating")
        // This capture is the actual native evaluation screen, held by a test fixture, not a measured latency claim.
        runBlocking{app.repository.initialize()};Assert.assertEquals(Phase.REVIEW,app.repository.state.value!!.draft!!.phase)
    }
    @Test fun i_amountValidationAndInternalMove(){
        transfer(Scenario.NORMAL);go("amount","transfer_amount");click("digit_clear");ui.onNodeWithTag("transfer_continue").assertIsNotEnabled()
        go("bring","bring_screen");click("bring_confirm");click("bring_execute");ready("account_detail")
        Assert.assertEquals(12_940_000L,app.repository.state.value!!.balance);Assert.assertEquals(2_300_000L,app.repository.state.value!!.savings)
        Assert.assertTrue(BankEngine.ledgerIsConsistent(app.repository.state.value!!))
    }
}
