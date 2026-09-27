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

val LocalTaskHeight=staticCompositionLocalOf{600.dp}
val LocalTaskDense=staticCompositionLocalOf{false}
val LocalBankLandscape=staticCompositionLocalOf{false}
val LocalBankWide=staticCompositionLocalOf{false}

/** Actual current window bounds. Never pass infinite height from a scroll container.
 * Portrait: one readable column. Landscape task: reading pane and actions side by side.
 */
@Composable fun AdaptiveFrame(title:String="",tag:String="",root:Boolean=false,task:Boolean=false,
    back:(()->Unit)?=null,actions:(@Composable RowScope.()->Unit)?=null,
    footer:(@Composable ColumnScope.()->Unit)?=null,content:@Composable ColumnScope.()->Unit){
    val font=LocalDensity.current.fontScale
    val bodyScroll=rememberScrollState()
    val footerScroll=rememberScrollState()
    BoxWithConstraints(Modifier.fillMaxSize().testTag(tag),contentAlignment=Alignment.TopCenter){
        val landscape=maxWidth>maxHeight*1.22f && maxWidth>=600.dp
        val wide=maxWidth>=600.dp
        val maxContent=if(landscape)1080.dp else if(wide)720.dp else 560.dp
        val split=landscape&&footer!=null
        val side=if(wide)28.dp else 20.dp
        CompositionLocalProvider(LocalBankLandscape provides landscape,LocalBankWide provides wide){
            Column(Modifier.widthIn(max=maxContent).fillMaxSize().padding(bottom=if(root)72.dp else 0.dp)){
                if(title.isNotEmpty()||back!=null||actions!=null){
                    Row(Modifier.fillMaxWidth().heightIn(min=if(landscape)48.dp else 52.dp)
                        .padding(horizontal=if(back!=null)6.dp else side),verticalAlignment=Alignment.CenterVertically){
                        if(back!=null)IconAction(BankIcons.Back,"이전 화면",onClick=back)
                        if(root&&title=="새온은행"){AppIcon(BankIcons.Trace,size=24,tint=TraceColors.Coral);Spacer(Modifier.width(8.dp))}
                        Text(title,Modifier.weight(1f).semantics{heading()},style=if(root)MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall)
                        actions?.invoke(this)
                    }
                }
                if(split){
                    Row(Modifier.fillMaxWidth().weight(1f).padding(horizontal=side),horizontalArrangement=Arrangement.spacedBy(28.dp)){
                        BoxWithConstraints(Modifier.weight(1.1f).fillMaxHeight()){
                            CompositionLocalProvider(LocalTaskHeight provides maxHeight,LocalTaskDense provides (maxHeight<510.dp||font>1.15f)){
                                Column(Modifier.fillMaxSize().verticalScroll(bodyScroll).testTag("${tag}_body").padding(top=8.dp,bottom=16.dp),content=content)
                            }
                        }
                        Column(Modifier.weight(.9f).fillMaxHeight().verticalScroll(footerScroll).testTag("${tag}_footer").padding(vertical=8.dp),
                            verticalArrangement=Arrangement.spacedBy(4.dp)){footer!!.invoke(this)}
                    }
                }else{
                    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)){
                        CompositionLocalProvider(LocalTaskHeight provides maxHeight,LocalTaskDense provides (maxHeight<510.dp||font>1.15f)){
                            Column(Modifier.fillMaxSize().verticalScroll(bodyScroll).testTag("${tag}_body")
                                .padding(horizontal=side).padding(top=if(task)8.dp else 12.dp,bottom=if(task)8.dp else 24.dp),content=content)
                        }
                    }
                    if(footer!=null)Column(Modifier.fillMaxWidth().testTag("${tag}_footer").padding(horizontal=side).padding(top=8.dp,bottom=8.dp),
                        verticalArrangement=Arrangement.spacedBy(4.dp),content=footer)
                }
            }
        }
    }
}

@Composable fun TaskPage(title:String="",tag:String,root:Boolean=false,back:(()->Unit)?=null,
    actions:(@Composable RowScope.()->Unit)?=null,footer:(@Composable ColumnScope.()->Unit)?=null,content:@Composable ColumnScope.()->Unit){
    AdaptiveFrame(title,tag,root,true,back,actions,footer,content)
}
@Composable fun TaskGap(size:Int=16){Spacer(Modifier.height((if(LocalTaskDense.current)size*.6f else size.toFloat()).dp))}
@Composable fun TaskHeadline(text:String,compact:String=text){
    Text(if(LocalTaskDense.current)compact else text,Modifier.semantics{heading()},style=MaterialTheme.typography.headlineMedium.copy(fontSize=if(LocalTaskDense.current)26.sp else 28.sp,lineHeight=36.sp))
}
@Composable fun TaskDetail(label:String,value:String,modifier:Modifier=Modifier){
    Row(modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)){
        Text(label,Modifier.weight(.9f),style=MaterialTheme.typography.bodyMedium,color=TraceColors.Muted)
        Text(value,Modifier.weight(1.5f),style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium,textAlign=androidx.compose.ui.text.style.TextAlign.End)
    }
}
@Composable fun TaskTransaction(name:String,amount:Long,caption:String="보내려던 송금"){
    Row(Modifier.fillMaxWidth().background(TraceColors.Surface,androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(16.dp),verticalAlignment=Alignment.CenterVertically){
        Column(Modifier.weight(1f)){Text(name,style=MaterialTheme.typography.titleSmall);Caption(caption)}
        Text("${won(amount)}원",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
    }
}
