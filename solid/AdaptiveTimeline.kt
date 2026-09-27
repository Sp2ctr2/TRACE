package app.saeon.trace.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp\nimport androidx.compose.ui.unit.dp
import app.saeon.trace.core.*
import app.saeon.trace.ui.design.*

private data class ContextCard(val time:Long,val title:String,val detail:String,val final:Boolean=false)

@Composable fun AdaptiveTimeline(state:BankState,back:()->Unit){
    val record=state.current?:state.pending.lastOrNull()
    val events=if(record==null)state.events else RiskContext(state.events).relevant(record.intent,record.intent.createdAt)
    val items=events.sortedBy{it.createdAt}.map{ContextCard(it.createdAt,it.type.label,it.summary)}.toMutableList()
    if(record!=null)items+=ContextCard(record.intent.createdAt,"${won(record.intent.amount)}원 송금 시도","${record.intent.recipient.name} · ${record.intent.recipient.bank}",true)
    Page("연결된 맥락","trace_timeline",back=back){
        val spec=LocalAdaptiveSpec.current
        Space(if(spec.compactHeight)4 else 10)
        Text("요청에서 송금까지",Modifier.semantics{heading()},style=MaterialTheme.typography.headlineSmall)
        if(record!=null){Space(6);Caption("${record.intent.recipient.name} · ${won(record.intent.amount)}원")}
        Space(if(spec.compactHeight)10 else 20)
        if(items.isEmpty()){
            EmptyState("연결된 내역이 없습니다.","확인한 요청과 거래가 시간순으로 표시됩니다.")
        }else{
            val width=if(spec.twoPane)620.dp else Dp.Unspecified
            Column(Modifier.then(if(width!=Dp.Unspecified)Modifier.widthIn(max=width)else Modifier).testTag("timeline_cards")){
                items.forEachIndexed{i,item->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.Top){
                        Column(Modifier.width(46.dp),horizontalAlignment=Alignment.End){
                            Text(timeLabel(item.time),style=MaterialTheme.typography.labelSmall,color=TraceColors.Muted)
                        }
                        Column(Modifier.width(10.dp).heightIn(min=78.dp),horizontalAlignment=Alignment.CenterHorizontally){
                            Box(Modifier.size(if(item.final)8.dp else 6.dp).background(if(item.final)TraceColors.Coral else TraceColors.Divider,RoundedCornerShape(5.dp)))
                            if(i<items.lastIndex)Box(Modifier.width(1.dp).weight(1f).background(TraceColors.Divider))
                        }
                        Surface(
                            Modifier.weight(1f).padding(bottom=12.dp),
                            shape=RoundedCornerShape(17.dp),
                            color=if(item.final)TraceColors.CoralLight.copy(alpha=.55f)else TraceColors.Surface,
                            border=BorderStroke(.8.dp,if(item.final)TraceColors.Coral.copy(alpha=.18f)else TraceColors.Divider.copy(alpha=.72f)),
                            shadowElevation=0.dp
                        ){
                            Column(Modifier.padding(horizontal=16.dp,vertical=14.dp)){
                                Text(item.title,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold,color=if(item.final)TraceColors.CoralText else TraceColors.Ink)
                                Space(6)
                                Text(item.detail,style=MaterialTheme.typography.bodyMedium,color=TraceColors.Muted)
                            }
                        }
                    }
                }
            }
        }
    }
}
