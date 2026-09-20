package app.saeon.trace.ui.design

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp
import app.saeon.trace.R

object TraceColors {
    val Paper = Color(0xFFF5F4F0)
    val Surface = Color(0xFFFBFAF7)
    val SurfaceRaised = Color(0xFFFFFEFB)
    val Soft = Color(0xFFF0EFEA)
    val SoftStrong = Color(0xFFE8E6DE)
    val Ink = Color(0xFF20211F)
    val InkSoft = Color(0xFF43443F)
    val Muted = Color(0xFF676960)
    val OriginalMuted = Color(0xFF7D7E77)
    val Divider = Color(0xFFDDDDD5)
    val DividerStrong = Color(0xFFC7C6BE)
    val Coral = Color(0xFFEF4A32)
    val Deep = Color(0xFFD93B25)
    val CoralText = Color(0xFFC93623)
    val CoralLight = Color(0xFFFAE7DF)
    val CoralWash = Color(0xFFFFF1EC)
    val InputOutline = Color(0xFF8A8C83)
    val White = Color.White
}

val BankFont = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold)
)
val LocalEasyMode = staticCompositionLocalOf { false }
private fun type(size: Int, weight: FontWeight = FontWeight.Normal, line: Int = (size * 1.45f).toInt()) = TextStyle(
    fontFamily = BankFont, fontSize = size.sp, fontWeight = weight, lineHeight = line.sp,
    letterSpacing = 0.sp, localeList = LocaleList("ko-KR"),
    lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase), hyphens = Hyphens.None
)
@Composable fun SaeonTheme(easy: Boolean = false, content: @Composable () -> Unit) {
    val typography = Typography(
        displayLarge = type(40, FontWeight.SemiBold, 48),
        displayMedium = type(36, FontWeight.SemiBold, 44),
        displaySmall = type(30, FontWeight.SemiBold, 38),
        headlineLarge = type(if (easy) 30 else 28, FontWeight.SemiBold, if (easy) 41 else 37),
        headlineMedium = type(if (easy) 28 else 25, FontWeight.SemiBold, if (easy) 38 else 34),
        headlineSmall = type(if (easy) 24 else 22, FontWeight.SemiBold, if (easy) 33 else 30),
        titleLarge = type(21, FontWeight.SemiBold, 29),
        titleMedium = type(if (easy) 20 else 17, FontWeight.SemiBold, if (easy) 29 else 25),
        titleSmall = type(if (easy) 18 else 15, FontWeight.Medium, if (easy) 27 else 22),
        bodyLarge = type(if (easy) 19 else 15, line = if (easy) 29 else 23),
        bodyMedium = type(if (easy) 18 else 14, line = if (easy) 27 else 21),
        bodySmall = type(if (easy) 16 else 12, line = if (easy) 24 else 18),
        labelLarge = type(if (easy) 19 else 16, FontWeight.SemiBold, if (easy) 28 else 23),
        labelMedium = type(if (easy) 16 else 13, FontWeight.Medium, if (easy) 23 else 19),
        labelSmall = type(if (easy) 14 else 11, FontWeight.Medium, if (easy) 21 else 16)
    )
    CompositionLocalProvider(LocalEasyMode provides easy) {
        MaterialTheme(colorScheme = lightColorScheme(
            primary = TraceColors.Ink, onPrimary = TraceColors.White,
            primaryContainer = TraceColors.Soft, onPrimaryContainer = TraceColors.Ink,
            secondary = TraceColors.CoralText, onSecondary = TraceColors.White,
            secondaryContainer = TraceColors.CoralLight, onSecondaryContainer = TraceColors.Ink,
            tertiary = TraceColors.InkSoft, onTertiary = TraceColors.White,
            background = TraceColors.Surface, onBackground = TraceColors.Ink,
            surface = TraceColors.Surface, onSurface = TraceColors.Ink,
            surfaceVariant = TraceColors.Surface, onSurfaceVariant = TraceColors.Muted,
            outline = TraceColors.InputOutline, outlineVariant = TraceColors.Divider,
            error = TraceColors.CoralText, onError = TraceColors.White,
            errorContainer = TraceColors.CoralLight, onErrorContainer = TraceColors.Ink,
            surfaceTint = Color.Transparent, scrim = TraceColors.Ink
        ), typography = typography, content = content)
    }
}
