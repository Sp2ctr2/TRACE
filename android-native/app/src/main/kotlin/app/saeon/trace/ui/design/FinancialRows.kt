package app.saeon.trace.ui.design

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Presentation-only rows for the fictional offline reference application. */
@Composable fun DetailRow(label: String, value: String, emphasize: Boolean = false) {
    val large = LocalDensity.current.fontScale >= 1.5f
    if (large) {
        Column(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TraceColors.Muted)
            Text(value, style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Medium)
        }
    } else {
        Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Top) {
            Text(label, Modifier.weight(0.9f), style = MaterialTheme.typography.bodyMedium, color = TraceColors.Muted)
            Text(value, Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Medium, textAlign = TextAlign.End)
        }
    }
}
@Composable fun MenuRow(title: String, subtitle: String? = null, icon: ImageVector? = null,
    tag: String = "", trailing: String? = null, onClick: (() -> Unit)? = null
) {
    val large = LocalDensity.current.fontScale >= 1.5f
    val click = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(Modifier.fillMaxWidth().then(click).testTag(tag).heightIn(min = 68.dp).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        if (icon != null) AppIcon(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (!subtitle.isNullOrEmpty()) { Space(4); Caption(subtitle) }
            if (large && trailing != null) { Space(6); Text(trailing, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
        }
        if (!large && trailing != null) Text(trailing, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        if (onClick != null) AppIcon(BankIcons.Chevron, size = 18, tint = TraceColors.Muted)
    }
}
@Composable fun SurfaceBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().background(TraceColors.Surface, RoundedCornerShape(12.dp)).padding(18.dp), content = content)
}
@Composable fun NumberedReason(number: Int, title: String, description: String? = null, accent: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
        Text(number.toString().padStart(2, '0'), Modifier.widthIn(min = 25.dp), style = MaterialTheme.typography.labelMedium,
            color = if (accent) TraceColors.AccentInk else TraceColors.Muted)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (description != null) { Space(5); Caption(description) }
        }
    }
}
