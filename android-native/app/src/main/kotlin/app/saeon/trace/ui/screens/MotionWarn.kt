package app.saeon.trace.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import app.saeon.trace.core.*
import app.saeon.trace.ui.*
import app.saeon.trace.ui.design.*

/** A warning is not a hold, and acknowledgement is not payment authorization. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MotionWarn(state:BankState,record:TransferRecord,interaction:InteractionState,
    model:BankViewModel,open:(String)->Unit,back:()->Unit,home:()->Unit) {
    var checked by rememberSaveable(record.intent.id){mutableStateOf(false)}
    // Its confirm action is disabled because this record is still WARN, not REVIEW.
    ViewportReview(state,record,interaction,model,open,back)
    ModalBottomSheet(onDismissRequest={model.cancelTransfer(record.intent.id,home)},
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=TraceColors.Surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=20.dp)
            .padding(bottom=16.dp).testTag("trace_warn")) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                AppIcon(BankIcons.Trace,tint=TraceColors.Coral,size=24)
                Text("보내기 전 확인",style=MaterialTheme.typography.labelLarge,color=TraceColors.Muted)
            }
            Space(14);Text("하나만 더 확인해 주세요",style=MaterialTheme.typography.headlineSmall,
                modifier=Modifier.semantics{heading()})
            Space(10);Body("확인하지 않은 링크와 이 송금이 가까운 시간에 이어졌어요.",subdued=true)
            Space(16);TaskTransaction(record.intent.recipient.name,record.intent.amount)
            Row(Modifier.fillMaxWidth().heightIn(min=64.dp).toggleable(checked,role=Role.Checkbox,onValueChange={checked=it}).testTag("warn_check"),
                verticalAlignment=Alignment.CenterVertically) {
                Checkbox(checked,null)
                Text("다른 경로로 받는 분과 금액을 확인했어요.",Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
            }
            Caption("아직 돈은 나가지 않았어요. 확인 뒤 다시 인증합니다.")
            Space(14)
            PrimaryButton("확인한 내용으로 다시 보기",Modifier.testTag("warn_acknowledge"),enabled=checked&&!interaction.busy){model.acknowledge(record.intent.id)}
            SecondaryButton("송금 취소",Modifier.fillMaxWidth()){model.cancelTransfer(record.intent.id,home)}
        }
    }
}
