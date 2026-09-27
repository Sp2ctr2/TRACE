package app.saeon.trace.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.saeon.trace.core.*
import app.saeon.trace.ui.BankViewModel
import app.saeon.trace.ui.design.*

@Composable fun GlassHold(state:BankState,record:TransferRecord,easy:Boolean,model:BankViewModel,open:(String)->Unit,back:()->Unit,home:()->Unit){
    var details by rememberSaveable(record.intent.id){mutableStateOf(false)}
    val order=listOf(RiskType.REMOTE_ACCESS,RiskType.FAMILY_CLAIM,RiskType.PROFIT_PROMISE,RiskType.SECRECY,RiskType.IMPERSONATION,RiskType.URGENCY,RiskType.NEW_RECIPIENT,RiskType.SUSPICIOUS_LINK,RiskType.FINANCIAL_INSTRUCTION)
    val reasons=order.filter{it in record.reasons}
    TaskPage("송금 보류","trace_hold",back=back,actions={AppIcon(BankIcons.Trace,size=25,tint=TraceColors.Coral)},footer={
        PrimaryButton(if(easy)"안전하게 확인하기"else"공식 경로로 확인하기",Modifier.testTag("hold_safe_action")){open("safety_guide")}
        SecondaryButton("송금 취소",Modifier.testTag("hold_cancel")){model.cancelTransfer(record.intent.id,home)}
    }){
        val compact=LocalTaskHeight.current<500.dp
        if(easy)Box(Modifier.testTag("easy_mode"))
        if(!compact){Space(12);AppIcon(BankIcons.Pause,size=36,tint=TraceColors.CoralText);Space(18)}
        Text(if(easy)"돈을 보내지 않았어요"else"이 송금은\n잠시 멈췄어요",Modifier.semantics{heading()},style=MaterialTheme.typography.headlineMedium.copy(fontSize=if(compact)26.sp else 29.sp,lineHeight=36.sp))
        Space(if(compact)8 else 12)
        Body(if(easy)"상대방의 요청을 먼저 확인해 주세요."else"요청과 송금에서 위험 신호를 확인했어요.",subdued=true)
        Space(if(compact)14 else 24)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){Text(record.intent.recipient.name,style=MaterialTheme.typography.titleMedium);Caption(if(record.intent.recipient.known)"저장된 계좌"else"처음 보내는 계좌")}
            Text("${won(record.intent.amount)}원",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
        }
        Space(if(compact)12 else 20);Rule();Space(if(compact)8 else 16)
        if(easy){
            reasons.take(1).forEach{Text(it.label,style=MaterialTheme.typography.titleMedium);Space(8);Body(it.explanation,subdued=true)}
        }else{
            reasons.take(if(compact)2 else 3).forEach{r->
                Row(Modifier.fillMaxWidth().heightIn(min=if(compact)32.dp else 42.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    AppIcon(BankIcons.Pause,size=15,tint=TraceColors.Muted)
                    Text(r.label,style=MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Space(if(compact)2 else 12)
        QuietButton("이유 자세히 보기",Modifier.fillMaxWidth().testTag("hold_reasons_open")){details=true}
        if(!compact)Caption("잔액은 그대로입니다.")
    }
    if(details)AlertDialog(onDismissRequest={details=false},title={Text("확인한 위험 신호")},text={
        Column(Modifier.verticalScroll(rememberScrollState()).testTag("trace_hold_reason")){
            reasons.forEach{r->Text(r.label,style=MaterialTheme.typography.titleSmall);Space(5);Body(r.explanation,subdued=true);Space(18)}
            QuietButton("이어진 흐름 보기",Modifier.testTag("hold_timeline_open")){details=false;open("timeline")}
        }
    },confirmButton={QuietButton("확인",Modifier.testTag("hold_reasons_close")){details=false}},containerColor=TraceColors.Surface)
}
