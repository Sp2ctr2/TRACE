package app.saeon.trace

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.saeon.trace.core.*
import app.saeon.trace.data.ReadingMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LayoutMatrixTest : UiHarness() {
    private fun assertTextSingleLine(text: String) {
        val node = compose.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        assertTrue("Missing text measurement: $text", layouts.isNotEmpty())
        layouts.forEach {
            val actual = TextBoundsAudit.withinDrawnBounds(it)
            assertEquals("Unexpected wrapping: $text", 1, actual.lineCount)
            assertFalse("Clipped label: $text", actual.hasVisualOverflow)
        }
    }
    @Test fun bankingAndProtectionRespectCurrentConfiguration() {
        fresh(); capture("matrix_Home")
        if (context.resources.configuration.fontScale < 1.5f) {
            assertSingleLine("home_action_label_transfer")
            assertTextSingleLine("가져오기")
            assertTextSingleLine("내역")
        }
        state.receipts.take(3).forEach {
            assertSingleLine("receipt_name_${it.id}")
            assertSingleLine("receipt_amount_${it.id}")
        }
        navigate("history"); capture("matrix_History")
        runBlocking { repository.setDraft(TransferDraft(Fixtures.seoyeon, 32_000, Purpose.SETTLEMENT)) }
        navigate("amount"); capture("matrix_Amount")
        val next = compose.onNodeWithTag("amount_next")
        var ancestor = next.fetchSemanticsNode().parent
        var scrollable = false
        while (ancestor != null) {
            if (ancestor.config.contains(SemanticsActions.ScrollBy)) scrollable = true
            ancestor = ancestor.parent
        }
        if (scrollable) next.performScrollTo()
        next.assertIsDisplayed().assertIsEnabled()
        createReview(DemoScenario.NORMAL); capture("matrix_Review")
        evaluated(DemoScenario.WARN); capture("matrix_WARN")
        evaluated(DemoScenario.IMPERSONATION); capture("matrix_HOLD")
        evaluated(DemoScenario.EASY); capture("matrix_Easy_HOLD")
        runBlocking { graph.preferences.readingMode(ReadingMode.CHILD) }
        compose.waitUntil(10_000) { compose.activity.model.preferences.value.childMode }
        compose.waitForIdle(); capture("matrix_Child_HOLD")
        navigate("safety_guide"); capture("matrix_Child_Guide")
        fresh(); navigate("demo_center"); capture("matrix_Demo_Center")
    }
}
