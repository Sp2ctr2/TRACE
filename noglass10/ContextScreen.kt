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
import androidx.compose.ui.unit.dp
import app.saeon.trace.core.*
import app.saeon.trace.ui.design.*

@Composable fun ContextScreen(state:BankState,back:()->Unit){
    val record=state.current?:state.pending.lastOrNull()
    val events=if(record==null)state.events else RiskContext(state.events).relevant(record.intent,record.intent.createdAt)
    Page("연결된 맥락","trace_timeline",back=back){
        Space(10);Headline("요청과 송금의 흐름");Space(18)
        if(record!=null){
            Row(Modifier.fillMaxWidth().background(TraceColors.CoralLight,RoundedCornerShape(18.dp)).padding(18.dp),verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){Text(record.intent.recipient.name,style=MaterialTheme.typography.titleSmall);Caption(record.intent.recipient.bank)}
                Text("${won(record.intent.amount)}원",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
            };Space(22)
        }
        if(events.isEmpty()&&record==null)EmptyState("연결된 내역이 없습니다.","요청과 거래가 확인되면 시간순으로 표시됩니다.")
        events.sortedBy{it.createdAt}.forEachIndexed{i,event->
            ContextPanel(timeLabel(event.createdAt),event.type.label,event.summary,false,"context_event_$i")
            ContextConnector()
        }
        if(record!=null)ContextPanel(timeLabel(record.intent.createdAt),"송금 시도","${record.intent.recipient.name} · ${won(record.intent.amount)}원",true,"context_transfer")
        Space(20)
        Caption(if(record?.stage==TransferStage.COMPLETE)"완료된 거래의 확인 내역입니다."else"확인 중인 송금의 잔액은 그대로입니다.")
    }
}
@Composable private fun ContextPanel(time:String,title:String,body:String,final:Boolean,tag:String){
    Column(Modifier.fillMaxWidth().testTag(tag).background(TraceColors.Surface,RoundedCornerShape(18.dp)).padding(18.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Text(title,Modifier.weight(1f).semantics{heading()},style=MaterialTheme.typography.titleSmall,color=if(final)TraceColors.CoralText else TraceColors.Ink)
            Text(time,style=MaterialTheme.typography.labelMedium,color=TraceColors.Muted)
        }
        Space(8);Text(body,style=MaterialTheme.typography.bodyMedium,color=TraceColors.Muted)
    }
}
@Composable private fun ContextConnector(){
    Row(Modifier.fillMaxWidth().height(22.dp).padding(start=28.dp),verticalAlignment=Alignment.CenterVertically){
        Box(Modifier.width(2.dp).fillMaxHeight().background(TraceColors.Divider))
    }
}
