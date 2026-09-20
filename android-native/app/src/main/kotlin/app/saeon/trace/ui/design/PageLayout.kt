package app.saeon.trace.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** With little vertical space, actions become the end of the same scroll flow.
 * This prevents a large-font footer from consuming the entire landscape body. */
@Composable fun Page(title: String = "", tag: String = "", back: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null, footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag(tag), contentAlignment = Alignment.TopCenter) {
        val compactHeight = maxHeight < 440.dp
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize()) {
            if (title.isNotEmpty() || back != null || actions != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = if (back != null) 8.dp else 20.dp).heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (back != null) IconAction(BankIcons.Back, "이전 화면", onClick = back)
                    Text(title, Modifier.weight(1f).padding(vertical = 12.dp).semantics { heading() },
                        style = if (back == null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall)
                    actions?.invoke(this)
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
                .padding(top = if (title.isEmpty()) 20.dp else 12.dp, bottom = 28.dp)) {
                content()
                if (compactHeight && footer != null) {
                    Space(24)
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp), content = footer)
                }
            }
            if (!compactHeight && footer != null) Column(Modifier.fillMaxWidth().background(TraceColors.Paper).padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = footer)
        }
    }
}
