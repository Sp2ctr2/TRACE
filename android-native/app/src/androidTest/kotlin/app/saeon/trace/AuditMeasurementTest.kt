package app.saeon.trace

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import app.saeon.trace.ui.design.TraceColors
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuditMeasurementTest : UiHarness() {
    @Test fun functionalTextAndInputBoundariesMeetContrastThresholds() {
        fun contrast(a: Color, b: Color): Float {
            val light = maxOf(a.luminance(), b.luminance())
            val dark = minOf(a.luminance(), b.luminance())
            return (light + 0.05f) / (dark + 0.05f)
        }
        for (background in listOf(TraceColors.Paper, TraceColors.Surface, TraceColors.White)) {
            for (foreground in listOf(TraceColors.Ink, TraceColors.Muted, TraceColors.CoralText)) {
                assertTrue("Text contrast: $foreground / $background", contrast(foreground, background) >= 4.5f)
            }
            assertTrue("Input boundary contrast", contrast(TraceColors.InputOutline, background) >= 3f)
        }
        assertTrue("Primary button label contrast", contrast(TraceColors.White, TraceColors.Deep) >= 4.5f)
        assertEquals(Color(0xFFEF4A32), TraceColors.Coral)
        assertEquals(Color(0xFFD93B25), TraceColors.Deep)
    }
    private fun measurer() = TextMeasurer(createFontFamilyResolver(context), Density(2f), LayoutDirection.Ltr)
    @Test fun parentParagraphWidthIsNotMistakenForClippedGlyphs() {
        val text = AnnotatedString("TRACE")
        val style = TextStyle(fontSize = 16.sp)
        val compact = measurer().measure(text, style, constraints = Constraints(maxWidth = 600))
        val parent = measurer().measure(text, style, constraints = Constraints(minWidth = 600, maxWidth = 600))
        val reconstructedSemantics = parent.copy(size = compact.size)
        assertTrue(reconstructedSemantics.didOverflowWidth)
        assertFalse(TextBoundsAudit.withinDrawnBounds(reconstructedSemantics).hasVisualOverflow)
    }
    @Test fun realHeightWidthAndLineLimitClippingAreStillRejected() {
        val text = AnnotatedString("확인이 끝나기 전까지 송금은 진행되지 않습니다.")
        val style = TextStyle(fontSize = 20.sp)
        val heightClipped = measurer().measure(text, style, constraints = Constraints(maxWidth = 200, maxHeight = 8))
        assertTrue(TextBoundsAudit.withinDrawnBounds(heightClipped).hasVisualOverflow)
        val widthClipped = measurer().measure(text, style, softWrap = false, overflow = TextOverflow.Clip,
            constraints = Constraints(maxWidth = 30))
        assertTrue(TextBoundsAudit.withinDrawnBounds(widthClipped).hasVisualOverflow)
        val ellipsized = measurer().measure(text, style, maxLines = 1, overflow = TextOverflow.Ellipsis,
            constraints = Constraints(maxWidth = 60))
        assertTrue(TextBoundsAudit.withinDrawnBounds(ellipsized).hasVisualOverflow)
    }
}
