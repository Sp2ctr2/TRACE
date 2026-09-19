package app.saeon.trace.ui.design

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import app.saeon.trace.core.*
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
        Icon(icon, contentDescription = null, tint = TraceColors.Ink, modifier = Modifier.size(24.dp))
    }
}
@Composable fun TraceSignature(label: String = "TRACE") {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(BankIcons.Trace, null, Modifier.size(23.dp), tint = TraceColors.Coral)
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TraceColors.Muted)
    }
}
@Composable fun Page(
    title: String = "", tag: String = "", back: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(Modifier.fillMaxSize().testTag(tag), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = 600.dp)) {
            if (title.isNotEmpty() || back != null || actions != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = if (back != null) 8.dp else 20.dp).heightIn(min = 60.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (back != null) IconAction(BankIcons.Back, "이전 화면", onClick = back)
                    Text(title, Modifier.weight(1f).padding(vertical = 12.dp).semantics { heading() },
                        style = if (back == null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall)
                    actions?.invoke(this)
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).padding(top = if (title.isEmpty()) 20.dp else 12.dp, bottom = 28.dp), content = content)
            if (footer != null) {
                Column(Modifier.fillMaxWidth().background(TraceColors.Paper).padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = footer)
            }
        }
    }
}
@Composable fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = enabled,
        shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TraceColors.Deep, contentColor = TraceColors.White,
            disabledContainerColor = TraceColors.Divider, disabledContentColor = TraceColors.Muted)) {
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}
@Composable fun QuietButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled,
        shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.textButtonColors(contentColor = TraceColors.Ink),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}
@Composable fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    QuietButton(text, modifier.fillMaxWidth(), onClick = onClick)
}
@Composable fun Field(value: String, label: String, onChange: (String) -> Unit, modifier: Modifier = Modifier,
                      keyboard: KeyboardType = KeyboardType.Text, minLines: Int = 1, maxLines: Int = 1, enabled: Boolean = true) {
    OutlinedTextField(value = value, onValueChange = onChange, modifier = modifier.fillMaxWidth(),
        label = { Text(label) }, singleLine = maxLines == 1, minLines = minLines, maxLines = maxLines,
        enabled = enabled, textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TraceColors.Deep,
            unfocusedBorderColor = TraceColors.Divider, focusedContainerColor = TraceColors.Surface,
            unfocusedContainerColor = TraceColors.Surface, cursorColor = TraceColors.Deep))
}
@Composable fun Money(value: Long, modifier: Modifier = Modifier, prefix: String = "", hero: Boolean = true) {
    val scale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth().semantics { contentDescription = "$prefix${won(value)}원" }) {
        val digits = prefix + won(value)
        val base = if (hero) ((maxWidth.value - 30f * scale) / (digits.length.coerceAtLeast(1) * 0.59f * scale)).coerceIn(18f, 40f) else 24f
        Text(buildAnnotatedString {
            withStyle(SpanStyle(fontSize = base.sp, fontWeight = FontWeight.Bold)) { append(digits) }
            append(" ")
            withStyle(SpanStyle(fontSize = (if (hero) 20 else 16).sp, fontWeight = FontWeight.Medium)) { append("원") }
        }, modifier = Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
            color = TraceColors.Ink, softWrap = true)
    }
}
@Composable fun DetailRow(label: String, value: String, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Top) {
        Text(label, Modifier.weight(0.9f), style = MaterialTheme.typography.bodyMedium, color = TraceColors.Muted)
        Text(value, Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Medium, textAlign = TextAlign.End)
    }
}
@Composable fun MenuRow(title: String, subtitle: String? = null, icon: ImageVector? = null,
                        tag: String = "", trailing: String? = null, onClick: (() -> Unit)? = null) {
    val clickModifier = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(Modifier.fillMaxWidth().then(clickModifier).testTag(tag).heightIn(min = 68.dp).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        if (icon != null) AppIcon(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (!subtitle.isNullOrEmpty()) { Space(4); Caption(subtitle) }
        }
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        if (onClick != null) AppIcon(BankIcons.Chevron, size = 18, tint = TraceColors.Muted)
    }
}
@Composable fun SurfaceBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().background(TraceColors.Surface, RoundedCornerShape(12.dp)).padding(18.dp), content = content)
}
@Composable fun NumberedReason(number: Int, title: String, description: String? = null, accent: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
        Text(number.toString().padStart(2, '0'), Modifier.widthIn(min = 25.dp), style = MaterialTheme.typography.labelMedium,
            color = if (accent) TraceColors.Deep else TraceColors.Muted)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (description != null) { Space(5); Caption(description) }
        }
    }
}
@Composable fun EmptyState(title: String, description: String) {
    Space(28); Text(title, style = MaterialTheme.typography.titleMedium); Space(12); Body(description, subdued = true); Space(24)
}
@Composable fun ErrorNote(message: String) {
    Text(message, Modifier.fillMaxWidth().padding(vertical = 12.dp).semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyMedium, color = TraceColors.Deep)
}
@Composable fun SimulationNote() { Caption("시연용 가상 거래 · 실제 자금 이동 없음") }
@Composable fun OptionRow(label: String, checked: Boolean, description: String? = null, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(role = Role.Switch) { onChange(!checked) }
        .semantics { stateDescription = if (checked) "켜짐" else "꺼짐" }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) { Body(label); if (description != null) { Space(4); Caption(description) } }
        Switch(checked, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics {},
            colors = SwitchDefaults.colors(checkedThumbColor = TraceColors.White, checkedTrackColor = TraceColors.Ink))
    }
}
