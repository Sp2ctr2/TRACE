package app.saeon.trace.ui.design

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp

/**
 * TRACE website-derived warm editorial palette.
 * Coral is intentionally not the default banking action color: normal banking
 * stays quiet/ink-first, while TRACE intervention earns coral emphasis.
 */
object TraceColors {
    val Paper = Color(0xFFF5F4F0)
    val Surface = Color(0xFFFBFAF7)
    val SurfaceRaised = Color(0xFFFFFFFF)
    val Soft = Color(0xFFEEEDE7)
    val SoftStrong = Color(0xFFE8E6DE)
    val Ink = Color(0xFF20211F)
    val InkSoft = Color(0xFF43443F)
    val Muted = Color(0xFF70716A)
    val OriginalMuted = Color(0xFF7D7E77)
    val Divider = Color(0xFFDDDCD5)
    val DividerStrong = Color(0xFFC7C6BE)
    val Coral = Color(0xFFEF4A32)
    val Deep = Color(0xFFD93B25)
    val CoralText = Color(0xFFC93623)
    val CoralLight = Color(0xFFFAE7DF)
    val CoralWash = Color(0xFFFFF1EC)
    val InputOutline = Color(0xFF8A8C83)
    val White = Color.White
}

val LocalEasyMode = staticCompositionLocalOf { false }

private fun editorialType(
    size: Int,
    weight: FontWeight = FontWeight.Normal,
    line: Int = (size * 1.42f).toInt()
) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = size.sp,
    fontWeight = weight,
    lineHeight = line.sp,
    letterSpacing = 0.sp,
    localeList = LocaleList("ko-KR"),
    lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase),
    hyphens = Hyphens.None
)

@Composable
fun SaeonTheme(easy: Boolean = false, content: @Composable () -> Unit) {
    val body = if (easy) 19 else 16
    val type = Typography(
        displayLarge = editorialType(if (easy) 40 else 38, FontWeight.SemiBold, if (easy) 48 else 44),
        displayMedium = editorialType(if (easy) 36 else 34, FontWeight.SemiBold, if (easy) 44 else 40),
        displaySmall = editorialType(if (easy) 32 else 30, FontWeight.SemiBold, if (easy) 40 else 36),
        headlineLarge = editorialType(if (easy) 31 else 28, FontWeight.SemiBold, if (easy) 41 else 37),
        headlineMedium = editorialType(if (easy) 28 else 25, FontWeight.SemiBold, if (easy) 38 else 34),
        headlineSmall = editorialType(if (easy) 24 else 22, FontWeight.SemiBold, if (easy) 34 else 31),
        titleLarge = editorialType(if (easy) 23 else 21, FontWeight.SemiBold, if (easy) 33 else 29),
        titleMedium = editorialType(if (easy) 20 else 18, FontWeight.SemiBold, if (easy) 29 else 26),
        titleSmall = editorialType(if (easy) 18 else 16, FontWeight.Medium, if (easy) 27 else 24),
        bodyLarge = editorialType(body, FontWeight.Normal, if (easy) 29 else 24),
        bodyMedium = editorialType(if (easy) 17 else 15, FontWeight.Normal, if (easy) 26 else 22),
        bodySmall = editorialType(if (easy) 15 else 13, FontWeight.Normal, if (easy) 23 else 19),
        labelLarge = editorialType(if (easy) 18 else 16, FontWeight.SemiBold, if (easy) 26 else 22),
        labelMedium = editorialType(if (easy) 15 else 13, FontWeight.Medium, if (easy) 22 else 18),
        labelSmall = editorialType(if (easy) 13 else 11, FontWeight.Medium, if (easy) 19 else 16)
    )
    CompositionLocalProvider(LocalEasyMode provides easy) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = TraceColors.Ink,
                onPrimary = TraceColors.White,
                primaryContainer = TraceColors.Soft,
                onPrimaryContainer = TraceColors.Ink,
                secondary = TraceColors.CoralText,
                onSecondary = TraceColors.White,
                secondaryContainer = TraceColors.CoralLight,
                onSecondaryContainer = TraceColors.Ink,
                tertiary = TraceColors.InkSoft,
                onTertiary = TraceColors.White,
                background = TraceColors.Paper,
                onBackground = TraceColors.Ink,
                surface = TraceColors.Surface,
                onSurface = TraceColors.Ink,
                surfaceVariant = TraceColors.Soft,
                onSurfaceVariant = TraceColors.Muted,
                outline = TraceColors.InputOutline,
                outlineVariant = TraceColors.Divider,
                error = TraceColors.CoralText,
                onError = TraceColors.White,
                errorContainer = TraceColors.CoralLight,
                onErrorContainer = TraceColors.Ink,
                surfaceTint = Color.Transparent,
                scrim = TraceColors.Ink
            ),
            typography = type,
            content = content
        )
    }
}
