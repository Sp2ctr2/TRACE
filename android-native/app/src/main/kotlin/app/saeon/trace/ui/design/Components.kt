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
import androidx.compose.ui.text.style.TextOverflow
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
    Text(text, modifier, style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold, color = if (coral) TraceColors.CoralText else TraceColors.Muted)
}

@Composable fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodySmall, color = TraceColors.Muted)
}

@Composable fun Body(text: String, modifier: Modifier = Modifier, subdued: Boolean = false) {
    Text(text, modifier, style = MaterialTheme.typography.bodyLarge,
        color = if (subdued) TraceColors.Muted else TraceColors.Ink)
}

@Composable fun Headline(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.semantics { heading() }, style = MaterialTheme.typography.headlineLarge,
        color = TraceColors.Ink)
}

@Composable fun SectionTitle(text: String, action: String? = null, onAction: (() -> Unit)? = null) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    if (largeText && action != null && onAction != null) {
        Column(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp)) {
            Text(text, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 44.dp)) {
                Text(action, style = MaterialTheme.typography.labelMedium, color = TraceColors.Muted)
            }
        }
    } else {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleMedium)
            if (action != null && onAction != null) {
                TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 6.dp), modifier = Modifier.heightIn(min = 44.dp)) {
                    Text(action, style = MaterialTheme.typography.labelMedium, color = TraceColors.Muted)
                }
            }
        }
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

@Composable fun TraceSignature(label: String = "TRACE") {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(BankIcons.Trace, null, Modifier.size(20.dp), tint = TraceColors.Coral)
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = TraceColors.InkSoft)
    }
}

@Composable fun SaeonWordmark(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.width(3.dp).height(22.dp).background(TraceColors.Coral))
        Text("새온은행", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable fun Page(
    title: String = "",
    tag: String = "",
    back: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag(tag), contentAlignment = Alignment.TopCenter) {
        val compactHeight = maxHeight < 480.dp
        val pageScroll = rememberScrollState()
        val bodyScroll = rememberScrollState()
        Column(Modifier.widthIn(max = 620.dp).fillMaxSize()
            .then(if (compactHeight) Modifier.verticalScroll(pageScroll) else Modifier)) {
            if (title.isNotEmpty() || back != null || actions != null) {
                Row(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = if (back != null) 8.dp else 20.dp)
                        .heightIn(min = 58.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (back != null) IconAction(BankIcons.Back, "이전 화면", onClick = back)
                    if (back == null && title == "새온은행") {
                        SaeonWordmark(Modifier.weight(1f))
                    } else {
                        Text(title, Modifier.weight(1f).padding(vertical = 10.dp).semantics { heading() },
                            style = if (back == null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
                            fontWeight = if (back == null) FontWeight.SemiBold else FontWeight.Medium)
                    }
                    actions?.invoke(this)
                }
            }
            val bodyModifier = if (compactHeight) Modifier else Modifier.weight(1f).verticalScroll(bodyScroll)
            Column(
                bodyModifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .padding(top = if (title.isEmpty()) 20.dp else 10.dp, bottom = 30.dp),
                content = content
            )
            if (footer != null) {
                Column(
                    Modifier.fillMaxWidth().background(TraceColors.Paper)
                        .border(BorderStroke(0.dp, Color.Transparent))
                        .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp), content = footer
                )
            }
        }
    }
}

@Composable fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    val container = if (accent) TraceColors.Deep else TraceColors.Ink
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = TraceColors.White,
            disabledContainerColor = TraceColors.SoftStrong,
            disabledContentColor = TraceColors.Muted
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable fun QuietButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = TraceColors.Ink),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}

@Composable fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    QuietButton(text, modifier.fillMaxWidth(), onClick = onClick)
}

@Composable fun Field(
    value: String,
    label: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    maxLines: Int = 1,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value,
        onChange,
        modifier.fillMaxWidth(),
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        singleLine = maxLines == 1,
        minLines = minLines,
        maxLines = maxLines,
        enabled = enabled,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TraceColors.Ink,
            unfocusedBorderColor = TraceColors.DividerStrong,
            focusedContainerColor = TraceColors.SurfaceRaised,
            unfocusedContainerColor = TraceColors.SurfaceRaised,
            cursorColor = TraceColors.CoralText,
            focusedLabelColor = TraceColors.InkSoft,
            unfocusedLabelColor = TraceColors.Muted
        )
    )
}

@Composable fun Money(value: Long, modifier: Modifier = Modifier, prefix: String = "", hero: Boolean = true) {
    val scale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth().semantics { contentDescription = "$prefix${won(value)}원" }) {
        val digits = prefix + won(value)
        val base = if (hero) ((maxWidth.value - 22f * scale) / (digits.length.coerceAtLeast(1) * 0.58f * scale)).coerceIn(21f, 36f) else 23f
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = base.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")) { append(digits) }
                withStyle(SpanStyle(fontSize = (if (hero) 17 else 15).sp, fontWeight = FontWeight.Medium)) { append("원") }
            },
            Modifier.clearAndSetSemantics {},
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            style = MaterialTheme.typography.displayMedium,
            color = TraceColors.Ink
        )
    }
}

@Composable fun DetailRow(label: String, value: String, emphasize: Boolean = false) {
    val valueWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Medium
    if (LocalDensity.current.fontScale >= 1.3f) {
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TraceColors.Muted)
            Text(value, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, fontWeight = valueWeight)
        }
    } else {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
            Text(label, Modifier.weight(0.85f), style = MaterialTheme.typography.bodySmall, color = TraceColors.Muted)
            Text(value, Modifier.weight(1.55f), style = MaterialTheme.typography.bodyMedium,
                fontWeight = valueWeight, textAlign = TextAlign.End)
        }
    }
}

@Composable fun MenuRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tag: String = "",
    trailing: String? = null,
    onClick: (() -> Unit)? = null
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    val click = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(
        Modifier.fillMaxWidth().then(click).testTag(tag).heightIn(min = 66.dp).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        if (icon != null) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(TraceColors.Soft), contentAlignment = Alignment.Center) {
                AppIcon(icon, size = 19, tint = TraceColors.InkSoft)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (!subtitle.isNullOrEmpty()) { Space(3); Caption(subtitle) }
            if (largeText && trailing != null) { Space(5); Text(trailing, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
        }
        if (!largeText && trailing != null) {
            Text(trailing, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
        }
        if (onClick != null) AppIcon(BankIcons.Chevron, size = 17, tint = TraceColors.Muted)
    }
}

@Composable fun SurfaceBox(modifier: Modifier = Modifier, tint: Color = TraceColors.SurfaceRaised, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().background(tint, RoundedCornerShape(12.dp)).padding(18.dp), content = content)
}

@Composable fun EditorialPanel(modifier: Modifier = Modifier, accent: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Row(modifier.fillMaxWidth()) {
        Box(Modifier.width(2.dp).fillMaxHeight().background(if (accent) TraceColors.Coral else TraceColors.DividerStrong))
        Column(Modifier.weight(1f).padding(start = 16.dp, top = 2.dp, bottom = 2.dp), content = content)
    }
}

@Composable fun RecipientGlyph(name: String, modifier: Modifier = Modifier) {
    val initial = name.firstOrNull()?.toString().orEmpty()
    Box(modifier.size(40.dp).clip(CircleShape).background(TraceColors.Soft), contentAlignment = Alignment.Center) {
        Text(initial, style = MaterialTheme.typography.labelLarge, color = TraceColors.InkSoft)
    }
}

@Composable fun NumberedReason(number: Int, title: String, description: String? = null, accent: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(number.toString().padStart(2, '0'), Modifier.widthIn(min = 25.dp), style = MaterialTheme.typography.labelMedium,
            color = if (accent) TraceColors.CoralText else TraceColors.Muted)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (description != null) { Space(4); Caption(description) }
        }
    }
}

@Composable fun EmptyState(title: String, description: String) {
    Space(24); Text(title, style = MaterialTheme.typography.titleMedium); Space(10); Body(description, subdued = true); Space(22)
}

@Composable fun ErrorNote(message: String) {
    Text(message, Modifier.fillMaxWidth().padding(vertical = 10.dp).semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyMedium, color = TraceColors.CoralText)
}

@Composable fun SimulationNote() { Caption("시연용 가상 거래 · 실제 자금 이동 없음") }

@Composable fun OptionRow(label: String, checked: Boolean, description: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 70.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .semantics { stateDescription = if (checked) "켜짐" else "꺼짐" }.padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) { Body(label); if (description != null) { Space(3); Caption(description) } }
        Switch(checked, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics {},
            colors = SwitchDefaults.colors(checkedThumbColor = TraceColors.White, checkedTrackColor = TraceColors.Ink))
    }
}
