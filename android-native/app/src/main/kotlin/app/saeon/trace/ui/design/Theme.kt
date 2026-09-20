package app.saeon.trace.ui.design

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.unit.sp

object TraceColors {
    val Paper = Color(0xFFF5F4F0)
    val Surface = Color(0xFFFBFAF7)
    val Ink = Color(0xFF20211F)
    val Muted = Color(0xFF676960)
    val OriginalMuted = Color(0xFF7D7E77)
    val Divider = Color(0xFFDDDDD5)
    val Coral = Color(0xFFEF4A32)
    val Deep = Color(0xFFD93B25)
    // 4.78:1 on paper; keep canonical Deep for the white-on-coral CTA.
    val AccentInk = Color(0xFFC93420)
    val CoralLight = Color(0xFFFAE7DF)
    val White = Color.White
}
val LocalEasyMode = staticCompositionLocalOf { false }
private fun style(size: Int, weight: FontWeight = FontWeight.Normal, line: Int = (size * 1.5).toInt()) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontSize = size.sp, fontWeight = weight,
    lineHeight = line.sp, letterSpacing = 0.sp, lineBreak = LineBreak.Paragraph, hyphens = Hyphens.None
)
@Composable
fun SaeonTheme(easy: Boolean = false, content: @Composable () -> Unit) {
    val body = if (easy) 19 else 16
    val type = Typography(
        displayLarge = style(42, FontWeight.Bold, 48), displayMedium = style(38, FontWeight.Bold, 44),
        displaySmall = style(34, FontWeight.Bold, 40),
        headlineLarge = style(if (easy) 32 else 30, FontWeight.Bold, if (easy) 43 else 40),
        headlineMedium = style(28, FontWeight.Bold, 38), headlineSmall = style(24, FontWeight.Bold, 34),
        titleLarge = style(24, FontWeight.Bold, 34), titleMedium = style(if (easy) 21 else 19, FontWeight.SemiBold, 29),
        titleSmall = style(body, FontWeight.SemiBold), bodyLarge = style(body), bodyMedium = style(if (easy) 18 else 15),
        bodySmall = style(if (easy) 16 else 13), labelLarge = style(if (easy) 19 else 16, FontWeight.SemiBold),
        labelMedium = style(if (easy) 15 else 13, FontWeight.Medium), labelSmall = style(if (easy) 14 else 12, FontWeight.Medium)
    )
    CompositionLocalProvider(LocalEasyMode provides easy) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = TraceColors.AccentInk, onPrimary = TraceColors.White,
                primaryContainer = TraceColors.CoralLight, onPrimaryContainer = TraceColors.Ink,
                secondary = TraceColors.Ink, onSecondary = TraceColors.White,
                secondaryContainer = TraceColors.Paper, onSecondaryContainer = TraceColors.Ink,
                tertiary = TraceColors.Ink, onTertiary = TraceColors.White,
                background = TraceColors.Paper, onBackground = TraceColors.Ink,
                surface = TraceColors.Surface, onSurface = TraceColors.Ink,
                surfaceVariant = TraceColors.Paper, onSurfaceVariant = TraceColors.Muted,
                outline = TraceColors.Muted, outlineVariant = TraceColors.Divider,
                error = TraceColors.AccentInk, onError = TraceColors.White,
                errorContainer = TraceColors.CoralLight, onErrorContainer = TraceColors.Ink,
                surfaceTint = TraceColors.Surface, scrim = TraceColors.Ink
            ), typography = type, content = content
        )
    }
}
