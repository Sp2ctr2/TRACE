package app.saeon.trace.ui.design

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun won(value: Long): String = NumberFormat.getIntegerInstance(Locale.KOREA).format(value)
fun timeLabel(value: Long): String = Instant.ofEpochMilli(value).atZone(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("HH:mm"))
fun dateLabel(value: Long): String = Instant.ofEpochMilli(value).atZone(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
@Composable fun Space(size: Int = 16) { Spacer(Modifier.height(size.dp)) }
@Composable fun Rule() { HorizontalDivider(color = TraceColors.Divider, thickness = 1.dp) }
@Composable fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodySmall, color = TraceColors.Muted)
}
@Composable fun Body(text: String, modifier: Modifier = Modifier, subdued: Boolean = false) {
    Text(text, modifier, style = MaterialTheme.typography.bodyLarge, color = if (subdued) TraceColors.Muted else TraceColors.Ink)
}
@Composable fun Headline(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.semantics { heading() }, style = MaterialTheme.typography.headlineLarge, color = TraceColors.Ink)
}
@Composable fun SectionTitle(text: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleMedium)
        if (action != null && onAction != null) QuietButton(action, onClick = onAction)
    }
}
@Composable fun AppIcon(icon: ImageVector, description: String? = null, tint: Color = TraceColors.Ink, size: Int = 24) {
    Icon(icon, description, Modifier.size(size.dp), tint = tint)
}
@Composable fun IconAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(onClick, modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = label }) {
        Icon(icon, null, Modifier.size(24.dp), tint = TraceColors.Ink)
    }
}
@Composable fun TraceSignature(label: String = "TRACE") {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(BankIcons.Trace, null, Modifier.size(23.dp), tint = TraceColors.Coral)
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TraceColors.Muted)
    }
}
@Composable fun Money(value: Long, modifier: Modifier = Modifier, prefix: String = "", hero: Boolean = true) {
    val scale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth().semantics { contentDescription = "$prefix${won(value)}원" }) {
        val digits = prefix + won(value)
        val base = if (hero) ((maxWidth.value - 30f * scale) / (digits.length.coerceAtLeast(1) * 0.59f * scale)).coerceIn(18f, 40f) else 24f
        Text(buildAnnotatedString {
            withStyle(SpanStyle(fontSize = base.sp, fontWeight = FontWeight.Bold)) { append(digits) }
            withStyle(SpanStyle(fontSize = (if (hero) 20 else 16).sp, fontWeight = FontWeight.Medium)) { append("원") }
        }, Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"), color = TraceColors.Ink)
    }
}
@Composable fun EmptyState(title: String, description: String) {
    Space(28); Text(title, style = MaterialTheme.typography.titleMedium); Space(12); Body(description, subdued = true); Space(24)
}
@Composable fun ErrorNote(message: String) {
    Text(message, Modifier.fillMaxWidth().padding(vertical = 12.dp).semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyMedium, color = TraceColors.AccentInk)
}
@Composable fun SimulationNote() { Caption("시연용 가상 거래 · 실제 자금 이동 없음") }
