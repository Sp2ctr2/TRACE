package app.saeon.trace

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the auditor itself so a false-positive fix cannot hide measured text clipping.
 * The contract is TextLayoutResult/semantics clipping; arbitrary ancestor clipToBounds
 * is outside this metric and is covered by screenshot/layout audits instead.
 */
@RunWith(AndroidJUnit4::class)
class TextBoundsAuditTest {
    @get:Rule val compose = createComposeRule()
    @Test fun occupiedLineAuditAcceptsWhitespaceButRejectsMeasuredClipping() {
        compose.setContent {
            Column(Modifier.width(300.dp)) {
                Text("확인", Modifier.testTag("audit_short"))
                Text("This sentence must not fit into forty dp.",
                    Modifier.width(40.dp).testTag("audit_horizontal"), softWrap = false, overflow = TextOverflow.Clip)
                Text("첫 번째 줄\n두 번째 줄", Modifier.testTag("audit_omitted"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("첫 번째 줄\n두 번째 줄", Modifier.testTag("audit_vertical"), maxLines = 1, overflow = TextOverflow.Clip)
            }
        }
        fun read(tag: String): TextOverflowMetric {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals("Text layout must be observable", 1, layouts.size)
            val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
            return textOverflow(layouts.single(), node.size)
        }
        assertFalse("Unoccupied parent width is not text overflow", read("audit_short").exceedsBounds)
        assertTrue("Real horizontal clipping must fail", read("audit_horizontal").exceedsBounds)
        assertTrue("Omitted/ellipsized lines must fail", read("audit_omitted").exceedsBounds)
        assertTrue("Line-clipped text without ellipsis must fail", read("audit_vertical").exceedsBounds)
    }
}
