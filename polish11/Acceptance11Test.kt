package app.saeon.trace

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.saeon.trace.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File

/** UI/ledger checks only. Enrollment and authentication on a real device are a separate acceptance gate. */
@RunWith(AndroidJUnit4::class)
class Acceptance11Test:UiHarness(){
 private val theme get()=InstrumentationRegistry.getArguments().getString("theme")?:"light"
 private fun reset(){fresh();runBlocking{graph.preferences.theme(theme);compose.activity.model.services.resetFixtures()};compose.runOnIdle{compose.activity.model.uiTestAuthentication=true};compose.waitUntil(10000){compose.activity.model.preferences.value.themeMode==theme};Thread.sleep(850)}
 private fun shot(route:String,name:String){navigate(route);Thread.sleep(360);capture(name,audit=true)}
 private fun scenario(s:DemoScenario){
  runBlocking{repository.reset(s)};createReview(s);authorizeOnly();finish()
  waitScreen(when(s){DemoScenario.NORMAL,DemoScenario.EDUCATION,DemoScenario.UNRELATED,DemoScenario.NEW_ACCOUNT->"transfer_complete";DemoScenario.LOAN->"trace_official_route";DemoScenario.UNKNOWN->"trace_unknown";DemoScenario.WARN->"trace_warn";else->"trace_hold"})
 }
 @Test fun bankScreens(){
  reset()
  listOf("home","assets","account","savings","card","loan","bring","history","transfer","recipients_all","recipient_entry","safety","pending","timeline","safety_guide","privacy","manual","more").forEachIndexed{i,p->shot(p,"bank_${i.toString().padStart(2,'0')}_$p")}
  runBlocking{repository.setDraft(TransferDraft(Fixtures.seoyeon,32000,Purpose.SETTLEMENT))};shot("amount","bank_amount")
  tap("transfer_purpose",scroll=true);capture("bank_purpose_sheet",audit=false);device.pressBack()
  shot("receipt/${state.receipts.first().id}","bank_receipt")
 }
 @Test fun detailScreens(){
  reset()
  listOf("profile","security","transfer_settings","favorites","recurring","schedule_new","notifications","appearance","accessibility","support","inquiry","cases","help","app_info","investments","investment_detail","bond_detail","insurance","insurance_detail","credit","certificates","card_service","account_protection","terms").forEachIndexed{i,p->shot(p,"detail_${i.toString().padStart(2,'0')}_$p")}
  navigate("help");tap("help_0",scroll=true);capture("detail_help_expanded",audit=true)
  runBlocking{compose.activity.model.services.document("잔액 확인서","새온은행\n시연용 잔액 확인서\n12,840,000원\n증빙 효력 없음")};shot("document","detail_document")
 }
 @Test fun policies(){
  reset();createReview(DemoScenario.NORMAL);capture("transfer_review",audit=true);authorizeOnly();capture("transfer_gate",audit=true);finish();waitScreen("transfer_complete");capture("transfer_complete",audit=true)
  assertEquals(12808000L,state.balance);assertEquals(1,state.receipts.count{!it.seed});tap("complete_receipt",scroll=true);capture("transfer_receipt_sheet",audit=false);tap("receipt_collapse")
  scenario(DemoScenario.IMPERSONATION);capture("policy_hold",audit=true);assertEquals(Fixtures.START_BALANCE,state.balance)
  tap("hold_reasons_open",scroll=true);waitScreen("trace_timeline");capture("policy_context",audit=true);compose.onNodeWithTag("context_transfer").assertExists()
  scenario(DemoScenario.WARN);capture("policy_warn",audit=false);tap("warn_check",scroll=true);capture("policy_warn_checked",audit=false);tap("warn_acknowledge",scroll=true);waitScreen("transfer_review");assertNull(state.current!!.authorization)
  scenario(DemoScenario.LOAN);capture("policy_verify",audit=true);val old=state.currentTransferId;assertNotNull(state.current!!.route)
  tap("official_route_use",scroll=true);waitScreen("transfer_review");assertNotEquals(old,state.currentTransferId);assertNull(state.current!!.authorization);capture("official_new_review",audit=true)
  scenario(DemoScenario.UNKNOWN);capture("policy_unknown",audit=true);assertEquals(Fixtures.START_BALANCE,state.balance)
  compose.runOnIdle{compose.activity.model.routeDelayForTest=1700};runBlocking{repository.reset(DemoScenario.LOAN)};createReview(DemoScenario.LOAN);authorizeOnly();finish();waitScreen("loading_skeleton");capture("lookup_pending",audit=false);waitScreen("trace_official_route");compose.onAllNodesWithTag("loading_skeleton").assertCountEquals(0)
 }
 @Test fun navigationAndResize(){
  reset();compose.onNodeWithText("TRACE").assertExists()
  val a=compose.onNodeWithTag("home_bring").fetchSemanticsNode().boundsInRoot
  val b=compose.onNodeWithTag("home_transfer").fetchSemanticsNode().boundsInRoot
  assertEquals(a.width,b.width,1.5f);assertTrue(a.right<b.left)
  tap("home_hide_balance");compose.onAllNodesWithText("12,840,000원").assertCountEquals(0);capture("home_hidden",audit=true);tap("home_hide_balance")
  listOf("home","assets","transfer","safety","more").forEach{r->
   tap("nav_$r");Thread.sleep(850)
   val lens=compose.onNodeWithTag("bank_selection",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
   val tab=compose.onNodeWithTag("nav_$r",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
   assertEquals(r,tab.center.x,lens.center.x,1.5f);assertEquals(r,tab.center.y,lens.center.y,1.5f)
   File(output,"navigation.tsv").appendText("$r\t${tab.center.x-lens.center.x}\t${tab.center.y-lens.center.y}\n");capture("tab_$r",audit=true)
  }
  createReview(DemoScenario.NORMAL);val id=state.currentTransferId
  device.setOrientationLeft();Thread.sleep(1800);awaitReady();assertEquals(id,state.currentTransferId);capture("rotated_review",audit=true)
  shot("home","rotated_home");shot("assets","rotated_assets")
  device.setOrientationNatural();device.unfreezeRotation();Thread.sleep(1200)
  runBlocking{graph.preferences.easy(true)};compose.waitUntil(10000){compose.activity.model.preferences.value.easyMode};shot("home","easy_home");shot("safety_guide","easy_guide")
  runBlocking{graph.preferences.motion(true);graph.preferences.transparency(true)};shot("home","reduced_effects_home")
 }
 @Test fun servicesAndDemo(){
  reset();runBlocking{repository.bring(100000,"acceptance-bring-11")};assertEquals(12940000L,state.balance);assertEquals(2300000L,state.savings)
  runBlocking{repository.bring(100000,"acceptance-bring-11")};assertEquals(12940000L,state.balance)
  runBlocking{compose.activity.model.services.schedule("생활비",200000,25);compose.activity.model.services.addCase("송금 문의","송금 내역을 확인하고 싶습니다.")};shot("recurring","service_schedule");shot("cases","service_inquiry")
  navigate("card_service");tap("card_loss",scroll=true);tap("service_confirm");compose.waitUntil(10000){compose.activity.model.services.state.value.cardLost};capture("service_card_lost",audit=true)
  navigate("account_protection");tap("account_lock",scroll=true);tap("service_confirm");compose.waitUntil(10000){state.accountLocked};capture("service_locked",audit=true)
  compose.activityRule.scenario.recreate();awaitReady();assertTrue(state.accountLocked);assertEquals(12940000L,state.balance)
  navigate("app_info");repeat(7){tap("app_version",scroll=true)};tap("stage_unlock_confirm");waitScreen("demo_center");capture("demo_center",audit=true)
  tap("demo_more",scroll=true);capture("demo_all",audit=true);shot("comparison","demo_comparison")
  DemoScenario.entries.filter{it !in listOf(DemoScenario.NORMAL,DemoScenario.IMPERSONATION,DemoScenario.LOAN,DemoScenario.WARN,DemoScenario.UNKNOWN)}.forEach{s->scenario(s);capture("scenario_${s.name.lowercase()}",audit=false)}
 }
 @Test fun motion(){
  reset();tap("nav_assets");Thread.sleep(450);tap("nav_home");tap("home_transfer");tap("recipient_seoyeon",scroll=true)
  tap("key_clear",scroll=true);listOf("3","2","0","0","0").forEach{tap("key_$it",scroll=true)};tap("amount_next",scroll=true)
  waitScreen("transfer_review");tap("transfer_confirm");tap("auth_confirm");waitScreen("transfer_complete");assertEquals(12808000L,state.balance);capture("motion_complete",audit=true)
 }
}
