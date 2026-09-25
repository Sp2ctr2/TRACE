package app.saeon.trace.ui.design

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

val LocalTaskHeight = staticCompositionLocalOf { 600.dp }
val LocalTaskDense = staticCompositionLocalOf { false }

/** Bounded task surface, separate from list pages. Overflow is a safety fallback,
 * never clipped or made unreadably small. The dock and CTA have reserved space. */
@Composable fun TaskPage(title: String = "", tag: String, root: Boolean = false,
    back: (() -> Unit)? = null, actions: (@Composable RowScope.() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = LocalDensity.current.fontScale
    val scroll = rememberScrollState()
    val outer = rememberScrollState()
    BoxWithConstraints(Modifier.fillMaxSize().testTag(tag),contentAlignment=Alignment.TopCenter) {
        val landscape = maxHeight < 350.dp
        Column(Modifier.widthIn(max=560.dp).fillMaxSize()
            .padding(bottom=if(root)84.dp else 0.dp)
            .then(if(landscape)Modifier.verticalScroll(outer)else Modifier)) {
            if(title.isNotEmpty()||back!=null||actions!=null) {
                Row(Modifier.fillMaxWidth().heightIn(min=52.dp).padding(horizontal=if(back!=null)6.dp else 20.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    if(back!=null)IconAction(BankIcons.Back,"이전 화면",onClick=back)
                    Text(title,Modifier.weight(1f).semantics{heading()},style=if(root)MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall)
                    actions?.invoke(this)
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth().then(if(landscape)Modifier else Modifier.weight(1f))) {
                val dense = maxHeight < 510.dp || scale > 1.15f
                CompositionLocalProvider(LocalTaskHeight provides maxHeight,LocalTaskDense provides dense) {
                    Column(Modifier.fillMaxWidth().then(if(landscape)Modifier else Modifier.verticalScroll(scroll))
                        .testTag("${tag}_body").padding(horizontal=20.dp).padding(top=8.dp,bottom=8.dp),content=content)
                }
            }
            if(footer!=null) Column(Modifier.fillMaxWidth().background(TraceColors.Paper).padding(horizontal=20.dp)
                .padding(top=8.dp,bottom=8.dp),verticalArrangement=Arrangement.spacedBy(4.dp),content=footer)
        }
    }
}
@Composable fun TaskGap(size:Int=16) { Spacer(Modifier.height((if(LocalTaskDense.current)size*.6f else size.toFloat()).dp)) }
@Composable fun TaskHeadline(text:String,compact:String=text) {
    Text(if(LocalTaskDense.current)compact else text,Modifier.semantics{heading()},
        style=MaterialTheme.typography.headlineMedium.copy(fontSize=if(LocalTaskDense.current)26.sp else 28.sp,lineHeight=36.sp))
}
@Composable fun TaskDetail(label:String,value:String,modifier:Modifier=Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        Text(label,Modifier.weight(.9f),style=MaterialTheme.typography.bodyMedium,color=TraceColors.Muted)
        Text(value,Modifier.weight(1.5f),style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium,
            textAlign=androidx.compose.ui.text.style.TextAlign.End)
    }
}
@Composable fun TaskTransaction(name:String,amount:Long,caption:String="보내려던 송금") {
    Row(Modifier.fillMaxWidth().background(TraceColors.Surface,androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
        .padding(horizontal=16.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(name,style=MaterialTheme.typography.titleSmall);Caption(caption) }
        Text("${won(amount)}원",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
    }
}
