package app.saeon.trace.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saeon.trace.core.*
import app.saeon.trace.data.BankPreferences
import app.saeon.trace.ui.*
import app.saeon.trace.ui.design.*

@Composable fun ViewportHome(state:BankState,preferences:BankPreferences,model:BankViewModel,open:(String)->Unit) {
    TaskPage("새온은행","home",root=true,actions={
        IconAction(BankIcons.Bell,"알림"){open("notifications")};IconAction(BankIcons.Profile,"내 정보"){open("profile")}
    }) {
        val small=LocalTaskHeight.current<450.dp
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(role=Role.Button){open("account")}.heightIn(min=48.dp),verticalArrangement=Arrangement.Center) {
                Text("새온 생활통장",style=MaterialTheme.typography.bodyLarge)
                if(!small)Caption("110-***-0001")
            }
            IconAction(BankIcons.Eye,if(preferences.hideBalance)"잔액 보이기"else"잔액 숨기기"){model.preference{hideBalance(!preferences.hideBalance)}}
        }
        Box(Modifier.testTag("home_balance")){AmountDisplay(state.balance,hidden=preferences.hideBalance)}
        TaskGap(18)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1.45f)){PrimaryButton("송금",Modifier.testTag("home_transfer")){open("transfer")}}
            Box(Modifier.weight(1f)){SecondaryButton("가져오기",Modifier.testTag("home_bring")){open("bring")}}
        }
        TaskGap(24);Rule();TaskGap(12)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            listOf(Triple("이번 달 쓴 돈","382,400원","card"),Triple("모아둔 돈","${won(state.savings)}원","savings")).forEach{(label,value,route)->
                Column(Modifier.weight(1f).heightIn(min=48.dp).clickable(role=Role.Button){open(route)},verticalArrangement=Arrangement.Center) {
                    Caption(label);Text(value,style=MaterialTheme.typography.titleSmall)
                }
            }
        }
        TaskGap(18)
        Row(Modifier.fillMaxWidth().heightIn(min=56.dp).clip(RoundedCornerShape(14.dp)).background(TraceColors.Surface)
            .clickable(role=Role.Button){open("safety")}.padding(horizontal=12.dp,vertical=8.dp),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Icon(BankIcons.Trace,null,Modifier.size(27.dp),tint=TraceColors.Coral)
            Column(Modifier.weight(1f)) {
                Text(if(state.pending.isEmpty())"보내기 전, 한 번 더."else"보류한 송금 ${state.pending.size}건",style=MaterialTheme.typography.titleSmall)
                if(!small)Caption(if(state.pending.isEmpty())"송금 앞의 맥락을 확인해요"else"아직 돈은 나가지 않았어요")
            }
            AppIcon(BankIcons.Chevron,size=16,tint=TraceColors.Muted)
        }
        TaskGap(18)
        SectionTitle("최근 거래","전체"){open("history")}
        state.receipts.take(if(small)1 else 2).forEach{receipt ->
            Row(Modifier.fillMaxWidth().heightIn(min=56.dp).clickable(role=Role.Button){open("receipt/${receipt.id}")},
                verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                AppIcon(if(receipt.direction==Direction.CREDIT)BankIcons.Download else BankIcons.Card,size=21,tint=TraceColors.Muted)
                Column(Modifier.weight(1f)) {
                    Text(receipt.recipient.name,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium)
                    if(!small)Caption(receipt.memo.ifEmpty{receipt.purpose.label})
                }
                Text("${if(receipt.direction==Direction.DEBIT)"−"else"+"}${won(receipt.amount)}원",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold)
            }
        }
    }
}

@Composable fun ViewportRecipient(state:BankState,model:BankViewModel,open:(String)->Unit) {
    var query by rememberSaveable{mutableStateOf("")};var favorites by rememberSaveable{mutableStateOf(false)}
    val prefs by model.preferences.collectAsStateWithLifecycle()
    val base=(listOf(Fixtures.recipient(state.scenario))+state.recipients).distinctBy{it.id}
    val result=base.filter{(!favorites||it.id in prefs.favoriteIds)&&(query.isBlank()||it.name.contains(query)||it.bank.contains(query)||it.account.contains(query))}
    TaskPage("송금","transfer_recipient",root=true) {
        TaskHeadline("누구에게 보낼까요?");TaskGap(20)
        Field(query,"이름 또는 계좌번호",{query=it},Modifier.testTag("recipient_search"))
        MenuRow("계좌번호로 보내기",icon=BankIcons.Bank,tag="recipient_account_entry"){open("recipient_entry")}
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            QuietButton("최근",onClick={favorites=false});QuietButton("자주",onClick={favorites=true})
            Spacer(Modifier.weight(1f));QuietButton("전체 계좌"){open("recipients_all")}
        }
        if(result.isEmpty())EmptyState("찾는 계좌가 없어요.","이름이나 계좌 끝자리를 다시 입력해 주세요.")
        result.take(if(LocalTaskHeight.current<450.dp)2 else 3).forEach{person->
            Row(Modifier.fillMaxWidth().heightIn(min=64.dp).clickable(role=Role.Button){model.startRecipient(person){open("amount")}}.testTag("recipient_${person.id}"),
                verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                PersonBadge(person.name,40)
                Column(Modifier.weight(1f)){Text(person.name,style=MaterialTheme.typography.titleSmall);Caption("${person.bank} ${person.account}")}
                AppIcon(BankIcons.Chevron,size=16,tint=TraceColors.Muted)
            }
        }
    }
}

@Composable fun ViewportAmount(state:BankState,model:BankViewModel,open:(String)->Unit,back:()->Unit) {
    val draft=state.draft?:return
    var digits by rememberSaveable(draft.recipient.id){mutableStateOf(draft.amount.toString())}
    var purpose by rememberSaveable(draft.recipient.id){mutableStateOf(draft.purpose.name)}
    var purposeOpen by rememberSaveable{mutableStateOf(false)}
    var direct by rememberSaveable{mutableStateOf(false)}
    var entered by rememberSaveable{mutableStateOf("")}
    val interaction by model.interaction.collectAsStateWithLifecycle()
    val amount=digits.toLongOrNull()?:0L
    val selected=Purpose.valueOf(purpose)
    val error=runCatching{BankEngine.validateAmount(state,amount,selected,model.repository.clock.now())}.exceptionOrNull()?.message
    fun update(v:String) {
        val value=v.filter(Char::isDigit).trimStart('0').ifEmpty{"0"}
        if(value.length>9){model.showError("입력 가능한 금액을 넘었어요.");return}
        digits=value;model.storeDraft(TransferDraft(draft.recipient,value.toLongOrNull()?:0L,selected))
    }
    TaskPage("송금 금액","transfer_amount",back=back,footer={
        listOf(listOf("1","2","3"),listOf("4","5","6"),listOf("7","8","9"),listOf("clear","0","backspace")).forEach{keys->
            Row(Modifier.fillMaxWidth()) {keys.forEach{key->
                TextButton(onClick={when(key){"clear"->update("0");"backspace"->update(digits.dropLast(1));else->update(if(digits=="0")key else digits+key)}},
                    enabled=!interaction.busy,modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("key_$key"),
                    contentPadding=PaddingValues(4.dp),colors=ButtonDefaults.textButtonColors(contentColor=TraceColors.Ink)) {
                    when(key){"clear"->Text("지우기",style=MaterialTheme.typography.labelMedium);"backspace"->AppIcon(BankIcons.Delete,description="한 자리 지우기");else->Text(key,fontSize=26.sp,fontWeight=FontWeight.Medium)}
                }
            }}
        }
        PrimaryButton("다음",Modifier.testTag("amount_next"),enabled=amount>0&&error==null&&!interaction.busy){model.review(TransferDraft(draft.recipient,amount,selected)){open("transfer_state")}}
    }) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Text("${draft.recipient.name}님에게",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
            QuietButton(selected.label,Modifier.testTag("transfer_purpose")){purposeOpen=true}
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Money(amount,Modifier.weight(1f).testTag("amount_value"));IconAction(BankIcons.Edit,"금액 직접 입력",Modifier.testTag("amount_edit")){entered=digits;direct=true}}
        Caption("잔액 ${won(state.balance)}원");TaskGap(10)
        Row(Modifier.fillMaxWidth()){listOf(10000L to "+1만",100000L to "+10만",1000000L to "+100만").forEach{(n,label)->QuietButton(label,Modifier.weight(1f)){update((amount+n).toString())}}}
        if(amount>0&&error!=null)ErrorNote(error)
    }
    if(purposeOpen)AlertDialog(onDismissRequest={purposeOpen=false},title={Text("송금 목적")},text={
        Column(Modifier.verticalScroll(rememberScrollState())){Purpose.entries.forEach{p->
            Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clickable(role=Role.RadioButton){purpose=p.name;model.storeDraft(TransferDraft(draft.recipient,amount,p));purposeOpen=false}.semantics{this.selected=selected==p},verticalAlignment=Alignment.CenterVertically){Text(p.label,Modifier.weight(1f));if(selected==p)AppIcon(BankIcons.Check)}
        }}
    },confirmButton={QuietButton("닫기"){purposeOpen=false}},containerColor=TraceColors.Surface)
    if(direct)AlertDialog(onDismissRequest={direct=false},title={Text("송금 금액")},text={Field(entered,"원",{entered=it.filter(Char::isDigit).take(9)},Modifier.testTag("amount_direct_input"),keyboard=KeyboardType.Number)},confirmButton={QuietButton("입력 완료"){update(entered);direct=false}},dismissButton={QuietButton("취소"){direct=false}},containerColor=TraceColors.Surface)
}

@Composable fun ViewportReview(state:BankState,record:TransferRecord,interaction:InteractionState,model:BankViewModel,open:(String)->Unit,back:()->Unit) {
    val i=record.intent
    val error=runCatching{BankEngine.validateAmount(state,i.amount,i.purpose,model.repository.clock.now())}.exceptionOrNull()?.message
    TaskPage("송금 확인","transfer_review",back=back,footer={
        Row(Modifier.fillMaxWidth().padding(bottom=6.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){Icon(BankIcons.Trace,null,Modifier.size(18.dp),tint=TraceColors.Coral);Caption("보내기 전 맥락까지 확인해요",Modifier.padding(start=4.dp))}
        PrimaryButton("${won(i.amount)}원 보내기",Modifier.testTag("transfer_confirm"),enabled=error==null&&!interaction.busy&&record.stage==TransferStage.REVIEW){model.requestAuthorization(i.id)}
    }) {
        TaskGap(8)
        Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){
            PersonBadge(i.recipient.name,48,true);TaskGap(12)
            Text("${i.recipient.name}님에게",style=MaterialTheme.typography.titleMedium,textAlign=TextAlign.Center)
            Text("${won(i.amount)}원",style=MaterialTheme.typography.displaySmall,modifier=Modifier.semantics{heading()})
            Text("보낼까요?",style=MaterialTheme.typography.titleLarge)
        }
        TaskGap(22)
        TaskDetail("받는 계좌","${i.recipient.bank}\n${i.recipient.account}")
        TaskDetail("출금 계좌","새온 생활통장")
        TaskDetail("목적 · 수수료","${i.purpose.label} · 0원")
        QuietButton("금액·목적 수정",Modifier.fillMaxWidth().testTag("review_edit"),enabled=record.stage==TransferStage.REVIEW&&!interaction.busy){model.editReview(i.id){open("amount")}}
        if(i.officialRouteId!=null)Caption("수취인이 바뀌어 새 거래로 인증합니다.")
        error?.let{ErrorNote(it)};record.error?.takeIf{it!=error}?.let{ErrorNote(it)}
    }
}

@Composable fun ViewportHold(record:TransferRecord,easy:Boolean,model:BankViewModel,open:(String)->Unit,back:()->Unit,home:()->Unit) {
    var details by rememberSaveable{mutableStateOf(false)}
    val order=listOf(RiskType.REMOTE_ACCESS,RiskType.FAMILY_CLAIM,RiskType.PROFIT_PROMISE,RiskType.SECRECY,RiskType.IMPERSONATION,RiskType.URGENCY,RiskType.NEW_RECIPIENT,RiskType.SUSPICIOUS_LINK,RiskType.FINANCIAL_INSTRUCTION)
    val reasons=order.filter{it in record.reasons}
    TaskPage("송금 보류","trace_hold",back=back,actions={AppIcon(BankIcons.Trace,tint=TraceColors.Coral,size=25)},footer={
        PrimaryButton("공식 경로로 확인하기",Modifier.testTag("hold_safe_action")){open("safety_guide")}
        SecondaryButton("송금 취소",Modifier.testTag("hold_cancel")){model.cancelTransfer(record.intent.id,home)}
    }) {
        TaskHeadline("잠깐, 확인하고\n보내볼까요?","확인하고 보내세요")
        if(easy)Box(Modifier.testTag("easy_mode"))
        TaskGap(12);Body("방금 전의 요청이\n이 송금과 이어져 있어요.",subdued=true);TaskGap(16)
        TaskTransaction(record.intent.recipient.name,record.intent.amount,if(record.intent.recipient.known)"저장된 수취인"else"처음 보내는 계좌")
        TaskGap(12)
        reasons.take(3).forEach{reason->Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){AppIcon(BankIcons.Link,size=16,tint=TraceColors.CoralText);Text(reason.label,style=MaterialTheme.typography.bodyMedium)}}
        Row{QuietButton("이유 자세히",Modifier.weight(1f).testTag("hold_reasons_open")){details=true};QuietButton("이어진 흐름",Modifier.weight(1f).testTag("hold_timeline_open")){open("timeline")}}
        Caption("아직 돈은 나가지 않았어요.")
    }
    if(details)AlertDialog(onDismissRequest={details=false},title={Text("왜 이 송금을 멈췄나요?")},text={Column(Modifier.verticalScroll(rememberScrollState()).testTag("trace_hold_reason")){reasons.forEach{NumberedReason(0,it.label,it.explanation)}}},confirmButton={QuietButton("확인",Modifier.testTag("hold_reasons_close")){details=false}},containerColor=TraceColors.Surface)
}

@Composable fun ViewportWarn(record:TransferRecord,interaction:InteractionState,model:BankViewModel,back:()->Unit,home:()->Unit) {
    var checked by rememberSaveable(record.intent.id){mutableStateOf(false)}
    TaskPage("보내기 전 확인","trace_warn",back=back,actions={AppIcon(BankIcons.Trace,tint=TraceColors.Coral,size=25)},footer={
        PrimaryButton("확인한 내용으로 다시 보기",Modifier.testTag("warn_acknowledge"),enabled=checked&&!interaction.busy){model.acknowledge(record.intent.id)}
        SecondaryButton("송금 취소"){model.cancelTransfer(record.intent.id,home)}
    }) {
        TaskHeadline("보내기 전에\n하나만 확인해 주세요","한 번 더 확인해 주세요")
        TaskGap(16);Body("확인하지 않은 링크와 이 송금이 가까운 시간에 이어졌어요.",subdued=true);TaskGap(20)
        TaskTransaction(record.intent.recipient.name,record.intent.amount)
        TaskGap(16);Body("이미 알고 있던 연락처나 공식 앱에서 받는 분과 금액을 확인하세요.",subdued=true)
        Row(Modifier.fillMaxWidth().heightIn(min=64.dp).toggleable(checked,role=Role.Checkbox,onValueChange={checked=it}).testTag("warn_check"),verticalAlignment=Alignment.CenterVertically){Checkbox(checked,null);Text("다른 경로로 받는 분과 금액을 확인했어요.",Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)}
        Caption("아직 돈은 나가지 않았어요. 다음 화면에서 다시 인증합니다.")
    }
}

@Composable fun ViewportVerify(record:TransferRecord,interaction:InteractionState,model:BankViewModel,back:()->Unit,home:()->Unit) {
    TaskPage("상환 경로 확인","trace_verify",back=back,actions={AppIcon(BankIcons.Trace,tint=TraceColors.Coral,size=25)},footer={
        PrimaryButton(if(interaction.routeLoading)"공식 경로 확인 중…"else"공식 상환 경로 확인",Modifier.testTag("verify_route"),enabled=!interaction.busy){model.resolveRoute(record.intent.id)}
        SecondaryButton("송금 취소"){model.cancelTransfer(record.intent.id,home)}
    }) {
        TaskHeadline("대출을 갚는 돈,\n받는 곳이 달라요","상환 경로를 확인하세요")
        TaskGap(16);Body("안내받은 곳은 개인 계좌예요. 은행에 등록된 상환 경로를 먼저 확인해요.",subdued=true);TaskGap(20)
        TaskTransaction(record.intent.recipient.name,record.intent.amount,"안내받은 개인 계좌")
        TaskGap(16);TaskDetail("보내는 목적","대출 상환");TaskDetail("받는 계좌",record.intent.recipient.account)
        TaskGap(12);Caption("상대가 준 번호나 링크로 확인하지 않아요.\n아직 돈은 나가지 않았습니다.")
    }
}
@Composable fun ViewportUnknown(record:TransferRecord,interaction:InteractionState,model:BankViewModel,back:()->Unit,home:()->Unit) {
    TaskPage("확인 미완료","trace_unknown",back=back,actions={AppIcon(BankIcons.Trace,tint=TraceColors.Coral,size=25)},footer={
        PrimaryButton(if(interaction.routeLoading)"다시 확인하고 있어요"else"공식 경로 다시 확인",Modifier.testTag("unknown_retry"),enabled=!interaction.busy){model.resolveRoute(record.intent.id)}
        SecondaryButton("송금 취소"){model.cancelTransfer(record.intent.id,home)}
    }) {
        TaskHeadline("확인을 마친 뒤에\n보낼 수 있어요","확인 후에 보내세요");TaskGap(16)
        Body("공식 상환 경로를 확인하지 못했어요. 확인되지 않았다는 건 안전하다는 뜻이 아니에요.",subdued=true);TaskGap(20)
        TaskTransaction(record.intent.recipient.name,record.intent.amount);TaskGap(20)
        Body("잔액은 그대로예요.");TaskGap(8);Caption("송금을 완료하거나 일반 송금으로 전환하지 않았습니다.")
    }
}
@Composable fun ViewportComplete(state:BankState,record:TransferRecord,open:(String)->Unit,home:()->Unit) {
    val receipt=state.receipts.find{it.intentId==record.intent.id&&!it.seed}
    if(receipt==null){Page(title="거래 결과 확인",footer={PrimaryButton("거래 내역 확인"){open("history")}}){Body("다시 송금하지 말고 거래 내역을 확인해 주세요.")};return}
    TaskPage(tag="transfer_complete",footer={PrimaryButton("확인",Modifier.testTag("complete_confirm"),onClick=home)}) {
        Spacer(Modifier.height((LocalTaskHeight.current.value*.14f).coerceIn(20f,78f).dp))
        Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){
            ResultSeal(diameter=64);TaskGap(24)
            Text("${receipt.recipient.name}님에게",style=MaterialTheme.typography.titleMedium)
            Text("${won(receipt.amount)}원을",style=MaterialTheme.typography.headlineLarge,modifier=Modifier.semantics{heading()})
            Text(if(record.intent.purpose==Purpose.LOAN)"상환했어요"else"보냈어요",style=MaterialTheme.typography.headlineSmall)
            TaskGap(16);Caption("수수료 0원");TaskGap(24)
            QuietButton("송금 내역 보기",Modifier.testTag("complete_receipt")){open("receipt/${receipt.id}")}
            if(state.scenario==DemoScenario.NEW_ACCOUNT)QuietButton("같은 송금, 다른 맥락 보기"){open("comparison")}
        }
    }
}
@Composable fun ComparisonScreen(model:BankViewModel,open:(String)->Unit,back:()->Unit) {
    TaskPage("비교 시연","comparison",back=back) {
        TaskHeadline("같은 송금,\n다른 맥락");TaskGap(16)
        TaskTransaction("김○○",3000000,"노을은행 110-***-9802")
        TaskGap(20);Body("계좌와 금액은 같아요.\n송금하게 된 이유만 달라집니다.",subdued=true);TaskGap(20)
        PrimaryButton("정상 첫 거래 시연",Modifier.testTag("compare_normal")){open("scenario/NEW_ACCOUNT")}
        TaskGap(12);PrimaryButton("기관 사칭 요청 시연",Modifier.testTag("compare_risk")){open("scenario/IMPERSONATION")}
        TaskGap(20);Caption("각 시연을 시작하면 가상 잔액을 초기화합니다. 두 상황 모두 같은 로컬 정책을 통과하며, 결과를 버튼에서 강제하지 않습니다.")
    }
}
