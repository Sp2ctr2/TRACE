package app.saeon.trace.ui.design

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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
    keyboard: KeyboardType = KeyboardType.Text, minLines: Int = 1, maxLines: Int = 1, enabled: Boolean = true
) {
    OutlinedTextField(value, onChange, modifier.fillMaxWidth(), label = { Text(label) }, singleLine = maxLines == 1,
        minLines = minLines, maxLines = maxLines, enabled = enabled, textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TraceColors.Deep, unfocusedBorderColor = TraceColors.OriginalMuted,
            focusedContainerColor = TraceColors.Surface, unfocusedContainerColor = TraceColors.Surface, cursorColor = TraceColors.Deep))
}
@Composable fun OptionRow(label: String, checked: Boolean, description: String? = null, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
        .semantics { stateDescription = if (checked) "켜짐" else "꺼짐" }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) { Body(label); if (description != null) { Space(4); Caption(description) } }
        Switch(checked, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics {},
            colors = SwitchDefaults.colors(checkedThumbColor = TraceColors.White, checkedTrackColor = TraceColors.Ink))
    }
}
