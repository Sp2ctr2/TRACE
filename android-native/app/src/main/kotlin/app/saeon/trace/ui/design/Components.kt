package app.saeon.trace.ui.design

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun won(value: Long): String = NumberFormat.getIntegerInstance(Locale.KOREA).format(value)
fun timeLabel(value: Long): String = Instant.ofEpochMilli(value).atZone(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("HH:mm"))
fun dateLabel(value: Long): String = Instant.ofEpochMilli(value).atZone(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
@Composable fun Space(size: Int = 16) { Spacer(Modifier.height(size.dp)) }
@Composable fun Rule(modifier: Modifier = Modifier) { HorizontalDivider(modifier, color = TraceColors.Divider, thickness = 1.dp) }
@Composable fun AccentRule(width: Int = 30) { Box(Modifier.width(width.dp).height(2.dp).background(TraceColors.Coral)) }
@Composable fun MicroLabel(text: String, modifier: Modifier = Modifier, coral: Boolean = false) {
    Text(text, modifier, style = MaterialTheme.typography.labelSmall, color = if (coral) TraceColors.CoralText else TraceColors.Muted)
}
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
    if (LocalDensity.current.fontScale >= 1.3f && action != null && onAction != null) {
        Column(Modifier.fillMaxWidth()) {
            Text(text, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
            QuietButton(action, onClick = onAction)
        }
    } else Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleMedium)
        if (action != null && onAction != null) QuietButton(action, onClick = onAction)
    }
}
@Composable fun AppIcon(icon: ImageVector, description: String? = null, tint: Color = TraceColors.Ink, size: Int = 24) {
    Icon(icon, description, Modifier.size(size.dp), tint = tint)
}
@Composable fun IconAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(onClick, modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = label }) {
        Icon(icon, null, Modifier.size(22.dp), tint = TraceColors.Ink)
    }
}
@Composable fun TraceSignature(label: String = "trace") {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(BankIcons.Trace, null, Modifier.size(27.dp), tint = TraceColors.Coral)
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TraceColors.Ink)
    }
}
@Composable fun SaeonWordmark(modifier: Modifier = Modifier) {
    Text("새온은행", modifier, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}
@Composable fun Page(title: String = "", tag: String = "", back: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null, footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag(tag), contentAlignment = Alignment.TopCenter) {
        val compact = maxHeight < 480.dp
        val pageScroll = rememberScrollState()
        val bodyScroll = rememberScrollState()
        Column(Modifier.widthIn(max = 620.dp).fillMaxSize().then(if (compact) Modifier.verticalScroll(pageScroll) else Modifier)) {
            if (title.isNotEmpty() || back != null || actions != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = if (back != null) 8.dp else 24.dp)
                    .heightIn(min = 58.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (back != null) IconAction(BankIcons.Back, "이전 화면", onClick = back)
                    if (back == null && title == "새온은행") SaeonWordmark(Modifier.weight(1f))
                    else Text(title, Modifier.weight(1f).padding(vertical = 10.dp).semantics { heading() },
                        style = if (back == null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall)
                    actions?.invoke(this)
                }
            }
            val bodyModifier = if (compact) Modifier else Modifier.weight(1f).verticalScroll(bodyScroll)
            Column(bodyModifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 10.dp, bottom = 24.dp), content = content)
            if (footer != null) Column(Modifier.fillMaxWidth().background(TraceColors.Surface).padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp), content = footer)
        }
    }
}
@Composable fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, accent: Boolean = false, onClick: () -> Unit) {
    Button(onClick, modifier.fillMaxWidth().heightIn(min = if (LocalEasyMode.current) 60.dp else 56.dp), enabled = enabled,
        shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TraceColors.Deep, contentColor = TraceColors.White,
            disabledContainerColor = TraceColors.Divider, disabledContentColor = TraceColors.Muted),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}
@Composable fun QuietButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(onClick, modifier.heightIn(min = 48.dp), enabled = enabled, shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = TraceColors.Ink)) {
        Text(text, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}
@Composable fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    QuietButton(text, modifier.fillMaxWidth(), onClick = onClick)
}
@Composable fun Field(value: String, label: String, onChange: (String) -> Unit, modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text, minLines: Int = 1, maxLines: Int = 1, enabled: Boolean = true) {
    OutlinedTextField(value, onChange, modifier.fillMaxWidth(), label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        singleLine = maxLines == 1, minLines = minLines, maxLines = maxLines, enabled = enabled,
        textStyle = MaterialTheme.typography.bodyLarge, shape = RoundedCornerShape(10.dp), keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TraceColors.Ink, unfocusedBorderColor = TraceColors.InputOutline,
            focusedContainerColor = TraceColors.Surface, unfocusedContainerColor = TraceColors.Surface,
            cursorColor = TraceColors.CoralText, focusedLabelColor = TraceColors.Ink, unfocusedLabelColor = TraceColors.Muted))
}
@Composable fun Money(value: Long, modifier: Modifier = Modifier, prefix: String = "", hero: Boolean = true) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val type = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum", lineHeight = if (hero) 48.sp else 36.sp)
    BoxWithConstraints(modifier.fillMaxWidth().semantics { contentDescription = "$prefix${won(value)}원" }) {
        val width = with(density) { maxWidth.roundToPx() }
        val number = prefix + won(value)
        val label = remember(number, width, hero, density.density, density.fontScale, type) {
            var chosen = buildAnnotatedString { append(number + "원") }
            for (size in (if (hero) 38 else 28) downTo 16) {
                val candidate = buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = size.sp, fontWeight = FontWeight.Bold)) { append(number) }
                    withStyle(SpanStyle(fontSize = (size * 0.53f).sp, fontWeight = FontWeight.Medium)) { append("원") }
                }
                chosen = candidate
                if (!measurer.measure(candidate, type, softWrap = false, maxLines = 1,
                    constraints = Constraints(maxWidth = width)).hasVisualOverflow) break
            }
            chosen
        }
        Text(label, Modifier.clearAndSetSemantics {}, style = type, softWrap = false, maxLines = 1, color = TraceColors.Ink)
    }
}
@Composable fun DetailRow(label: String, value: String, emphasize: Boolean = false) {
    val weight = if (emphasize) FontWeight.SemiBold else FontWeight.Medium
    if (LocalDensity.current.fontScale >= 1.3f) Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Caption(label); Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = weight)
    } else Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(label, Modifier.weight(0.85f), style = MaterialTheme.typography.bodySmall, color = TraceColors.Muted)
        Text(value, Modifier.weight(1.55f), style = MaterialTheme.typography.bodyMedium, fontWeight = weight, textAlign = TextAlign.End)
    }
}
@Composable fun MenuRow(title: String, subtitle: String? = null, icon: ImageVector? = null, tag: String = "", trailing: String? = null, onClick: (() -> Unit)? = null) {
    val large = LocalDensity.current.fontScale >= 1.3f
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
        .testTag(tag).heightIn(min = 64.dp).padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        if (icon != null) AppIcon(icon, size = 21, tint = TraceColors.InkSoft)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (!subtitle.isNullOrEmpty()) { Space(4); Caption(subtitle) }
            if (large && trailing != null) { Space(6); Text(trailing, style = MaterialTheme.typography.bodyMedium) }
        }
        if (!large && trailing != null) Text(trailing, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        if (onClick != null) AppIcon(BankIcons.Chevron, size = 17, tint = TraceColors.Muted)
    }
}
@Composable fun SurfaceBox(modifier: Modifier = Modifier, tint: Color = TraceColors.Surface, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().background(tint, RoundedCornerShape(12.dp)).border(1.dp, TraceColors.Divider, RoundedCornerShape(12.dp)).padding(18.dp), content = content)
}
@Composable fun EditorialPanel(modifier: Modifier = Modifier, accent: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(2.dp).fillMaxHeight().background(if (accent) TraceColors.Coral else TraceColors.Divider))
        Column(Modifier.weight(1f).padding(start = 16.dp), content = content)
    }
}
@Composable fun RecipientGlyph(name: String, modifier: Modifier = Modifier) {
    val scale = LocalDensity.current.fontScale
    Box(modifier.size(if (scale >= 1.75f) 64.dp else if (scale >= 1.4f) 52.dp else 40.dp)
        .clip(CircleShape).background(TraceColors.Paper), contentAlignment = Alignment.Center) {
        Text(name.firstOrNull()?.toString().orEmpty(), style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
    }
}
@Composable fun NumberedReason(number: Int, title: String, description: String? = null, accent: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(number.toString().padStart(2, '0'), style = MaterialTheme.typography.labelMedium, color = if (accent) TraceColors.CoralText else TraceColors.Muted)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (description != null) { Space(5); Caption(description) }
        }
    }
}
@Composable fun EmptyState(title: String, description: String) {
    Space(24); Text(title, style = MaterialTheme.typography.titleMedium); Space(10); Body(description, subdued = true); Space(22)
}
@Composable fun ErrorNote(message: String) {
    Text(message, Modifier.fillMaxWidth().padding(vertical = 10.dp).semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodyMedium, color = TraceColors.CoralText)
}
@Composable fun SimulationNote() { Caption("시연용 가상 거래 · 실제 자금 이동 없음") }
@Composable fun OptionRow(label: String, checked: Boolean, description: String? = null, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
        .semantics { stateDescription = if (checked) "켜짐" else "꺼짐" }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) { Body(label); if (description != null) { Space(4); Caption(description) } }
        Switch(checked, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics {},
            colors = SwitchDefaults.colors(checkedThumbColor = TraceColors.White, checkedTrackColor = TraceColors.Ink))
    }
}
