package app.saeon.trace

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints

internal object TextBoundsAudit {
    fun withinDrawnBounds(reported: TextLayoutResult): TextLayoutResult {
        val input = reported.layoutInput
        // TextStringSimpleNode synthesizes semantics with a MultiParagraph using
        // its PARENT max width, but reports the original intrinsic layoutSize.
        // That flags an 82px "TRACE" label as overflowing a 634px empty paragraph.
        // Re-layout with the actual drawn size, retaining density, fonts, wrapping
        // and line limits. Real clipped text/ellipses remain failures; no tolerance
        // is introduced. AuditMeasurementTest includes negative controls.
        return TextMeasurer(input.fontFamilyResolver, input.density, input.layoutDirection, cacheSize = 0).measure(
            text = input.text, style = input.style, overflow = input.overflow,
            softWrap = input.softWrap, maxLines = input.maxLines, placeholders = input.placeholders,
            constraints = Constraints(maxWidth = reported.size.width, maxHeight = reported.size.height)
        )
    }
}
