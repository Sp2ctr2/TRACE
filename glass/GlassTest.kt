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

@RunWith(AndroidJUnit4::class)
class GlassTest:UiHarness(){
    @Test fun everyTabLensIsCentered(){
        fresh()
        val theme=InstrumentationRegistry.getArguments().getString("theme")?:"light"
        runBlocking{graph.preferences.theme(theme)}
        listOf("home","assets","transfer","safety","more").forEach{route->
            tap("nav_$route");Thread.sleep(900);compose.waitForIdle()
            val lens=compose.onNodeWithTag("glass_lens",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            val tab=compose.onNodeWithTag("nav_$route",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            assertEquals("Horizontal lens center $route",tab.center.x,lens.center.x,1.5f)
            assertEquals("Vertical lens center $route",tab.center.y,lens.center.y,1.5f)
            File(output,"dock-centers.tsv").appendText("$route\t${tab.center.x}\t${lens.center.x}\t${tab.center.y}\t${lens.center.y}\n")
            capture("glass_tab_$route",audit=false)
        }
    }
    @Test fun approvedHomeAndEasyHome(){
        fresh();Thread.sleep(700)
        compose.onNodeWithTag("home_transfer").assertIsDisplayed()
        compose.onAllNodesWithTag("home_bring").assertCountEquals(0)
        compose.onAllNodesWithText("보내기 전, 한 번 더.").assertCountEquals(0)
        capture("glass_home",audit=true)
        tap("home_hide_balance");compose.onNodeWithText("잔액 숨김").assertExists();capture("glass_balance_hidden",audit=false)
        tap("home_hide_balance");tap("home_account_picker");capture("glass_account_picker",audit=false);device.pressBack()
        runBlocking{graph.preferences.easy(true)}
        compose.waitUntil(10000){compose.activity.model.preferences.value.easyMode}
        compose.onNodeWithText("돈 보내기").assertExists()
        compose.onNodeWithText("도움 받기").assertExists()
        capture("glass_easy_home",audit=false)
    }
}
