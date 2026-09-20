package app.saeon.trace

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.saeon.trace.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unlike the warm-share tests, the Activity is initially launched with SEND,
 * not MAIN. Its launch identity must survive payload removal and recreation. */
@RunWith(AndroidJUnit4::class)
class SharedLaunchTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun directShareLaunchAndRecreationWaitForConsent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val graph = (context.applicationContext as SaeonApplication).graph
        runBlocking { graph.repository.initialize(); graph.repository.reset(); graph.preferences.reset() }
        val sentinel = "SHARE_FIXTURE_D72"
        val incoming = Intent(context, MainActivity::class.java).setAction(Intent.ACTION_SEND)
            .setType("text/plain").putExtra(Intent.EXTRA_TEXT, "검찰입니다. 지금 입금하세요. $sentinel")
        ActivityScenario.launch<MainActivity>(incoming).use { scenario ->
            fun awaitConsentScreen() {
                compose.waitUntil(15_000) {
                    runCatching { compose.onAllNodesWithTag("shared_text_review").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
                }
                compose.waitForIdle()
            }
            awaitConsentScreen()
            assertTrue(graph.repository.state.value!!.events.isEmpty())
            scenario.onActivity { activity ->
                assertEquals(Intent.ACTION_SEND, activity.intent.action)
                assertNull(activity.intent.getStringExtra(Intent.EXTRA_TEXT))
                assertTrue(activity.safetyModel.state.value.text.contains(sentinel))
            }
            scenario.recreate()
            awaitConsentScreen()
            assertTrue(graph.repository.state.value!!.events.isEmpty())
            compose.onNodeWithTag("share_consent").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("manual_result").fetchSemanticsNodes().isNotEmpty() }
            assertTrue(graph.repository.state.value!!.events.any { it.type == RiskType.IMPERSONATION })
            val row = runBlocking { graph.database.snapshots().read()!! }
            assertFalse(row.payload.contains(sentinel))
            assertTrue(graph.repository.state.value!!.receipts.none { !it.seed })
        }
    }
}
